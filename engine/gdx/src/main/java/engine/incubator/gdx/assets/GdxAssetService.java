package engine.incubator.gdx.assets;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import engine.incubator.assets.AssetDiagnostic;
import engine.incubator.assets.AssetDiagnosticCode;
import engine.incubator.assets.AssetDiagnosticSeverity;
import engine.incubator.assets.AssetEntry;
import engine.incubator.assets.AssetException;
import engine.incubator.assets.AssetFailure;
import engine.incubator.assets.AssetGroupHandle;
import engine.incubator.assets.AssetHandle;
import engine.incubator.assets.AssetId;
import engine.incubator.assets.AssetLoad;
import engine.incubator.assets.AssetManifest;
import engine.incubator.assets.AssetMetrics;
import engine.incubator.assets.AssetProgress;
import engine.incubator.assets.AssetService;
import engine.incubator.assets.SharedAssetData;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Typed, group-owned facade over one exclusively owned libGDX {@link AssetManager}.
 *
 * <p>Calls to {@link #load(AssetManifest)} may come from concurrent producers. Backend work is
 * serialized by {@link #update()}, which binds the service to the backend thread. Group unload
 * and service close must then run on that same thread so graphics resources cannot be disposed
 * by producer threads. The supplied manager must be empty and ownership is transferred to this
 * service; callers must not access or dispose it afterwards.</p>
 */
public final class GdxAssetService implements AssetService {
    private static final int DIAGNOSTIC_LIMIT = 512;

    private final ReentrantLock lock = new ReentrantLock();
    private final AssetManager assetManager;
    private final FileHandleResolver resolver;
    private final ArrayDeque<LoadOperation> queuedLoads = new ArrayDeque<>();
    private final LinkedHashMap<String, GroupHandle> liveGroups = new LinkedHashMap<>();
    private final Map<String, Long> generations = new HashMap<>();
    private final Map<String, ManifestDefinition> definitionsById = new HashMap<>();
    private final Map<String, SourceReservation> sourceReservations = new HashMap<>();
    private final ArrayDeque<AssetDiagnostic> diagnosticHistory = new ArrayDeque<>();

    private LoadOperation currentLoad;
    private BackendFailure backendFailure;
    private long diagnosticSequence;
    private long loadRequests;
    private long groupsLoaded;
    private long groupsUnloaded;
    private long assetReferencesLoaded;
    private long assetReferencesUnloaded;
    private long backendLoads;
    private long backendUnloads;
    private long loadFailures;
    private long staleHandleAccesses;
    private Thread backendThread;
    private boolean closed;

    /**
     * Takes exclusive ownership of an empty manager and installs the service error listener.
     */
    public GdxAssetService(AssetManager assetManager) {
        this.assetManager = Objects.requireNonNull(assetManager, "assetManager");
        resolver = Objects.requireNonNull(
            assetManager.getFileHandleResolver(),
            "assetManager file resolver"
        );
        if (assetManager.getLoadedAssets() != 0 || assetManager.getQueuedAssets() != 0) {
            throw new IllegalArgumentException(
                "GdxAssetService requires an empty AssetManager with exclusive ownership"
            );
        }
        assetManager.setErrorListener(this::recordBackendFailure);
    }

    @Override
    public AssetLoad load(AssetManifest manifest) {
        Objects.requireNonNull(manifest, "manifest");
        lock.lock();
        try {
            requireOpen();
            loadRequests++;
            String groupId = manifest.groupId();
            if (containsGroup(groupId)) {
                throw preflightFailure(
                    AssetFailure.DUPLICATE_GROUP,
                    AssetDiagnosticCode.LOAD_FAILED,
                    groupId,
                    "",
                    "",
                    "Asset group is already queued or loaded: " + groupId
                );
            }

            List<ResolvedEntry> resolvedEntries = new ArrayList<>();
            for (AssetEntry<?> entry : manifest.entries()) {
                resolvedEntries.add(resolve(groupId, entry));
            }
            validateDefinitions(groupId, resolvedEntries);
            reserveSources(resolvedEntries);
            resolvedEntries.forEach(entry -> definitionsById.putIfAbsent(
                entry.definition().id().value(),
                ManifestDefinition.from(entry.definition())
            ));

            long generation = generations.merge(groupId, 1L, Long::sum);
            LoadOperation operation = new LoadOperation(groupId, generation, resolvedEntries);
            queuedLoads.addLast(operation);
            addDiagnostic(
                AssetDiagnosticSeverity.INFO,
                AssetDiagnosticCode.GROUP_QUEUED,
                groupId,
                "",
                "",
                "Queued " + resolvedEntries.size() + " typed assets"
            );
            return operation;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean update() {
        lock.lock();
        try {
            requireOpen();
            bindBackendThread("update");
            if (currentLoad == null) {
                startNextLoad();
            }
            if (currentLoad == null) {
                return queuedLoads.isEmpty();
            }

            backendFailure = null;
            try {
                boolean finished = assetManager.update();
                refreshProgress(currentLoad);
                if (backendFailure != null) {
                    failCurrentLoad(
                        backendFailure.sourcePath(),
                        backendFailure.failure()
                    );
                } else if (finished) {
                    completeCurrentLoad();
                }
            } catch (Throwable failure) {
                failCurrentLoad("", failure);
            }
            return currentLoad == null && queuedLoads.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public AssetMetrics metrics() {
        lock.lock();
        try {
            int pending = queuedLoads.size() + (currentLoad == null ? 0 : 1);
            int references = sourceReservations.values().stream()
                .mapToInt(SourceReservation::references)
                .sum();
            return new AssetMetrics(
                loadRequests,
                groupsLoaded,
                groupsUnloaded,
                assetReferencesLoaded,
                assetReferencesUnloaded,
                backendLoads,
                backendUnloads,
                loadFailures,
                staleHandleAccesses,
                pending,
                liveGroups.size(),
                references,
                assetManager.getLoadedAssets()
            );
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<AssetDiagnostic> diagnostics() {
        lock.lock();
        try {
            return List.copyOf(diagnosticHistory);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isClosed() {
        lock.lock();
        try {
            return closed;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        lock.lock();
        try {
            if (closed) {
                return;
            }
            bindBackendThread("close");

            AssetException closingFailure = new AssetException(
                AssetFailure.SERVICE_CLOSED,
                "",
                "",
                "Asset service closed before the group finished loading"
            );
            Throwable failure = null;
            for (LoadOperation operation : queuedLoads) {
                releaseSources(operation.entries());
                operation.fail(closingFailure);
            }
            queuedLoads.clear();
            if (currentLoad != null) {
                failure = appendFailure(
                    failure,
                    rollbackBackendReferences(currentLoad.entries())
                );
                releaseSources(currentLoad.entries());
                currentLoad.fail(closingFailure);
                currentLoad = null;
            }

            List<GroupHandle> groups = new ArrayList<>(liveGroups.values());
            Collections.reverse(groups);
            for (GroupHandle group : groups) {
                try {
                    unloadLocked(group);
                } catch (Throwable unloadFailure) {
                    failure = appendFailure(failure, unloadFailure);
                }
            }
            try {
                assetManager.dispose();
            } catch (Throwable disposeFailure) {
                failure = appendFailure(failure, disposeFailure);
            } finally {
                closed = true;
            }
            if (failure != null) {
                throw new AssetException(
                    AssetFailure.LOAD_FAILED,
                    "",
                    "",
                    "Asset service cleanup failed",
                    failure
                );
            }
        } finally {
            lock.unlock();
        }
    }

    private ResolvedEntry resolve(String groupId, AssetEntry<?> entry) {
        String selected = entry.sourcePath();
        FileHandle selectedHandle = resolvePackagedHandle(groupId, entry, selected);
        if (!selectedHandle.exists()) {
            Optional<String> fallback = entry.fallbackPath()
                .filter(path -> resolvePackagedHandle(groupId, entry, path).exists());
            if (fallback.isEmpty()) {
                throw preflightFailure(
                    AssetFailure.SOURCE_NOT_FOUND,
                    AssetDiagnosticCode.LOAD_FAILED,
                    groupId,
                    entry.id().value(),
                    entry.sourcePath(),
                    "Asset source and fallback are missing"
                );
            }
            selected = fallback.orElseThrow();
            addDiagnostic(
                AssetDiagnosticSeverity.WARNING,
                AssetDiagnosticCode.FALLBACK_SELECTED,
                groupId,
                entry.id().value(),
                selected,
                "Primary source is missing; selected manifest fallback"
            );
        }
        if (assetManager.getLoader(entry.id().type(), selected) == null) {
            throw preflightFailure(
                AssetFailure.LOADER_NOT_FOUND,
                AssetDiagnosticCode.LOAD_FAILED,
                groupId,
                entry.id().value(),
                selected,
                "No libGDX loader is registered for " + entry.id().type().getName()
            );
        }
        return new ResolvedEntry(entry, selected);
    }

    private FileHandle resolvePackagedHandle(
        String groupId,
        AssetEntry<?> entry,
        String path
    ) {
        FileHandle handle = Objects.requireNonNull(
            resolver.resolve(path),
            "Asset resolver returned null for " + path
        );
        if (handle.type() != Files.FileType.Classpath
            && handle.type() != Files.FileType.Internal) {
            throw preflightFailure(
                AssetFailure.CWD_DEPENDENT_SOURCE,
                AssetDiagnosticCode.LOAD_FAILED,
                groupId,
                entry.id().value(),
                path,
                "Asset resolver must produce Classpath or Internal handles, not "
                    + handle.type()
            );
        }
        return handle;
    }

    private void validateDefinitions(String groupId, List<ResolvedEntry> entries) {
        for (ResolvedEntry entry : entries) {
            String id = entry.definition().id().value();
            ManifestDefinition candidate = ManifestDefinition.from(entry.definition());
            ManifestDefinition known = definitionsById.get(id);
            if (known != null && !known.equals(candidate)) {
                AssetFailure failure = known.type().equals(candidate.type())
                    ? AssetFailure.LOAD_FAILED
                    : AssetFailure.TYPE_MISMATCH;
                AssetDiagnosticCode code = failure == AssetFailure.TYPE_MISMATCH
                    ? AssetDiagnosticCode.TYPE_MISMATCH
                    : AssetDiagnosticCode.LOAD_FAILED;
                throw preflightFailure(
                    failure,
                    code,
                    groupId,
                    id,
                    entry.selectedPath(),
                    "Asset id conflicts with its canonical manifest definition: " + id
                );
            }

            SourceReservation source = sourceReservations.get(entry.selectedPath());
            if (source != null && !source.type().equals(candidate.type())) {
                throw preflightFailure(
                    AssetFailure.TYPE_MISMATCH,
                    AssetDiagnosticCode.TYPE_MISMATCH,
                    groupId,
                    id,
                    entry.selectedPath(),
                    "Source is already reserved with type " + source.type().getName()
                        + ", requested " + candidate.type().getName()
                );
            }
        }
    }

    private void reserveSources(List<ResolvedEntry> entries) {
        for (ResolvedEntry entry : entries) {
            String path = entry.selectedPath();
            SourceReservation reservation = sourceReservations.get(path);
            if (reservation == null) {
                sourceReservations.put(
                    path,
                    new SourceReservation(entry.definition().id().type(), 1)
                );
                backendLoads++;
            } else {
                reservation.increment();
            }
        }
    }

    private void releaseSources(List<ResolvedEntry> entries) {
        for (ResolvedEntry entry : entries) {
            SourceReservation reservation = sourceReservations.get(entry.selectedPath());
            if (reservation == null) {
                continue;
            }
            if (reservation.decrementAndGet() == 0) {
                sourceReservations.remove(entry.selectedPath());
                backendUnloads++;
            }
        }
    }

    private void startNextLoad() {
        currentLoad = queuedLoads.pollFirst();
        if (currentLoad == null) {
            return;
        }
        try {
            for (ResolvedEntry entry : currentLoad.entries()) {
                queueBackendLoad(entry);
            }
        } catch (Throwable failure) {
            failCurrentLoad("", failure);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void queueBackendLoad(ResolvedEntry entry) {
        assetManager.load(entry.selectedPath(), (Class) entry.definition().id().type());
    }

    private void refreshProgress(LoadOperation operation) {
        int loaded = 0;
        for (ResolvedEntry entry : operation.entries()) {
            if (assetManager.isLoaded(
                entry.selectedPath(),
                entry.definition().id().type()
            )) {
                loaded++;
            }
        }
        operation.setLoadedAssets(loaded);
    }

    private void completeCurrentLoad() {
        LoadOperation completed = currentLoad;
        refreshProgress(completed);
        if (completed.loadedAssets() != completed.entries().size()) {
            failCurrentLoad("", new IllegalStateException(
                "AssetManager finished without every typed manifest entry loaded"
            ));
            return;
        }
        GroupHandle group = new GroupHandle(
            completed.groupId(),
            completed.generation(),
            completed.entries()
        );
        liveGroups.put(group.groupId(), group);
        groupsLoaded++;
        assetReferencesLoaded += completed.entries().size();
        currentLoad = null;
        completed.complete(group);
        addDiagnostic(
            AssetDiagnosticSeverity.INFO,
            AssetDiagnosticCode.GROUP_LOADED,
            group.groupId(),
            "",
            "",
            "Loaded generation " + group.generation()
        );
    }

    private void failCurrentLoad(String failedPath, Throwable failure) {
        LoadOperation failed = currentLoad;
        if (failed == null) {
            return;
        }
        Throwable combinedFailure = appendFailure(
            Objects.requireNonNull(failure, "failure"),
            rollbackBackendReferences(failed.entries())
        );
        releaseSources(failed.entries());
        currentLoad = null;
        loadFailures++;
        ResolvedEntry first = failed.entries().stream()
            .filter(entry -> entry.selectedPath().equals(failedPath))
            .findFirst()
            .orElseGet(() -> failed.entries().getFirst());
        AssetException exception = new AssetException(
            AssetFailure.LOAD_FAILED,
            failed.groupId(),
            first.definition().id().value(),
            "Failed to load asset group '" + failed.groupId() + "'",
            combinedFailure
        );
        failed.fail(exception);
        addDiagnostic(
            AssetDiagnosticSeverity.ERROR,
            AssetDiagnosticCode.LOAD_FAILED,
            failed.groupId(),
            first.definition().id().value(),
            first.selectedPath(),
            failure.getClass().getName() + ": " + Objects.toString(failure.getMessage(), "")
        );
        backendFailure = null;
    }

    private Throwable rollbackBackendReferences(List<ResolvedEntry> entries) {
        Throwable failure = null;
        for (int index = entries.size() - 1; index >= 0; index--) {
            String path = entries.get(index).selectedPath();
            try {
                if (assetManager.contains(path)) {
                    assetManager.unload(path);
                }
            } catch (Throwable unloadFailure) {
                failure = appendFailure(failure, unloadFailure);
            }
        }
        return failure;
    }

    private void unload(GroupHandle group) {
        lock.lock();
        try {
            unloadLocked(group);
        } finally {
            lock.unlock();
        }
    }

    private void unloadLocked(GroupHandle group) {
        if (!group.active) {
            return;
        }
        bindBackendThread("unload");
        GroupHandle registered = liveGroups.get(group.groupId());
        if (registered != group) {
            markStaleAndThrow(group.groupId(), "", "Asset group handle is stale");
        }
        group.active = false;
        liveGroups.remove(group.groupId());

        Throwable failure = null;
        List<ResolvedEntry> entries = group.entries;
        for (int index = entries.size() - 1; index >= 0; index--) {
            ResolvedEntry entry = entries.get(index);
            try {
                assetManager.unload(entry.selectedPath());
            } catch (Throwable unloadFailure) {
                if (failure == null) {
                    failure = unloadFailure;
                } else {
                    failure.addSuppressed(unloadFailure);
                }
            }
        }
        releaseSources(entries);
        groupsUnloaded++;
        assetReferencesUnloaded += entries.size();
        addDiagnostic(
            AssetDiagnosticSeverity.INFO,
            AssetDiagnosticCode.GROUP_UNLOADED,
            group.groupId(),
            "",
            "",
            "Unloaded generation " + group.generation()
        );
        if (failure != null) {
            loadFailures++;
            throw new AssetException(
                AssetFailure.LOAD_FAILED,
                group.groupId(),
                "",
                "One or more backend resources failed to unload",
                failure
            );
        }
    }

    private <T extends SharedAssetData> AssetHandle<T> createHandle(
        GroupHandle group,
        AssetId<T> id
    ) {
        Objects.requireNonNull(id, "id");
        lock.lock();
        try {
            requireLive(group, id.value());
            ResolvedEntry entry = group.entriesById.get(id.value());
            if (entry == null) {
                throw new AssetException(
                    AssetFailure.UNKNOWN_ASSET,
                    group.groupId(),
                    id.value(),
                    "Asset id is not present in group: " + id.value()
                );
            }
            if (!entry.definition().id().type().equals(id.type())) {
                addDiagnostic(
                    AssetDiagnosticSeverity.ERROR,
                    AssetDiagnosticCode.TYPE_MISMATCH,
                    group.groupId(),
                    id.value(),
                    entry.selectedPath(),
                    "Handle type does not match the manifest"
                );
                throw new AssetException(
                    AssetFailure.TYPE_MISMATCH,
                    group.groupId(),
                    id.value(),
                    "Expected " + entry.definition().id().type().getName()
                        + ", requested " + id.type().getName()
                );
            }
            return new TypedAssetHandle<>(group, entry, id);
        } finally {
            lock.unlock();
        }
    }

    private <T extends SharedAssetData> T value(
        GroupHandle group,
        ResolvedEntry entry,
        AssetId<T> id
    ) {
        lock.lock();
        try {
            requireLive(group, id.value());
            Object value = assetManager.get(entry.selectedPath(), id.type());
            return id.type().cast(value);
        } finally {
            lock.unlock();
        }
    }

    private void requireLive(GroupHandle group, String assetId) {
        if (!group.active || closed || liveGroups.get(group.groupId()) != group) {
            markStaleAndThrow(group.groupId(), assetId, "Asset handle generation is stale");
        }
    }

    private void markStaleAndThrow(String groupId, String assetId, String message) {
        staleHandleAccesses++;
        addDiagnostic(
            AssetDiagnosticSeverity.ERROR,
            AssetDiagnosticCode.STALE_HANDLE,
            groupId,
            assetId,
            "",
            message
        );
        throw new AssetException(
            AssetFailure.STALE_HANDLE,
            groupId,
            assetId,
            message
        );
    }

    private boolean isStale(GroupHandle group) {
        lock.lock();
        try {
            return closed || !group.active || liveGroups.get(group.groupId()) != group;
        } finally {
            lock.unlock();
        }
    }

    private boolean containsGroup(String groupId) {
        return liveGroups.containsKey(groupId)
            || currentLoad != null && currentLoad.groupId().equals(groupId)
            || queuedLoads.stream().anyMatch(load -> load.groupId().equals(groupId));
    }

    private AssetException preflightFailure(
        AssetFailure failure,
        AssetDiagnosticCode code,
        String groupId,
        String assetId,
        String path,
        String message
    ) {
        loadFailures++;
        addDiagnostic(
            AssetDiagnosticSeverity.ERROR,
            code,
            groupId,
            assetId,
            path,
            message
        );
        return new AssetException(failure, groupId, assetId, message);
    }

    private void recordBackendFailure(AssetDescriptor descriptor, Throwable failure) {
        lock.lock();
        try {
            backendFailure = new BackendFailure(
                descriptor == null ? "" : descriptor.fileName,
                Objects.requireNonNull(failure, "failure")
            );
        } finally {
            lock.unlock();
        }
    }

    private void addDiagnostic(
        AssetDiagnosticSeverity severity,
        AssetDiagnosticCode code,
        String groupId,
        String assetId,
        String sourcePath,
        String message
    ) {
        if (diagnosticHistory.size() == DIAGNOSTIC_LIMIT) {
            diagnosticHistory.removeFirst();
        }
        diagnosticHistory.addLast(new AssetDiagnostic(
            ++diagnosticSequence,
            severity,
            code,
            groupId,
            assetId,
            sourcePath,
            message
        ));
    }

    private void requireOpen() {
        if (closed) {
            throw new AssetException(
                AssetFailure.SERVICE_CLOSED,
                "",
                "",
                "Asset service is closed"
            );
        }
    }

    private void bindBackendThread(String operation) {
        Thread current = Thread.currentThread();
        if (backendThread == null) {
            backendThread = current;
        } else if (backendThread != current) {
            throw new IllegalStateException(
                operation + " must run on the AssetService backend thread '"
                    + backendThread.getName() + "'"
            );
        }
    }

    private static Throwable appendFailure(Throwable primary, Throwable additional) {
        if (additional == null) {
            return primary;
        }
        if (primary == null) {
            return additional;
        }
        if (primary != additional) {
            primary.addSuppressed(additional);
        }
        return primary;
    }

    private record ResolvedEntry(AssetEntry<?> definition, String selectedPath) {
    }

    private record ManifestDefinition(
        Class<?> type,
        String sourcePath,
        Optional<String> fallbackPath
    ) {
        static ManifestDefinition from(AssetEntry<?> entry) {
            return new ManifestDefinition(
                entry.id().type(),
                entry.sourcePath(),
                entry.fallbackPath()
            );
        }
    }

    private static final class SourceReservation {
        private final Class<?> type;
        private int references;

        private SourceReservation(Class<?> type, int references) {
            this.type = type;
            this.references = references;
        }

        Class<?> type() {
            return type;
        }

        int references() {
            return references;
        }

        void increment() {
            references++;
        }

        int decrementAndGet() {
            references--;
            return references;
        }
    }

    private static final class LoadOperation implements AssetLoad {
        private final String groupId;
        private final long generation;
        private final List<ResolvedEntry> entries;
        private final CompletableFuture<AssetGroupHandle> completion = new CompletableFuture<>();
        private volatile int loadedAssets;

        private LoadOperation(
            String groupId,
            long generation,
            List<ResolvedEntry> entries
        ) {
            this.groupId = groupId;
            this.generation = generation;
            this.entries = List.copyOf(entries);
        }

        @Override
        public String groupId() {
            return groupId;
        }

        long generation() {
            return generation;
        }

        List<ResolvedEntry> entries() {
            return entries;
        }

        int loadedAssets() {
            return loadedAssets;
        }

        void setLoadedAssets(int loadedAssets) {
            this.loadedAssets = loadedAssets;
        }

        @Override
        public AssetProgress progress() {
            return new AssetProgress(groupId, loadedAssets, entries.size());
        }

        @Override
        public boolean isDone() {
            return completion.isDone();
        }

        @Override
        public CompletionStage<AssetGroupHandle> completion() {
            return completion.minimalCompletionStage();
        }

        void complete(AssetGroupHandle group) {
            loadedAssets = entries.size();
            completion.complete(group);
        }

        void fail(Throwable failure) {
            completion.completeExceptionally(failure);
        }
    }

    private final class GroupHandle implements AssetGroupHandle {
        private final String groupId;
        private final long generation;
        private final List<ResolvedEntry> entries;
        private final Map<String, ResolvedEntry> entriesById;
        private boolean active = true;

        private GroupHandle(
            String groupId,
            long generation,
            List<ResolvedEntry> entries
        ) {
            this.groupId = groupId;
            this.generation = generation;
            this.entries = List.copyOf(entries);
            Map<String, ResolvedEntry> byId = new LinkedHashMap<>();
            entries.forEach(entry -> byId.put(entry.definition().id().value(), entry));
            entriesById = Map.copyOf(byId);
        }

        @Override
        public String groupId() {
            return groupId;
        }

        @Override
        public long generation() {
            return generation;
        }

        @Override
        public <T extends SharedAssetData> AssetHandle<T> handle(AssetId<T> id) {
            return createHandle(this, id);
        }

        @Override
        public boolean isStale() {
            return GdxAssetService.this.isStale(this);
        }

        @Override
        public void close() {
            unload(this);
        }
    }

    private final class TypedAssetHandle<T extends SharedAssetData>
        implements AssetHandle<T> {
        private final GroupHandle group;
        private final ResolvedEntry entry;
        private final AssetId<T> id;

        private TypedAssetHandle(GroupHandle group, ResolvedEntry entry, AssetId<T> id) {
            this.group = group;
            this.entry = entry;
            this.id = id;
        }

        @Override
        public AssetId<T> id() {
            return id;
        }

        @Override
        public T value() {
            return GdxAssetService.this.value(group, entry, id);
        }

        @Override
        public boolean isStale() {
            return GdxAssetService.this.isStale(group);
        }
    }

    private record BackendFailure(String sourcePath, Throwable failure) {
    }
}
