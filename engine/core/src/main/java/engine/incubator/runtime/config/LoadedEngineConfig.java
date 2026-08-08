package engine.incubator.runtime.config;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Effective configuration plus provenance and unconsumed host arguments. */
public record LoadedEngineConfig(
    EngineConfig configuration,
    Path applicationHome,
    Optional<Path> configurationFile,
    Map<String, ConfigurationSource> sources,
    List<String> remainingArguments
) {
    public LoadedEngineConfig {
        Objects.requireNonNull(configuration, "configuration");
        Objects.requireNonNull(applicationHome, "applicationHome");
        Objects.requireNonNull(configurationFile, "configurationFile");
        Objects.requireNonNull(sources, "sources");
        Objects.requireNonNull(remainingArguments, "remainingArguments");
        if (!applicationHome.isAbsolute()) {
            throw new IllegalArgumentException("applicationHome must be absolute");
        }
        applicationHome = applicationHome.normalize();
        configurationFile = configurationFile.map(Path::normalize);
        sources = Map.copyOf(sources);
        remainingArguments = List.copyOf(remainingArguments);
    }
}
