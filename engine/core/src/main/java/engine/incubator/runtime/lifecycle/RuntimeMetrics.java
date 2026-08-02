package engine.incubator.runtime.lifecycle;

/**
 * Immutable counters across all executions of one {@link GameRuntime} instance.
 */
public record RuntimeMetrics(
    long executionsStarted,
    long restarts,
    long sceneTransitions,
    long failedExecutions,
    GameContextSnapshot lastClosedContext
) {
}
