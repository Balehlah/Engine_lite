package engine.display;

import engine.util.Logger;

import javax.swing.JFrame;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferStrategy;

/**
 * Janela do jogo baseada em AWT/Swing.
 * Usa Canvas + BufferStrategy para rendering eficiente.
 */
public class Window {
    
    private final JFrame frame;
    private final Canvas canvas;
    
    private final int width;
    private final int height;
    
    private BufferStrategy bufferStrategy;
    
    public Window(String title, int width, int height) {
        this.width = width;
        this.height = height;
        
        // Cria canvas para rendering
        canvas = new Canvas();
        Dimension size = new Dimension(width, height);
        canvas.setPreferredSize(size);
        canvas.setMinimumSize(size);
        canvas.setMaximumSize(size);
        canvas.setFocusable(true);
        
        // Cria frame
        frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(canvas);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        
        // Foca no canvas para input
        canvas.requestFocus();
        
        // Cria buffer strategy (triple buffering)
        createBufferStrategy();
        
        Logger.debug("Window criada: %s (%dx%d)", title, width, height);
    }
    
    /**
     * Cria BufferStrategy com triple buffering.
     */
    private void createBufferStrategy() {
        // Tenta criar até 10 vezes (pode falhar se janela não está pronta)
        for (int i = 0; i < 10; i++) {
            try {
                canvas.createBufferStrategy(3);
                bufferStrategy = canvas.getBufferStrategy();
                if (bufferStrategy != null) {
                    return;
                }
            } catch (IllegalStateException e) {
                // Janela ainda não está pronta
            }
            
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                break;
            }
        }
        
        Logger.error("Falha ao criar BufferStrategy");
    }
    
    // ==================== RENDERING ====================
    
    /**
     * Obtém Graphics2D do buffer atual.
     */
    public Graphics2D getGraphics() {
        if (bufferStrategy == null) {
            createBufferStrategy();
        }
        return (Graphics2D) bufferStrategy.getDrawGraphics();
    }
    
    /**
     * Mostra o buffer atual na tela.
     */
    public void show() {
        if (bufferStrategy != null && !bufferStrategy.contentsLost()) {
            bufferStrategy.show();
        }
    }
    
    /**
     * Retorna o BufferStrategy para uso direto.
     */
    public BufferStrategy getBufferStrategy() {
        if (bufferStrategy == null) {
            createBufferStrategy();
        }
        return bufferStrategy;
    }
    
    // ==================== PROPRIEDADES ====================
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public Canvas getCanvas() {
        return canvas;
    }
    
    public JFrame getFrame() {
        return frame;
    }
    
    public void setTitle(String title) {
        frame.setTitle(title);
    }
    
    public String getTitle() {
        return frame.getTitle();
    }
    
    public boolean isFocused() {
        return canvas.hasFocus();
    }
    
    // ==================== LIFECYCLE ====================
    
    /**
     * Fecha a janela e libera recursos.
     */
    public void dispose() {
        if (bufferStrategy != null) {
            bufferStrategy.dispose();
        }
        frame.dispose();
        Logger.debug("Window disposed");
    }
}
