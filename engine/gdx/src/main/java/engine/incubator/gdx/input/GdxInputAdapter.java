package engine.incubator.gdx.input;

import com.badlogic.gdx.InputAdapter;
import engine.incubator.runtime.input.InputEvent;
import engine.incubator.runtime.input.InputEventQueue;
import engine.incubator.runtime.input.InputSnapshot;
import engine.incubator.runtime.input.ScreenToVirtual;
import engine.incubator.runtime.input.TickInput;
import java.util.Objects;
import java.util.function.Supplier;

/** Translates libGDX callbacks into backend-neutral events consumed per tick. */
public final class GdxInputAdapter extends InputAdapter {
    private final TickInput input;
    private final Supplier<ScreenToVirtual> mappingSupplier;

    public GdxInputAdapter(
        TickInput input,
        Supplier<ScreenToVirtual> mappingSupplier
    ) {
        this.input = Objects.requireNonNull(input, "input");
        this.mappingSupplier = Objects.requireNonNull(mappingSupplier, "mappingSupplier");
    }

    @Override
    public boolean keyDown(int keycode) {
        input.enqueue(new InputEvent.KeyChanged(keycode, true));
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        input.enqueue(new InputEvent.KeyChanged(keycode, false));
        return true;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        movePointer(screenX, screenY);
        if (button >= 0) {
            input.enqueue(new InputEvent.MouseButtonChanged(button, true));
        }
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        movePointer(screenX, screenY);
        if (button >= 0) {
            input.enqueue(new InputEvent.MouseButtonChanged(button, false));
        }
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        movePointer(screenX, screenY);
        return true;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        movePointer(screenX, screenY);
        return true;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        input.enqueue(new InputEvent.Scrolled(amountX, amountY));
        return true;
    }

    public void focusLost() {
        input.enqueue(new InputEvent.FocusChanged(false));
    }

    public void focusGained() {
        input.enqueue(new InputEvent.FocusChanged(true));
    }

    public InputSnapshot nextSnapshot() {
        return input.nextSnapshot(
            Objects.requireNonNull(mappingSupplier.get(), "mappingSupplier result")
        );
    }

    public InputEventQueue.Metrics queueMetrics() {
        return input.queueMetrics();
    }

    public long nextTickIndex() {
        return input.nextTickIndex();
    }

    private void movePointer(int screenX, int screenY) {
        input.enqueue(new InputEvent.PointerMoved(screenX, screenY));
    }
}
