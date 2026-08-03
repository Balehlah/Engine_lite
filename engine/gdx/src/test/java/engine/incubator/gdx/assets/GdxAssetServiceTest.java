package engine.incubator.gdx.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.SynchronousAssetLoader;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import engine.incubator.assets.AssetDiagnosticCode;
import engine.incubator.assets.AssetException;
import engine.incubator.assets.AssetFailure;
import engine.incubator.assets.AssetGroupHandle;
import engine.incubator.assets.AssetHandle;
import engine.incubator.assets.AssetId;
import engine.incubator.assets.AssetLoad;
import engine.incubator.assets.AssetManifest;
import engine.incubator.assets.AssetMetrics;
import engine.incubator.assets.SharedAssetData;
import engine.incubator.runtime.lifecycle.GameContext;
import engine.incubator.runtime.lifecycle.GameRuntime;
import engine.incubator.runtime.lifecycle.RuntimeScene;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

@Tag("specification")
final class GdxAssetServiceTest {
    private static final String FIXTURE = "spike/sprite.rgba";
    private static final String BROKEN_FIXTURE = "spike/probe.tmx";

    @Test
    void serviceDeclaresNoMutableStaticState() {
        List<String> mutableStatics = List.of(GdxAssetService.class.getDeclaredFields())
            .stream()
            .filter(field -> Modifier.isStatic(field.getModifiers()))
            .filter(field -> !Modifier.isFinal(field.getModifiers()))
            .map(field -> field.getName())
            .toList();

        assertEquals(List.of(), mutableStatics);
    }

    @Test
    void cwdDependentResolversAreRejectedBeforeBackendUse() {
        Tracker tracker = new Tracker();
        try (GdxAssetService service = newService(tracker, FileHandle::new)) {
            AssetException failure = assertThrows(
                AssetException.class,
                () -> service.load(AssetManifest.builder("unsafe-resolver")
                    .add(AssetId.of("unsafe", TrackedData.class), FIXTURE)
                    .build())
            );

            assertEquals(AssetFailure.CWD_DEPENDENT_SOURCE, failure.failure());
            assertEquals(0, tracker.loads(FIXTURE));
            assertTrue(service.metrics().isAtResourceBaseline());
        }
    }

    @Test
    void serviceCloseUnloadsLiveGroupsAndIsIdempotent() {
        Tracker tracker = new Tracker();
        GdxAssetService service = newService(tracker);
        AssetGroupHandle group = loadFully(
            service,
            AssetManifest.builder("service-owned")
                .add(AssetId.of("first", TrackedData.class), FIXTURE)
                .add(AssetId.of("second", TrackedData.class), BROKEN_FIXTURE)
                .build()
        );

        service.close();
        service.close();

        assertTrue(service.isClosed());
        assertTrue(group.isStale());
        assertEquals(1, tracker.disposals(FIXTURE));
        assertEquals(1, tracker.disposals(BROKEN_FIXTURE));
        assertTrue(service.metrics().isAtResourceBaseline());
    }

    @Test
    void wrongTypeFailsBeforeTheBackendCanLoadOrReturnIt() {
        Tracker tracker = new Tracker();
        try (GdxAssetService service = newService(tracker)) {
            AssetId<TrackedData> sprite = AssetId.of("sprite", TrackedData.class);
            AssetManifest typedManifest = AssetManifest.builder("typed")
                .add(sprite, FIXTURE)
                .build();
            AssetGroupHandle group = loadFully(
                service,
                typedManifest
            );

            AssetException duplicateGroup = assertThrows(
                AssetException.class,
                () -> service.load(typedManifest)
            );
            assertEquals(AssetFailure.DUPLICATE_GROUP, duplicateGroup.failure());

            AssetException handleMismatch = assertThrows(
                AssetException.class,
                () -> group.handle(AssetId.of("sprite", OtherData.class))
            );
            assertEquals(AssetFailure.TYPE_MISMATCH, handleMismatch.failure());

            AssetException sourceMismatch = assertThrows(
                AssetException.class,
                () -> service.load(AssetManifest.builder("wrong-source-type")
                    .add(AssetId.of("other", OtherData.class), FIXTURE)
                    .build())
            );
            assertEquals(AssetFailure.TYPE_MISMATCH, sourceMismatch.failure());
            assertEquals(1, tracker.loads(FIXTURE));
            assertEquals(1, service.metrics().liveAssetReferences());

            group.close();
        }
        assertEquals(1, tracker.disposals(FIXTURE));
    }

    @Test
    @ResourceLock(Resources.SYSTEM_PROPERTIES)
    void classpathAssetAndManifestFallbackLoadOutsideTheWorkingDirectory(
        @TempDir Path externalWorkingDirectory
    ) {
        String previousWorkingDirectory = System.getProperty("user.dir");
        System.setProperty("user.dir", externalWorkingDirectory.toString());
        Tracker tracker = new Tracker();
        try (GdxAssetService service = newService(tracker)) {
            AssetId<TrackedData> sprite = AssetId.of("sprite", TrackedData.class);
            AssetLoad load = service.load(AssetManifest.builder("cwd-independent")
                .add(sprite, "missing/primary.rgba", FIXTURE)
                .build());

            assertEquals(0.0, load.progress().fraction());
            assertFalse(load.isDone());
            AssetGroupHandle group = finish(service, load);
            assertEquals(1.0, load.progress().fraction());
            assertTrue(group.handle(sprite).value().contents().contains("palette"));
            assertTrue(service.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code() == AssetDiagnosticCode.FALLBACK_SELECTED
                    && diagnostic.sourcePath().equals(FIXTURE)
            ));
            group.close();
            assertTrue(service.metrics().isAtResourceBaseline());
        } finally {
            if (previousWorkingDirectory == null) {
                System.clearProperty("user.dir");
            } else {
                System.setProperty("user.dir", previousWorkingDirectory);
            }
        }
    }

    @Test
    void sharedGroupsUnloadOneReferenceEachAndDisposeTheResourceOnce() {
        Tracker tracker = new Tracker();
        try (GdxAssetService service = newService(tracker)) {
            AssetId<TrackedData> firstId = AssetId.of("first", TrackedData.class);
            AssetId<TrackedData> secondId = AssetId.of("second", TrackedData.class);
            AssetGroupHandle first = loadFully(
                service,
                AssetManifest.builder("first-group").add(firstId, FIXTURE).build()
            );
            AssetGroupHandle second = loadFully(
                service,
                AssetManifest.builder("second-group").add(secondId, FIXTURE).build()
            );

            TrackedData shared = first.handle(firstId).value();
            assertSame(shared, second.handle(secondId).value());
            assertEquals(1, tracker.loads(FIXTURE));

            first.close();
            first.close();
            assertEquals(0, tracker.disposals(FIXTURE));
            assertSame(shared, second.handle(secondId).value());

            second.close();
            second.close();
            assertEquals(1, tracker.disposals(FIXTURE));
            AssetMetrics metrics = service.metrics();
            assertEquals(2, metrics.groupsUnloaded());
            assertEquals(2, metrics.assetReferencesUnloaded());
            assertTrue(metrics.isAtResourceBaseline());
        }
    }

    @Test
    void staleHandlesRemainDetectableAfterAGroupReload() {
        Tracker tracker = new Tracker();
        try (GdxAssetService service = newService(tracker)) {
            AssetId<TrackedData> sprite = AssetId.of("sprite", TrackedData.class);
            AssetManifest manifest = AssetManifest.builder("reloadable")
                .add(sprite, FIXTURE)
                .build();

            AssetGroupHandle first = loadFully(service, manifest);
            AssetHandle<TrackedData> stale = first.handle(sprite);
            TrackedData firstValue = stale.value();
            first.close();
            assertTrue(stale.isStale());
            assertEquals(
                AssetFailure.STALE_HANDLE,
                assertThrows(AssetException.class, stale::value).failure()
            );

            AssetGroupHandle second = loadFully(service, manifest);
            AssetHandle<TrackedData> current = second.handle(sprite);
            assertEquals(1, first.generation());
            assertEquals(2, second.generation());
            assertNotSame(firstValue, current.value());
            assertEquals(
                AssetFailure.STALE_HANDLE,
                assertThrows(AssetException.class, stale::value).failure()
            );
            second.close();

            assertEquals(2, service.metrics().staleHandleAccesses());
            assertTrue(service.metrics().isAtResourceBaseline());
        }
    }

    @Test
    void missingAndBrokenPathsFailWithDiagnosticsWithoutPoisoningTheNextLoad() {
        Tracker tracker = new Tracker();
        try (GdxAssetService service = newService(tracker)) {
            AssetException missing = assertThrows(
                AssetException.class,
                () -> service.load(AssetManifest.builder("missing")
                    .add(AssetId.of("missing", TrackedData.class), "missing/nope.rgba")
                    .build())
            );
            assertEquals(AssetFailure.SOURCE_NOT_FOUND, missing.failure());

            AssetLoad broken = service.load(AssetManifest.builder("broken")
                .add(AssetId.of("broken", BrokenData.class), BROKEN_FIXTURE)
                .build());
            AssetId<TrackedData> recoveredId = AssetId.of("recovered", TrackedData.class);
            AssetLoad recovered = service.load(AssetManifest.builder("recovered")
                .add(recoveredId, FIXTURE)
                .build());

            pumpUntilDone(service, List.of(broken, recovered));
            CompletionException brokenFailure = assertThrows(
                CompletionException.class,
                () -> broken.completion().toCompletableFuture().join()
            );
            assertEquals(
                AssetFailure.LOAD_FAILED,
                assertInstanceOf(AssetException.class, brokenFailure.getCause()).failure()
            );
            AssetGroupHandle recoveredGroup = recovered.completion()
                .toCompletableFuture()
                .join();
            assertTrue(recoveredGroup.handle(recoveredId).value().contents().contains("palette"));
            recoveredGroup.close();

            assertEquals(2, service.metrics().loadFailures());
            assertTrue(service.metrics().isAtResourceBaseline());
        }
    }

    @Test
    void concurrentProducersShareOneBackendResourceAndBackendThreadClosesDisposeOnce()
        throws Exception {
        Tracker tracker = new Tracker();
        ExecutorService executor = Executors.newFixedThreadPool(6);
        try (GdxAssetService service = newService(tracker)) {
            List<Future<AssetLoad>> submissions = new ArrayList<>();
            for (int index = 0; index < 12; index++) {
                int id = index;
                submissions.add(executor.submit(() -> service.load(
                    AssetManifest.builder("concurrent-" + id)
                        .add(AssetId.of("asset-" + id, TrackedData.class), FIXTURE)
                        .build()
                )));
            }
            List<AssetLoad> loads = new ArrayList<>();
            for (Future<AssetLoad> submission : submissions) {
                loads.add(submission.get());
            }
            pumpUntilDone(service, loads);

            List<AssetGroupHandle> groups = loads.stream()
                .map(load -> load.completion().toCompletableFuture().join())
                .toList();
            assertEquals(1, tracker.loads(FIXTURE));
            assertEquals(12, service.metrics().liveAssetReferences());

            for (AssetGroupHandle group : groups) {
                group.close();
            }
            assertEquals(1, tracker.disposals(FIXTURE));
            assertTrue(service.metrics().isAtResourceBaseline());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void twentySceneChangesReturnEveryCounterToBaseline() {
        Tracker tracker = new Tracker();
        GdxAssetService service = newService(tracker);
        GameRuntime runtime = new GameRuntime();
        GameContext execution = runtime.context();
        execution.resources().register(
            execution.executionOwner(),
            "asset-service",
            service,
            GdxAssetService::close
        );

        runtime.start(new AssetScene(service, 0, 20));
        for (int index = 0; index < 20; index++) {
            runtime.fixedUpdate(1.0 / 60.0);
        }
        runtime.close();
        service.close();

        AssetMetrics metrics = service.metrics();
        assertTrue(service.isClosed());
        assertEquals(21, metrics.groupsLoaded());
        assertEquals(21, metrics.groupsUnloaded());
        assertEquals(21, metrics.assetReferencesLoaded());
        assertEquals(21, metrics.assetReferencesUnloaded());
        assertEquals(21, metrics.backendLoads());
        assertEquals(21, metrics.backendUnloads());
        assertEquals(21, tracker.loads(FIXTURE));
        assertEquals(21, tracker.disposals(FIXTURE));
        assertTrue(metrics.isAtResourceBaseline());
        assertFalse(runtime.metrics().lastClosedContext().resources().hasLeaks());
    }

    private static GdxAssetService newService(Tracker tracker) {
        return newService(tracker, ClasspathHandle::new);
    }

    private static GdxAssetService newService(
        Tracker tracker,
        FileHandleResolver resolver
    ) {
        AssetManager manager = new AssetManager(resolver, false);
        manager.setLoader(TrackedData.class, new TrackedAssetLoader(resolver, tracker));
        manager.setLoader(OtherData.class, new OtherAssetLoader(resolver));
        manager.setLoader(BrokenData.class, new BrokenAssetLoader(resolver));
        return new GdxAssetService(manager);
    }

    private static AssetGroupHandle loadFully(
        GdxAssetService service,
        AssetManifest manifest
    ) {
        return finish(service, service.load(manifest));
    }

    private static AssetGroupHandle finish(GdxAssetService service, AssetLoad load) {
        pumpUntilDone(service, List.of(load));
        return load.completion().toCompletableFuture().join();
    }

    private static void pumpUntilDone(GdxAssetService service, List<AssetLoad> loads) {
        for (int iteration = 0; iteration < 2_000; iteration++) {
            if (loads.stream().allMatch(AssetLoad::isDone)) {
                return;
            }
            service.update();
            Thread.onSpinWait();
        }
        throw new AssertionError("Timed out while pumping typed asset loads");
    }

    private static final class AssetScene implements RuntimeScene {
        private static final AssetId<TrackedData> DATA = AssetId.of(
            "scene.shared",
            TrackedData.class
        );

        private final GdxAssetService service;
        private final int index;
        private final int finalIndex;

        private AssetScene(GdxAssetService service, int index, int finalIndex) {
            this.service = service;
            this.index = index;
            this.finalIndex = finalIndex;
        }

        @Override
        public void create(GameContext context) {
            AssetGroupHandle group = loadFully(
                service,
                AssetManifest.builder("scene-" + index).add(DATA, FIXTURE).build()
            );
            assertTrue(group.handle(DATA).value().contents().contains("palette"));
            context.resources().register(
                this,
                "asset-group",
                group,
                AssetGroupHandle::close
            );
        }

        @Override
        public void enter(GameContext context) {
        }

        @Override
        public void fixedUpdate(GameContext context, double fixedDeltaSeconds) {
            if (index < finalIndex) {
                context.requestScene(new AssetScene(service, index + 1, finalIndex));
            }
        }

        @Override
        public void render(GameContext context, double interpolationAlpha) {
        }

        @Override
        public void exit(GameContext context) {
        }

        @Override
        public void dispose(GameContext context) {
        }
    }

    private interface TrackedData extends SharedAssetData {
        String contents();
    }

    private interface OtherData extends SharedAssetData {
        String contents();
    }

    private interface BrokenData extends SharedAssetData {
    }

    private record OtherAsset(String contents) implements OtherData {
    }

    private static final class ClasspathHandle extends FileHandle {
        private ClasspathHandle(String path) {
            super(path, Files.FileType.Classpath);
        }
    }

    private static final class TrackedAsset implements TrackedData, Disposable {
        private final String source;
        private final String contents;
        private final Tracker tracker;
        private boolean disposed;

        private TrackedAsset(String source, String contents, Tracker tracker) {
            this.source = source;
            this.contents = contents;
            this.tracker = tracker;
        }

        @Override
        public String contents() {
            return contents;
        }

        @Override
        public void dispose() {
            if (disposed) {
                throw new AssertionError("Tracked asset was disposed twice: " + source);
            }
            disposed = true;
            tracker.recordDisposal(source);
        }
    }

    private static final class Tracker {
        private final Map<String, AtomicInteger> loads = new ConcurrentHashMap<>();
        private final Map<String, AtomicInteger> disposals = new ConcurrentHashMap<>();

        void recordLoad(String path) {
            loads.computeIfAbsent(path, ignored -> new AtomicInteger()).incrementAndGet();
        }

        void recordDisposal(String path) {
            disposals.computeIfAbsent(path, ignored -> new AtomicInteger()).incrementAndGet();
        }

        int loads(String path) {
            return loads.getOrDefault(path, new AtomicInteger()).get();
        }

        int disposals(String path) {
            return disposals.getOrDefault(path, new AtomicInteger()).get();
        }
    }

    private static final class TrackedAssetLoader extends SynchronousAssetLoader<
        TrackedData,
        AssetLoaderParameters<TrackedData>
    > {
        private final Tracker tracker;

        private TrackedAssetLoader(FileHandleResolver resolver, Tracker tracker) {
            super(resolver);
            this.tracker = tracker;
        }

        @Override
        public TrackedData load(
            AssetManager manager,
            String fileName,
            FileHandle file,
            AssetLoaderParameters<TrackedData> parameter
        ) {
            tracker.recordLoad(fileName);
            return new TrackedAsset(fileName, file.readString("UTF-8"), tracker);
        }

        @Override
        public Array<AssetDescriptor> getDependencies(
            String fileName,
            FileHandle file,
            AssetLoaderParameters<TrackedData> parameter
        ) {
            return null;
        }
    }

    private static final class OtherAssetLoader extends SynchronousAssetLoader<
        OtherData,
        AssetLoaderParameters<OtherData>
    > {
        private OtherAssetLoader(FileHandleResolver resolver) {
            super(resolver);
        }

        @Override
        public OtherData load(
            AssetManager manager,
            String fileName,
            FileHandle file,
            AssetLoaderParameters<OtherData> parameter
        ) {
            return new OtherAsset(file.readString("UTF-8"));
        }

        @Override
        public Array<AssetDescriptor> getDependencies(
            String fileName,
            FileHandle file,
            AssetLoaderParameters<OtherData> parameter
        ) {
            return null;
        }
    }

    private static final class BrokenAssetLoader extends SynchronousAssetLoader<
        BrokenData,
        AssetLoaderParameters<BrokenData>
    > {
        private BrokenAssetLoader(FileHandleResolver resolver) {
            super(resolver);
        }

        @Override
        public BrokenData load(
            AssetManager manager,
            String fileName,
            FileHandle file,
            AssetLoaderParameters<BrokenData> parameter
        ) {
            throw new IllegalArgumentException("controlled loader failure for " + fileName);
        }

        @Override
        public Array<AssetDescriptor> getDependencies(
            String fileName,
            FileHandle file,
            AssetLoaderParameters<BrokenData> parameter
        ) {
            return null;
        }
    }
}
