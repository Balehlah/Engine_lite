package engine.io;

import engine.core.Entity;
import engine.core.Scene;
import engine.math.Vector2;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SceneSerializerBaselineTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    @Tag("characterization")
    void sceneRoundTripOverwritesSceneNameAndDropsEntityName() {
        Scene scene = new EmptyScene("Cena ação");
        Entity entity = new Entity("Herói");
        entity.setTag("jogável");
        entity.setPosition(12.5f, -3.25f);
        entity.setRotation(45);
        entity.setScale(new Vector2(2, 3));
        entity.setActive(false);
        scene.addEntityImmediate(entity);
        Path target = temporaryDirectory.resolve("round-trip.scene");

        assertTrue(SceneSerializer.save(scene, target.toString()));
        SceneSerializer.SceneData loaded = SceneSerializer.load(target.toString());

        assertEquals("1.0", loaded.version);
        assertEquals("Herói", loaded.name);
        assertEquals(1, loaded.entities.size());
        SceneSerializer.EntityData loadedEntity = loaded.entities.getFirst();
        assertAll(
            () -> assertEquals("Entity", loadedEntity.name),
            () -> assertEquals("jogável", loadedEntity.tag),
            () -> assertEquals(12.5f, loadedEntity.x),
            () -> assertEquals(-3.25f, loadedEntity.y),
            () -> assertEquals(45, loadedEntity.rotation),
            () -> assertEquals(2, loadedEntity.scaleX),
            () -> assertEquals(3, loadedEntity.scaleY),
            () -> assertFalse(loadedEntity.active)
        );
    }

    @Test
    @Disabled("Known SERIAL-ROUNDTRIP defect; name fields must retain their scopes")
    @Tag("specification")
    void sceneRoundTripMustPreserveSceneAndEntityNames() {
        Scene scene = new EmptyScene("Cena ação");
        Entity entity = new Entity("Herói");
        scene.addEntityImmediate(entity);
        Path target = temporaryDirectory.resolve("round-trip-names.scene");

        assertTrue(SceneSerializer.save(scene, target.toString()));
        SceneSerializer.SceneData loaded = SceneSerializer.load(target.toString());

        assertEquals("Cena ação", loaded.name);
        assertEquals("Herói", loaded.entities.getFirst().name);
    }

    @Test
    @Tag("characterization")
    void invalidHeaderIsIgnoredAndItsEntitiesAreAccepted() throws IOException {
        Path target = write(
            "invalid-header.scene",
            """
            NOT_A_PIXEL_ENGINE_SCENE
            version=1.0
            name=Invalid
            [entities]
            entity {
              x=1
            }
            """
        );

        SceneSerializer.SceneData loaded = SceneSerializer.load(target.toString());

        assertEquals("Invalid", loaded.name);
        assertEquals(1, loaded.entities.size());
        assertEquals(1, loaded.entities.getFirst().x);
    }

    @Test
    @Disabled("Known SERIAL-INVALID defect; invalid headers must not yield scene data")
    @Tag("specification")
    void invalidHeaderMustBeRejected() throws IOException {
        Path target = write(
            "invalid-header.scene",
            "NOT_A_PIXEL_ENGINE_SCENE\nversion=1.0\nname=Invalid\n"
        );

        SceneSerializer.SceneData loaded = SceneSerializer.load(target.toString());

        assertTrue(loaded.version.isEmpty());
        assertTrue(loaded.name.isEmpty());
        assertTrue(loaded.entities.isEmpty());
    }

    @Test
    @Tag("characterization")
    void invalidNumericValueEscapesAsNumberFormatException() throws IOException {
        Path target = write(
            "invalid-number.scene",
            """
            PIXEL_ENGINE_SCENE
            version=1.0
            name=Invalid
            [entities]
            entity {
              x=oops
            }
            """
        );

        assertThrows(
            NumberFormatException.class,
            () -> SceneSerializer.load(target.toString())
        );
    }

    private Path write(String fileName, String contents) throws IOException {
        Path target = temporaryDirectory.resolve(fileName);
        return Files.writeString(target, contents, StandardCharsets.UTF_8);
    }

    private static final class EmptyScene extends Scene {
        private EmptyScene(String name) {
            super(name);
        }

        @Override
        public void onCreate() {
            // The test populates the scene explicitly.
        }
    }
}
