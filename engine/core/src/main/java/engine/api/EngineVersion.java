package engine.api;

/**
 * Provides the version embedded in a packaged Engine Lite artifact.
 */
public final class EngineVersion {
    private static final String DEVELOPMENT_VERSION = "development";

    private EngineVersion() {
    }

    /**
     * Returns the artifact implementation version, or {@code development} when
     * running directly from compiled classes.
     *
     * @return the current Engine Lite artifact version
     */
    public static String current() {
        String implementationVersion =
                EngineVersion.class.getPackage().getImplementationVersion();
        return implementationVersion == null ? DEVELOPMENT_VERSION : implementationVersion;
    }
}
