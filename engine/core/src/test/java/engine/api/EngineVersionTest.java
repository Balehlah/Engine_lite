package engine.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EngineVersionTest {
    @Test
    void reportsDevelopmentWhenRunningFromClasses() {
        assertEquals("development", EngineVersion.current());
    }
}
