package engine;

import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.image.BufferStrategy;

public abstract class Game extends Canvas implements Runnable {
    private Thread thread;
    private boolean running = false;

    public void start() {
        thread = new Thread(this);
        thread.start();
        running = true;
    }

    public void stop() {
        running = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public abstract void update();
    public abstract void render(Graphics g);

    @Override
    public void run() {
        createBufferStrategy(3);
        BufferStrategy bs = getBufferStrategy();

        long lastTime = System.nanoTime();
        double nsPerUpdate = 1e9 / 60.0;

        double delta = 0;

        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / nsPerUpdate;
            lastTime = now;

            while (delta >= 1) {
                update();
                delta--;
            }

            Graphics g = bs.getDrawGraphics();
            render(g);
            g.dispose();
            bs.show();
        }
    }
}
