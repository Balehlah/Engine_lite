package engine.incubator.runtime.lifecycle;

/**
 * Deterministic scene lifecycle owned by one {@link GameContext} execution.
 */
public interface RuntimeScene {
    void create(GameContext context);

    void enter(GameContext context);

    void fixedUpdate(GameContext context, double fixedDeltaSeconds);

    void render(GameContext context, double interpolationAlpha);

    void exit(GameContext context);

    void dispose(GameContext context);
}
