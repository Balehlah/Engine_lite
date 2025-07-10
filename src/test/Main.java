import engine.Game;
import engine.Input;
import engine.Window;
import java.awt.Color;
import java.awt.Graphics;

public class Main extends Game {
    private int x = 100;
    private int y = 100;
    private Input input;

    public Main() {
        input = new Input();
        addKeyListener(input);
        setFocusable(true);
        requestFocus();
        new Window(800, 600, "Project_Core", this);
    }

    public void update() {
        if (input.isKeyPressed(87)) y--; // W
        if (input.isKeyPressed(83)) y++; // S
        if (input.isKeyPressed(65)) x--; // A
        if (input.isKeyPressed(68)) x++; // D
    }

    public void render(Graphics g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(Color.GREEN);
        g.fillRect(x, y, 32, 32);
    }

    public static void main(String[] args) {
        new Main();
    }
}
