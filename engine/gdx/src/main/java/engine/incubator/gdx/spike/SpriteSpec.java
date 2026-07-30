package engine.incubator.gdx.spike;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class SpriteSpec {
    private final int width;
    private final int height;
    private final int[] rgba;

    private SpriteSpec(int width, int height, int[] rgba) {
        this.width = width;
        this.height = height;
        this.rgba = rgba;
    }

    static SpriteSpec parse(String source) {
        Objects.requireNonNull(source, "source");
        Map<Character, Integer> palette = new LinkedHashMap<>();
        List<String> rows = new ArrayList<>();
        boolean pixelsStarted = false;

        for (String rawLine : source.replace("\r\n", "\n").split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if ("pixels".equals(line)) {
                if (pixelsStarted) {
                    throw new IllegalArgumentException("Duplicate pixels marker.");
                }
                pixelsStarted = true;
                continue;
            }
            if (!pixelsStarted) {
                String[] fields = line.split("\\s+");
                if (fields.length != 3 || !"palette".equals(fields[0])) {
                    throw new IllegalArgumentException(
                        "Expected 'palette <character> <rgba8888>': " + line
                    );
                }
                if (fields[1].length() != 1 || fields[2].length() != 8) {
                    throw new IllegalArgumentException("Invalid palette row: " + line);
                }
                char key = fields[1].charAt(0);
                int color;
                try {
                    color = (int) Long.parseUnsignedLong(fields[2], 16);
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException(
                        "Invalid RGBA8888 value: " + fields[2],
                        exception
                    );
                }
                if (palette.putIfAbsent(key, color) != null) {
                    throw new IllegalArgumentException("Duplicate palette key: " + key);
                }
            } else {
                rows.add(line);
            }
        }

        if (palette.isEmpty() || rows.isEmpty()) {
            throw new IllegalArgumentException("Sprite must define a palette and pixels.");
        }
        int width = rows.getFirst().length();
        if (width == 0 || rows.stream().anyMatch(row -> row.length() != width)) {
            throw new IllegalArgumentException("Sprite rows must have one non-zero width.");
        }

        int[] rgba = new int[Math.multiplyExact(width, rows.size())];
        for (int y = 0; y < rows.size(); y++) {
            for (int x = 0; x < width; x++) {
                char key = rows.get(y).charAt(x);
                Integer color = palette.get(key);
                if (color == null) {
                    throw new IllegalArgumentException(
                        "Pixel uses undefined palette key '" + key + "'."
                    );
                }
                rgba[y * width + x] = color;
            }
        }
        return new SpriteSpec(width, rows.size(), rgba);
    }

    int width() {
        return width;
    }

    int height() {
        return height;
    }

    int rgbaAt(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            throw new IndexOutOfBoundsException("(" + x + "," + y + ")");
        }
        return rgba[y * width + x];
    }
}
