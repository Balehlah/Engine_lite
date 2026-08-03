package engine.incubator.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("specification")
final class AssetManifestTest {
    @Test
    void typedIdsAndManifestEntriesAreImmutable() {
        AssetId<TextData> text = AssetId.of("ui.title", TextData.class);
        AssetManifest manifest = AssetManifest.builder("menu")
            .add(text, "ui/title.txt", "fallback/title.txt")
            .build();

        assertEquals("ui.title", text.value());
        assertEquals(TextData.class, text.type());
        assertEquals("menu", manifest.groupId());
        assertEquals(List.of(AssetEntry.withFallback(
            text,
            "ui/title.txt",
            "fallback/title.txt"
        )), manifest.entries());
        assertThrows(
            UnsupportedOperationException.class,
            () -> manifest.entries().clear()
        );
    }

    @Test
    void duplicateIdsAndCandidateSourcesFailWhileBuildingTheManifest() {
        AssetId<TextData> text = AssetId.of("shared", TextData.class);
        AssetId<BytesData> bytes = AssetId.of("shared", BytesData.class);

        AssetManifest.Builder duplicateId = AssetManifest.builder("duplicate-id")
            .add(text, "content/text.txt");
        assertThrows(
            IllegalArgumentException.class,
            () -> duplicateId.add(bytes, "content/data.bin")
        );

        AssetManifest.Builder duplicateSource = AssetManifest.builder("duplicate-source")
            .add(text, "content/shared.dat");
        assertThrows(
            IllegalArgumentException.class,
            () -> duplicateSource.add(
                AssetId.of("alias", TextData.class),
                "content/shared.dat"
            )
        );
    }

    @Test
    void pathsArePortableRelativeAndTraversalSafe() {
        AssetId<TextData> id = AssetId.of("unsafe", TextData.class);

        assertThrows(
            IllegalArgumentException.class,
            () -> AssetManifest.builder("absolute").add(id, "C:/assets/file.txt")
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> AssetManifest.builder("rooted").add(id, "/assets/file.txt")
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> AssetManifest.builder("traversal").add(id, "assets/../file.txt")
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> AssetManifest.builder("windows").add(id, "assets\\file.txt")
        );
    }

    @Test
    void progressAndMetricsExposeExactBaselineState() {
        AssetProgress progress = new AssetProgress("loading", 1, 4);
        assertEquals(0.25, progress.fraction());

        AssetMetrics metrics = new AssetMetrics(
            3,
            2,
            2,
            4,
            4,
            2,
            2,
            1,
            1,
            0,
            0,
            0,
            0
        );
        assertEquals(true, metrics.isAtResourceBaseline());
    }

    private record TextData(String value) implements SharedAssetData {
    }

    private record BytesData(List<Byte> value) implements SharedAssetData {
    }
}
