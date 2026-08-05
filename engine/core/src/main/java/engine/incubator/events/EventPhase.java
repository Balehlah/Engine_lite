package engine.incubator.events;

/** Explicit world-event queues that callers drain at deterministic runtime boundaries. */
public enum EventPhase {
    BEFORE_FIXED_UPDATE,
    AFTER_FIXED_UPDATE,
    BEFORE_RENDER,
    AFTER_RENDER
}
