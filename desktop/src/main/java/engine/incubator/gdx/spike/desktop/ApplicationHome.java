package engine.incubator.gdx.spike.desktop;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Objects;

/** Resolves the installed application root from its code source, never from CWD. */
final class ApplicationHome {
    private ApplicationHome() {
    }

    static Path resolve(Class<?> anchor) {
        Objects.requireNonNull(anchor, "anchor");
        try {
            Path location = Path.of(
                anchor.getProtectionDomain().getCodeSource().getLocation().toURI()
            ).toAbsolutePath().normalize();
            if (location.getFileName() != null
                && location.getFileName().toString().endsWith(".jar")) {
                Path parent = location.getParent();
                if (parent != null
                    && parent.getFileName() != null
                    && "lib".equals(parent.getFileName().toString())) {
                    return parent.getParent().toAbsolutePath().normalize();
                }
                return Objects.requireNonNull(parent, "code source parent")
                    .toAbsolutePath()
                    .normalize();
            }
            return location;
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Unable to resolve application home", exception);
        }
    }
}
