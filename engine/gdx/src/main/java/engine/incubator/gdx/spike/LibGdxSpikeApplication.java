package engine.incubator.gdx.spike;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.math.Matrix4;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * One-screen, removable libGDX acceptance spike.
 */
public final class LibGdxSpikeApplication extends ApplicationAdapter {
    private static final int VIRTUAL_WIDTH = SpikeGraphicsPolicy.VIRTUAL_WIDTH;
    private static final int VIRTUAL_HEIGHT = SpikeGraphicsPolicy.VIRTUAL_HEIGHT;
    private static final int SPRITE_WIDTH = 16;
    private static final int SPRITE_HEIGHT = 16;
    private static final int SPRITE_BASE_X = 144;
    private static final int SPRITE_Y = 82;

    private static final int BLACK_RGBA = 0x000000ff;
    private static final int BACKGROUND_RGBA = 0x101828ff;

    private static final Fixture[] FIXTURES = {
        new Fixture(640, 360, 2, 0, 0),
        new Fixture(800, 600, 2, 80, 120),
        new Fixture(1280, 720, 4, 0, 0),
    };

    private final SpikeRunConfiguration configuration;
    private final DisposableRegistry disposables = new DisposableRegistry();
    private final Matrix4 virtualProjection = new Matrix4();
    private final Matrix4 backbufferProjection = new Matrix4();

    private EvidenceWriter evidence;
    private SpriteBatch batch;
    private SpriteSpec spriteSpec;
    private Texture spriteTexture;
    private FrameBuffer virtualFrameBuffer;
    private TextureRegion virtualFrameBufferRegion;
    private SpikeInputProcessor inputProcessor;
    private FixedTimestepLoop fixedTimestepLoop;
    private FixedTimestepDebugOverlay fixedTimestepDebugOverlay;
    private double simulationTimeSeconds;

    private int fixtureIndex;
    private int fixtureFrames;
    private int stableFrames;
    private int inputWaitFrames;
    private int inputEventSequence;
    private int inputSequenceBeforeRequest;
    private int spriteOffsetX;
    private boolean resizeRequested;
    private boolean smokeInputRequested;
    private boolean requestedCursorEventObserved;
    private boolean smokeCompleted;
    private boolean disposed;

    public LibGdxSpikeApplication(SpikeRunConfiguration configuration) {
        this.configuration = java.util.Objects.requireNonNull(
            configuration,
            "configuration"
        );
    }

    @Override
    public void create() {
        evidence = new EvidenceWriter(configuration.evidenceDirectory());
        evidence.beginRun();
        evidence.append("lifecycle.log", "create.begin");
        try {
            recordEnvironment();
            createRenderingResources();
            loadTiledProbe();
            runAudioProbe();
            installInputProcessor();
            fixedTimestepLoop = FixedTimestepLoop.createDefault();
            recordTimingPolicy();
            evidence.append("lifecycle.log", "create.end");
        } catch (Throwable failure) {
            recordFailure("create", failure);
            cleanupAfterFailedCreate(failure);
            throwUnchecked(failure);
        }
    }

    @Override
    public void resize(int width, int height) {
        if (evidence != null) {
            evidence.append(
                "lifecycle.log",
                "resize.callback="
                    + width
                    + "x"
                    + height
                    + ";backbuffer="
                    + Gdx.graphics.getBackBufferWidth()
                    + "x"
                    + Gdx.graphics.getBackBufferHeight()
            );
        }
    }

    @Override
    public void render() {
        try {
            fixedTimestepLoop.runFrame(
                this::updateSimulation,
                (alpha, metrics) -> {
                    renderVirtualScene();
                    renderBackbuffer();
                    if (!configuration.smoke()) {
                        fixedTimestepDebugOverlay.render(
                            batch,
                            backbufferProjection,
                            Gdx.graphics.getBackBufferHeight(),
                            metrics
                        );
                    }
                    if (configuration.smoke()) {
                        advanceSmoke();
                    }
                }
            );
        } catch (Throwable failure) {
            recordFailure("render", failure);
            throwUnchecked(failure);
        }
    }

    @Override
    public void pause() {
        if (fixedTimestepLoop != null) {
            fixedTimestepLoop.pause();
        }
        if (evidence != null) {
            evidence.append("lifecycle.log", "pause");
        }
    }

    @Override
    public void resume() {
        if (fixedTimestepLoop != null) {
            fixedTimestepLoop.resume();
        }
        if (evidence != null) {
            evidence.append("lifecycle.log", "resume");
        }
    }

    @Override
    public void dispose() {
        if (disposed) {
            throw new IllegalStateException(
                "ApplicationListener.dispose() was invoked more than once."
            );
        }
        disposed = true;
        evidence.append("lifecycle.log", "dispose.begin");
        Gdx.input.setInputProcessor(null);

        try {
            recordTimingMetrics();
            disposables.disposeAll(line -> evidence.append("dispose.log", line));
            if (configuration.smoke() && !smokeCompleted) {
                throw new IllegalStateException(
                    "Smoke ended before every acceptance fixture completed."
                );
            }
            StringBuilder counts = new StringBuilder();
            disposables.disposalCounts().forEach((name, count) ->
                counts.append(name).append('=').append(count).append('\n')
            );
            evidence.write(
                "summary.properties",
                "result=PASS\n"
                    + "fixtures="
                    + fixtureIndex
                    + "\n"
                    + "input.events="
                    + inputEventSequence
                    + "\n"
                    + "disposables="
                    + disposables.size()
                    + "\n"
                    + counts
            );
            evidence.append("lifecycle.log", "dispose.end;result=PASS");
        } catch (Throwable failure) {
            recordFailure("dispose", failure);
            throwUnchecked(failure);
        }
    }

    private void recordEnvironment() {
        evidence.append("probe.log", "cwd=" + System.getProperty("user.dir"));
        evidence.append(
            "probe.log",
            "java.version=" + System.getProperty("java.version")
        );
        evidence.append(
            "probe.log",
            "os="
                + System.getProperty("os.name")
                + ";arch="
                + System.getProperty("os.arch")
        );
        evidence.append(
            "probe.log",
            "graphics.logical="
                + Gdx.graphics.getWidth()
                + "x"
                + Gdx.graphics.getHeight()
                + ";backbuffer="
                + Gdx.graphics.getBackBufferWidth()
                + "x"
                + Gdx.graphics.getBackBufferHeight()
        );
        evidence.append(
            "probe.log",
            "gl.vendor=" + Gdx.gl.glGetString(GL20.GL_VENDOR)
        );
        evidence.append(
            "probe.log",
            "gl.renderer=" + Gdx.gl.glGetString(GL20.GL_RENDERER)
        );
        evidence.append(
            "probe.log",
            "gl.version=" + Gdx.gl.glGetString(GL20.GL_VERSION)
        );
    }

    private void createRenderingResources() {
        FileHandle spriteHandle = Gdx.files.internal("spike/sprite.rgba");
        require(spriteHandle.exists(), "Internal sprite asset is missing.");
        byte[] spriteBytes = spriteHandle.readBytes();
        evidence.append(
            "probe.log",
            "asset.sprite.type="
                + spriteHandle.type()
                + ";path="
                + spriteHandle.path()
                + ";sha256="
                + sha256(spriteBytes)
        );

        spriteSpec = SpriteSpec.parse(
            new String(spriteBytes, StandardCharsets.UTF_8)
        );
        Pixmap spritePixmap = disposables.own(
            "sprite-pixmap",
            new Pixmap(
                spriteSpec.width(),
                spriteSpec.height(),
                Pixmap.Format.RGBA8888
            )
        );
        spritePixmap.setBlending(Pixmap.Blending.None);
        for (int sourceY = 0; sourceY < spriteSpec.height(); sourceY++) {
            for (int x = 0; x < spriteSpec.width(); x++) {
                spritePixmap.drawPixel(
                    x,
                    sourceY,
                    spriteSpec.rgbaAt(x, sourceY)
                );
            }
        }

        spriteTexture = disposables.own(
            "sprite-texture",
            new Texture(spritePixmap)
        );
        spriteTexture.setFilter(
            SpikeGraphicsPolicy.MIN_FILTER,
            SpikeGraphicsPolicy.MAG_FILTER
        );

        virtualFrameBuffer = disposables.own(
            "virtual-framebuffer",
            new FrameBuffer(
                Pixmap.Format.RGBA8888,
                VIRTUAL_WIDTH,
                VIRTUAL_HEIGHT,
                false
            )
        );
        virtualFrameBuffer.getColorBufferTexture().setFilter(
            SpikeGraphicsPolicy.MIN_FILTER,
            SpikeGraphicsPolicy.MAG_FILTER
        );
        virtualFrameBufferRegion = new TextureRegion(
            virtualFrameBuffer.getColorBufferTexture()
        );
        virtualFrameBufferRegion.flip(false, true);
        batch = disposables.own("sprite-batch", new SpriteBatch());
        fixedTimestepDebugOverlay = disposables.own(
            "fixed-timestep-debug-overlay",
            new FixedTimestepDebugOverlay()
        );

        virtualProjection.setToOrtho2D(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        evidence.append(
            "probe.log",
            "render.virtual="
                + VIRTUAL_WIDTH
                + "x"
                + VIRTUAL_HEIGHT
                + ";sprite="
                + spriteSpec.width()
                + "x"
                + spriteSpec.height()
                + ";sprite.filter="
                + spriteTexture.getMinFilter()
                + "/"
                + spriteTexture.getMagFilter()
                + ";framebuffer.filter="
                + virtualFrameBuffer.getColorBufferTexture().getMinFilter()
                + "/"
                + virtualFrameBuffer.getColorBufferTexture().getMagFilter()
        );
    }

    private void loadTiledProbe() {
        FileHandle tiledHandle = Gdx.files.internal("spike/probe.tmx");
        require(tiledHandle.exists(), "Internal Tiled probe is missing.");
        TiledMap tiledMap = disposables.own(
            "tiled-map",
            new TmxMapLoader().load("spike/probe.tmx")
        );
        MapLayer probeLayer = tiledMap.getLayers().get("probes");
        require(probeLayer != null, "Tiled probe layer 'probes' was not loaded.");
        require(
            probeLayer.getObjects().getCount() == 1,
            "Tiled probe must expose exactly one object."
        );
        evidence.append(
            "probe.log",
            "tiled=PASS;layers="
                + tiledMap.getLayers().getCount()
                + ";objects="
                + probeLayer.getObjects().getCount()
                + ";sha256="
                + sha256(tiledHandle.readBytes())
        );
    }

    private void runAudioProbe() {
        String audioImplementation = Gdx.audio.getClass().getName();
        String lowerAudioImplementation = audioImplementation.toLowerCase();
        require(
            !lowerAudioImplementation.contains(".audio.mock.")
                && !audioImplementation.endsWith("MockAudio"),
            "LWJGL3 silently substituted MockAudio: " + audioImplementation
        );

        var wavePath = evidence.resolve("probe-tone.wav");
        try {
            Files.write(wavePath, createWaveTone());
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Unable to write audio probe.", exception);
        }
        Sound sound = disposables.own(
            "audio-sound",
            Gdx.audio.newSound(Gdx.files.absolute(wavePath.toString()))
        );
        long soundId = sound.play(0f);
        require(soundId >= 0, "OpenAL could not allocate a source for the probe.");
        sound.stop(soundId);
        evidence.append(
            "probe.log",
            "audio=PASS;implementation="
                + audioImplementation
                + ";soundId="
                + soundId
                + ";volume=0"
                + ";wav.sha256="
                + sha256(createWaveTone())
        );
    }

    private void installInputProcessor() {
        inputProcessor = new SpikeInputProcessor();
        Gdx.input.setInputProcessor(inputProcessor);
        require(
            Gdx.input.getInputProcessor() == inputProcessor,
            "The libGDX InputProcessor was not installed."
        );
        evidence.append(
            "probe.log",
            "input.processor=PASS;implementation="
                + Gdx.input.getClass().getName()
        );
    }

    private void updateSimulation(double fixedDeltaSeconds) {
        simulationTimeSeconds += fixedDeltaSeconds;
    }

    private void recordTimingPolicy() {
        var timing = fixedTimestepLoop.configuration();
        evidence.append(
            "timing.log",
            "fixed.updates-per-second="
                + timing.updatesPerSecond()
                + ";fixed.dt-seconds="
                + timing.fixedDeltaSeconds()
                + ";fixed.step-nanos="
                + timing.fixedStepNanos()
                + ";clamp-nanos="
                + timing.maximumFrameTimeNanos()
                + ";max-catch-up="
                + timing.maximumCatchUpSteps()
        );
    }

    private void recordTimingMetrics() {
        if (fixedTimestepLoop == null || evidence == null) {
            return;
        }
        var metrics = fixedTimestepLoop.metrics();
        evidence.append(
            "timing.log",
            "frames="
                + metrics.frameCount()
                + ";updates="
                + metrics.updateCount()
                + ";simulation-seconds="
                + simulationTimeSeconds
                + ";alpha="
                + metrics.interpolationAlpha()
                + ";clamped-frames="
                + metrics.clampedFrameCount()
                + ";clamped-wall-nanos="
                + metrics.clampedWallTimeNanos()
                + ";catch-up-limit-hits="
                + metrics.catchUpLimitHitCount()
                + ";catch-up-discarded-nanos="
                + metrics.catchUpDiscardedSimulationTimeNanos()
                + ";inactive-wall-nanos="
                + metrics.inactiveWallTimeNanos()
        );
    }

    private void renderVirtualScene() {
        virtualFrameBuffer.begin();
        Gdx.gl.glViewport(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        Gdx.gl.glClearColor(
            red(BACKGROUND_RGBA),
            green(BACKGROUND_RGBA),
            blue(BACKGROUND_RGBA),
            alpha(BACKGROUND_RGBA)
        );
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(virtualProjection);
        batch.begin();
        batch.draw(
            spriteTexture,
            SPRITE_BASE_X + spriteOffsetX,
            SPRITE_Y,
            SPRITE_WIDTH,
            SPRITE_HEIGHT
        );
        batch.end();
        virtualFrameBuffer.end();
    }

    private void renderBackbuffer() {
        int backbufferWidth = Gdx.graphics.getBackBufferWidth();
        int backbufferHeight = Gdx.graphics.getBackBufferHeight();
        IntegerViewport viewport = IntegerViewport.calculate(
            backbufferWidth,
            backbufferHeight,
            VIRTUAL_WIDTH,
            VIRTUAL_HEIGHT
        );

        Gdx.gl.glViewport(0, 0, backbufferWidth, backbufferHeight);
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        backbufferProjection.setToOrtho2D(0, 0, backbufferWidth, backbufferHeight);
        batch.setProjectionMatrix(backbufferProjection);
        batch.begin();
        batch.draw(
            virtualFrameBufferRegion,
            viewport.x(),
            viewport.y(),
            viewport.width(),
            viewport.height()
        );
        batch.end();
    }

    private void advanceSmoke() {
        if (smokeCompleted) {
            return;
        }
        Fixture fixture = FIXTURES[fixtureIndex];
        fixtureFrames++;
        require(
            fixtureFrames <= 300,
            "Timed out waiting for fixture " + fixture.fileStem()
        );

        int backbufferWidth = Gdx.graphics.getBackBufferWidth();
        int backbufferHeight = Gdx.graphics.getBackBufferHeight();
        if (
            backbufferWidth != fixture.width()
                || backbufferHeight != fixture.height()
        ) {
            stableFrames = 0;
            if (!resizeRequested) {
                LogicalWindowSize logicalWindowSize =
                    LogicalWindowSize.forBackbuffer(
                        fixture.width(),
                        fixture.height(),
                        Gdx.graphics.getWidth(),
                        Gdx.graphics.getHeight(),
                        backbufferWidth,
                        backbufferHeight
                    );
                boolean accepted = Gdx.graphics.setWindowedMode(
                    logicalWindowSize.width(),
                    logicalWindowSize.height()
                );
                require(
                    accepted,
                    "Backend rejected window size " + fixture.fileStem()
                );
                resizeRequested = true;
                evidence.append(
                    "lifecycle.log",
                    "resize.request="
                        + fixture.fileStem()
                        + ";logical="
                        + logicalWindowSize.width()
                        + "x"
                        + logicalWindowSize.height()
                );
            }
            return;
        }

        stableFrames++;
        if (stableFrames < 3) {
            return;
        }

        if (fixtureIndex == 1) {
            if (!smokeInputRequested) {
                smokeInputRequested = true;
                inputSequenceBeforeRequest = inputEventSequence;
                Gdx.input.setCursorPosition(17, 29);
                evidence.append(
                    "probe.log",
                    "input.request=cursor(17,29);sequence="
                        + inputSequenceBeforeRequest
                );
                return;
            }
            if (!requestedCursorEventObserved) {
                inputWaitFrames++;
                require(
                    inputWaitFrames <= 120,
                    "GLFW cursor event did not reach the libGDX InputProcessor."
                );
                return;
            }
            require(
                inputEventSequence > inputSequenceBeforeRequest
                    && spriteOffsetX == 8,
                "The backend cursor event did not update the sprite."
            );
            evidence.append(
                "probe.log",
                "input.backend-event=PASS;sequence="
                    + inputEventSequence
                    + ";sprite.offset="
                    + spriteOffsetX
            );
        }

        captureAndValidate(fixture);
        fixtureIndex++;
        if (fixtureIndex == FIXTURES.length) {
            smokeCompleted = true;
            evidence.append("lifecycle.log", "smoke.complete;exit.request");
            Gdx.app.exit();
            return;
        }

        fixtureFrames = 0;
        stableFrames = 0;
        resizeRequested = false;
    }

    private void captureAndValidate(Fixture fixture) {
        IntegerViewport viewport = IntegerViewport.calculate(
            fixture.width(),
            fixture.height(),
            VIRTUAL_WIDTH,
            VIRTUAL_HEIGHT
        );
        require(!viewport.degraded(), "Acceptance fixture entered degraded mode.");
        require(
            viewport.scale() == fixture.scale()
                && viewport.x() == fixture.barX()
                && viewport.y() == fixture.barY(),
            "Unexpected viewport for " + fixture.fileStem() + ": " + viewport
        );
        requireNearestFilters();

        Pixmap screenshot = disposables.own(
            "screenshot-" + fixture.fileStem(),
            Pixmap.createFromFrameBuffer(
                0,
                0,
                fixture.width(),
                fixture.height()
            )
        );
        validateGolden(screenshot, viewport, fixture);
        var screenshotPath = evidence.resolve(
            "viewport-" + fixture.fileStem() + ".png"
        );
        PixmapIO.writePNG(
            Gdx.files.absolute(screenshotPath.toString()),
            screenshot,
            -1,
            true
        );
        evidence.append(
            "viewport.log",
            fixture.fileStem()
                + "=PASS;scale="
                + viewport.scale()
                + ";viewport="
                + viewport.x()
                + ","
                + viewport.y()
                + ","
                + viewport.width()
                + ","
                + viewport.height()
                + ";bars="
                + viewport.leftBar()
                + ","
                + viewport.bottomBar()
                + ";golden=PASS"
        );
    }

    private void validateGolden(
        Pixmap screenshot,
        IntegerViewport viewport,
        Fixture fixture
    ) {
        if (fixture.barX() > 0) {
            requirePixel(screenshot, fixture.barX() - 1, fixture.height() / 2, BLACK_RGBA);
            requirePixel(screenshot, fixture.barX(), fixture.height() / 2, BACKGROUND_RGBA);
        }
        if (fixture.barY() > 0) {
            requirePixel(screenshot, fixture.width() / 2, fixture.barY() - 1, BLACK_RGBA);
            requirePixel(screenshot, fixture.width() / 2, fixture.barY(), BACKGROUND_RGBA);
        }
        requirePixel(
            screenshot,
            viewport.x() + viewport.width() - 1,
            viewport.y() + 1,
            BACKGROUND_RGBA
        );

        int mismatches = 0;
        int firstMismatchX = -1;
        int firstMismatchY = -1;
        int firstExpected = 0;
        int firstActual = 0;
        for (int y = 0; y < fixture.height(); y++) {
            for (int x = 0; x < fixture.width(); x++) {
                int expected = expectedPixel(x, y, viewport);
                int actual = screenshot.getPixel(x, y);
                if (actual != expected) {
                    mismatches++;
                    if (firstMismatchX < 0) {
                        firstMismatchX = x;
                        firstMismatchY = y;
                        firstExpected = expected;
                        firstActual = actual;
                    }
                }
            }
        }
        require(
            mismatches == 0,
            "Golden diff found "
                + mismatches
                + " mismatched pixels; first at ("
                + firstMismatchX
                + ","
                + firstMismatchY
                + "): expected 0x"
                + Integer.toUnsignedString(firstExpected, 16)
                + ", actual 0x"
                + Integer.toUnsignedString(firstActual, 16)
        );
    }

    private int expectedPixel(int x, int y, IntegerViewport viewport) {
        if (
            x < viewport.x()
                || x >= viewport.x() + viewport.width()
                || y < viewport.y()
                || y >= viewport.y() + viewport.height()
        ) {
            return BLACK_RGBA;
        }

        int virtualX = (x - viewport.x()) / viewport.scale();
        int virtualY = (y - viewport.y()) / viewport.scale();
        int spriteX = SPRITE_BASE_X + spriteOffsetX;
        if (
            virtualX < spriteX
                || virtualX >= spriteX + SPRITE_WIDTH
                || virtualY < SPRITE_Y
                || virtualY >= SPRITE_Y + SPRITE_HEIGHT
        ) {
            return BACKGROUND_RGBA;
        }

        int localX = virtualX - spriteX;
        int localY = virtualY - SPRITE_Y;
        int sourceX = localX * spriteSpec.width() / SPRITE_WIDTH;
        int sourceY = spriteSpec.height()
            - 1
            - (localY * spriteSpec.height() / SPRITE_HEIGHT);
        int spritePixel = spriteSpec.rgbaAt(sourceX, sourceY);
        return (spritePixel & 0xff) == 0 ? BACKGROUND_RGBA : spritePixel;
    }

    private void requireNearestFilters() {
        require(
            spriteTexture.getMinFilter() == SpikeGraphicsPolicy.MIN_FILTER
                && spriteTexture.getMagFilter() == SpikeGraphicsPolicy.MAG_FILTER,
            "Sprite texture filtering is not nearest-neighbor."
        );
        Texture frameTexture = virtualFrameBuffer.getColorBufferTexture();
        require(
            frameTexture.getMinFilter() == SpikeGraphicsPolicy.MIN_FILTER
                && frameTexture.getMagFilter() == SpikeGraphicsPolicy.MAG_FILTER,
            "Framebuffer filtering is not nearest-neighbor."
        );
    }

    private void cleanupAfterFailedCreate(Throwable originalFailure) {
        if (disposed) {
            return;
        }
        disposed = true;
        try {
            disposables.disposeAll(line -> evidence.append("dispose.log", line));
        } catch (Throwable cleanupFailure) {
            originalFailure.addSuppressed(cleanupFailure);
        }
    }

    private void recordFailure(String phase, Throwable failure) {
        if (evidence == null) {
            return;
        }
        var stackTrace = new StringWriter();
        failure.printStackTrace(new PrintWriter(stackTrace));
        evidence.write(
            "failure.log",
            "phase=" + phase + "\n" + stackTrace
        );
        evidence.append(
            "lifecycle.log",
            "result=FAIL;phase="
                + phase
                + ";type="
                + failure.getClass().getName()
                + ";message="
                + String.valueOf(failure.getMessage())
        );
    }

    private static void requirePixel(
        Pixmap pixmap,
        int x,
        int y,
        int expected
    ) {
        int actual = pixmap.getPixel(x, y);
        require(
            actual == expected,
            "Golden pixel mismatch at ("
                + x
                + ","
                + y
                + "): expected 0x"
                + Integer.toUnsignedString(expected, 16)
                + ", actual 0x"
                + Integer.toUnsignedString(actual, 16)
        );
    }

    private static byte[] createWaveTone() {
        int sampleRate = 22_050;
        int sampleCount = sampleRate / 10;
        int dataSize = sampleCount * Short.BYTES;
        ByteBuffer wave = ByteBuffer
            .allocate(44 + dataSize)
            .order(ByteOrder.LITTLE_ENDIAN);
        putAscii(wave, "RIFF");
        wave.putInt(36 + dataSize);
        putAscii(wave, "WAVE");
        putAscii(wave, "fmt ");
        wave.putInt(16);
        wave.putShort((short) 1);
        wave.putShort((short) 1);
        wave.putInt(sampleRate);
        wave.putInt(sampleRate * Short.BYTES);
        wave.putShort((short) Short.BYTES);
        wave.putShort((short) 16);
        putAscii(wave, "data");
        wave.putInt(dataSize);
        for (int sample = 0; sample < sampleCount; sample++) {
            double angle = 2.0 * Math.PI * 440.0 * sample / sampleRate;
            wave.putShort((short) Math.round(Math.sin(angle) * 3_276.0));
        }
        return wave.array();
    }

    private static void putAscii(ByteBuffer buffer, String value) {
        buffer.put(value.getBytes(StandardCharsets.US_ASCII));
    }

    private static String sha256(byte[] contents) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(contents)
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private static float red(int rgba) {
        return ((rgba >>> 24) & 0xff) / 255f;
    }

    private static float green(int rgba) {
        return ((rgba >>> 16) & 0xff) / 255f;
    }

    private static float blue(int rgba) {
        return ((rgba >>> 8) & 0xff) / 255f;
    }

    private static float alpha(int rgba) {
        return (rgba & 0xff) / 255f;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static void throwUnchecked(Throwable failure) {
        if (failure instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (failure instanceof Error error) {
            throw error;
        }
        throw new IllegalStateException(failure);
    }

    private final class SpikeInputProcessor extends InputAdapter {
        @Override
        public boolean mouseMoved(int screenX, int screenY) {
            inputEventSequence++;
            if (!configuration.smoke()) {
                spriteOffsetX = 8;
            } else if (
                smokeInputRequested
                    && Math.abs(screenX - 17) <= 2
                    && Math.abs(screenY - 29) <= 2
            ) {
                requestedCursorEventObserved = true;
                spriteOffsetX = 8;
            }
            evidence.append(
                "probe.log",
                "input.event=mouseMoved("
                    + screenX
                    + ","
                    + screenY
                    + ");sequence="
                    + inputEventSequence
            );
            return true;
        }

        @Override
        public boolean keyDown(int keycode) {
            inputEventSequence++;
            evidence.append(
                "probe.log",
                "input.event=keyDown("
                    + Input.Keys.toString(keycode)
                    + ");sequence="
                    + inputEventSequence
            );
            if (keycode == Input.Keys.ESCAPE) {
                Gdx.app.exit();
            } else if (keycode == Input.Keys.SPACE) {
                spriteOffsetX = spriteOffsetX == 0 ? 8 : 0;
            }
            return true;
        }
    }

    private record Fixture(
        int width,
        int height,
        int scale,
        int barX,
        int barY
    ) {
        String fileStem() {
            return width + "x" + height;
        }
    }
}
