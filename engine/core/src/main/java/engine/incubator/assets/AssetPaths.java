package engine.incubator.assets;

import java.util.Objects;

final class AssetPaths {
    private AssetPaths() {
    }

    static String requirePortable(String path, String role) {
        Objects.requireNonNull(path, role);
        if (path.isBlank()) {
            throw new IllegalArgumentException(role + " must not be blank");
        }
        if (path.indexOf('\\') >= 0 || path.startsWith("/") || hasDrivePrefix(path)) {
            throw new IllegalArgumentException(
                role + " must be a portable classpath-relative path: " + path
            );
        }
        String[] segments = path.split("/", -1);
        for (String segment : segments) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new IllegalArgumentException(
                    role + " contains an unsafe segment: " + path
                );
            }
        }
        return path;
    }

    private static boolean hasDrivePrefix(String path) {
        return path.length() >= 2
            && Character.isLetter(path.charAt(0))
            && path.charAt(1) == ':';
    }
}
