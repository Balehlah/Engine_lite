package engine.incubator.gdx.assets;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.SynchronousAssetLoader;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import engine.incubator.assets.AssetGroupHandle;
import engine.incubator.assets.AssetId;
import engine.incubator.assets.AssetLoad;
import engine.incubator.assets.AssetManifest;
import engine.incubator.assets.SharedAssetData;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

/** Forked acceptance probe executed by {@code assetServiceCwdSmoke}. */
public final class AssetServiceCwdProbe {
    private static final String SOURCE = "spike/sprite.rgba";

    private AssetServiceCwdProbe() {
    }

    public static void main(String[] arguments) {
        FileHandleResolver resolver = ClasspathHandle::new;
        AtomicInteger disposals = new AtomicInteger();
        AssetManager manager = new AssetManager(resolver, false);
        manager.setLoader(ProbeData.class, new ProbeLoader(resolver, disposals));

        try (GdxAssetService service = new GdxAssetService(manager)) {
            AssetId<ProbeData> id = AssetId.of("cwd.probe", ProbeData.class);
            AssetLoad load = service.load(
                AssetManifest.builder("cwd-probe").add(id, SOURCE).build()
            );
            for (int iteration = 0; !load.isDone() && iteration < 2_000; iteration++) {
                service.update();
                Thread.onSpinWait();
            }
            if (!load.isDone()) {
                throw new IllegalStateException("Timed out loading the CWD probe manifest");
            }
            AssetGroupHandle group = load.completion().toCompletableFuture().join();
            if (!group.handle(id).value().contents().contains("palette")) {
                throw new IllegalStateException("Unexpected CWD probe fixture contents");
            }
            group.close();
            if (disposals.get() != 1 || !service.metrics().isAtResourceBaseline()) {
                throw new IllegalStateException(
                    "CWD probe did not return to baseline: " + service.metrics()
                );
            }
        }

        System.out.println(
            "asset-service-cwd-smoke=PASS;cwd=" + Path.of("").toAbsolutePath()
        );
    }

    private interface ProbeData extends SharedAssetData {
        String contents();
    }

    private static final class ProbeValue implements ProbeData, Disposable {
        private final String contents;
        private final AtomicInteger disposals;
        private boolean disposed;

        private ProbeValue(String contents, AtomicInteger disposals) {
            this.contents = contents;
            this.disposals = disposals;
        }

        @Override
        public String contents() {
            return contents;
        }

        @Override
        public void dispose() {
            if (disposed) {
                throw new IllegalStateException("CWD probe asset disposed twice");
            }
            disposed = true;
            disposals.incrementAndGet();
        }
    }

    private static final class ProbeLoader extends SynchronousAssetLoader<
        ProbeData,
        AssetLoaderParameters<ProbeData>
    > {
        private final AtomicInteger disposals;

        private ProbeLoader(FileHandleResolver resolver, AtomicInteger disposals) {
            super(resolver);
            this.disposals = disposals;
        }

        @Override
        public ProbeData load(
            AssetManager manager,
            String fileName,
            FileHandle file,
            AssetLoaderParameters<ProbeData> parameter
        ) {
            return new ProbeValue(file.readString("UTF-8"), disposals);
        }

        @Override
        public Array<AssetDescriptor> getDependencies(
            String fileName,
            FileHandle file,
            AssetLoaderParameters<ProbeData> parameter
        ) {
            return null;
        }
    }

    private static final class ClasspathHandle extends FileHandle {
        private ClasspathHandle(String path) {
            super(path, Files.FileType.Classpath);
        }
    }
}
