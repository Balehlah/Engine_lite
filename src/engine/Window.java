package engine;

import javax.swing,JFrame;

public class Window {
    public Window(int width, int height, String title, game game) {
        JFrame frame = new JFrame(title);
        frame.seDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setSize(width, height);
        frame.setLocationRelativeTo(null);
        frame.add(game);
        frame.setVisible(true);

        game.start();
    }
}W