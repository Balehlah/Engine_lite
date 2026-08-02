package engine.incubator.runtime.lifecycle;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("architecture")
final class NoMutableSingletonTest {
    @Test
    void newRuntimeDeclaresNoMutableStaticState() {
        List<Class<?>> runtimeTypes = List.of(
            AssetStore.class,
            GameContext.class,
            GameContextSnapshot.class,
            GameRuntime.class,
            LifecycleException.class,
            OwnedResourceRegistry.class,
            ResourceDisposer.class,
            ResourceMetrics.class,
            RuntimeEventQueue.class,
            RuntimeMetrics.class,
            RuntimeScene.class,
            WorldState.class
        );

        runtimeTypes.forEach(type -> assertAll(
            Arrays.stream(type.getDeclaredFields())
                .filter(field -> Modifier.isStatic(field.getModifiers()))
                .map(field -> () -> assertFalse(
                    isMutableStatic(field),
                    () -> type.getName() + " declares mutable static field " + field.getName()
                ))
        ));
    }

    private static boolean isMutableStatic(Field field) {
        return !Modifier.isFinal(field.getModifiers());
    }
}
