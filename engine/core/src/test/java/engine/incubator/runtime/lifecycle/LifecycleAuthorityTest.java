package engine.incubator.runtime.lifecycle;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("architecture")
final class LifecycleAuthorityTest {
    @Test
    void onlyGameRuntimeExposesLifecycleShutdownAuthority() throws NoSuchMethodException {
        Method contextClose = GameContext.class.getDeclaredMethod("close");
        Method registryClose = OwnedResourceRegistry.class.getDeclaredMethod("close");
        Method registerOwner = OwnedResourceRegistry.class.getDeclaredMethod(
            "registerOwner",
            Object.class,
            String.class
        );
        Method disposeOwner = OwnedResourceRegistry.class.getDeclaredMethod(
            "disposeOwner",
            Object.class
        );

        assertAll(
            () -> assertFalse(AutoCloseable.class.isAssignableFrom(GameContext.class)),
            () -> assertFalse(AutoCloseable.class.isAssignableFrom(OwnedResourceRegistry.class)),
            () -> assertFalse(Modifier.isPublic(contextClose.getModifiers())),
            () -> assertFalse(Modifier.isPublic(registryClose.getModifiers())),
            () -> assertFalse(Modifier.isPublic(registerOwner.getModifiers())),
            () -> assertFalse(Modifier.isPublic(disposeOwner.getModifiers()))
        );
    }
}
