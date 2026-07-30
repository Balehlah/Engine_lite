package engine.incubator.gdx.spike;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.badlogic.gdx.utils.Disposable;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("specification")
final class DisposableRegistryTest {
    @Test
    void disposesEveryOwnedResourceOnceInReverseOrder() {
        var registry = new DisposableRegistry();
        var calls = new ArrayList<String>();
        registry.own("first", new RecordingDisposable("first", calls));
        registry.own("second", new RecordingDisposable("second", calls));

        registry.disposeAll(message -> calls.add("log:" + message));

        assertEquals(
            List.of("second", "log:second=1", "first", "log:first=1"),
            calls
        );
        assertEquals(1, registry.disposalCounts().get("first"));
        assertEquals(1, registry.disposalCounts().get("second"));
        registry.assertDisposedExactlyOnce();
    }

    @Test
    void rejectsDuplicateIdentityAndDuplicateName() {
        var registry = new DisposableRegistry();
        var resource = new RecordingDisposable("one", new ArrayList<>());
        registry.own("one", resource);

        assertThrows(
            IllegalArgumentException.class,
            () -> registry.own("other", resource)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> registry.own(
                "one",
                new RecordingDisposable("another", new ArrayList<>())
            )
        );
    }

    @Test
    void rejectsASecondDisposalPass() {
        var registry = new DisposableRegistry();
        registry.own("resource", new RecordingDisposable("resource", new ArrayList<>()));
        registry.disposeAll(ignored -> {
        });

        assertThrows(
            IllegalStateException.class,
            () -> registry.disposeAll(ignored -> {
            })
        );
    }

    private static final class RecordingDisposable implements Disposable {
        private final String name;
        private final List<String> calls;

        private RecordingDisposable(String name, List<String> calls) {
            this.name = name;
            this.calls = calls;
        }

        @Override
        public void dispose() {
            calls.add(name);
        }
    }
}
