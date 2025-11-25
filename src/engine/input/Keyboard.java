package engine.input;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * Gerenciador de input de teclado.
 * Rastreia estados de teclas: pressionada, solta, just pressed, just released.
 */
public class Keyboard implements KeyListener {
    
    // Estados das teclas
    private final boolean[] keys;
    private final boolean[] keysLast;
    
    // Constantes para teclas comuns
    public static final int KEY_UP = KeyEvent.VK_UP;
    public static final int KEY_DOWN = KeyEvent.VK_DOWN;
    public static final int KEY_LEFT = KeyEvent.VK_LEFT;
    public static final int KEY_RIGHT = KeyEvent.VK_RIGHT;
    
    public static final int KEY_W = KeyEvent.VK_W;
    public static final int KEY_A = KeyEvent.VK_A;
    public static final int KEY_S = KeyEvent.VK_S;
    public static final int KEY_D = KeyEvent.VK_D;
    
    public static final int KEY_SPACE = KeyEvent.VK_SPACE;
    public static final int KEY_ENTER = KeyEvent.VK_ENTER;
    public static final int KEY_ESCAPE = KeyEvent.VK_ESCAPE;
    public static final int KEY_SHIFT = KeyEvent.VK_SHIFT;
    public static final int KEY_CTRL = KeyEvent.VK_CONTROL;
    public static final int KEY_ALT = KeyEvent.VK_ALT;
    public static final int KEY_TAB = KeyEvent.VK_TAB;
    
    public static final int KEY_1 = KeyEvent.VK_1;
    public static final int KEY_2 = KeyEvent.VK_2;
    public static final int KEY_3 = KeyEvent.VK_3;
    public static final int KEY_4 = KeyEvent.VK_4;
    public static final int KEY_5 = KeyEvent.VK_5;
    
    public static final int KEY_F1 = KeyEvent.VK_F1;
    public static final int KEY_F2 = KeyEvent.VK_F2;
    public static final int KEY_F3 = KeyEvent.VK_F3;
    
    private static final int NUM_KEYS = 1024;
    
    public Keyboard() {
        keys = new boolean[NUM_KEYS];
        keysLast = new boolean[NUM_KEYS];
    }
    
    // ==================== UPDATE ====================
    
    /**
     * Atualiza estado anterior. Chamar no início de cada frame.
     */
    public void update() {
        System.arraycopy(keys, 0, keysLast, 0, NUM_KEYS);
    }
    
    // ==================== CONSULTAS ====================
    
    /**
     * Retorna true enquanto a tecla estiver pressionada.
     */
    public boolean isKeyDown(int keyCode) {
        if (keyCode < 0 || keyCode >= NUM_KEYS) return false;
        return keys[keyCode];
    }
    
    /**
     * Retorna true enquanto a tecla NÃO estiver pressionada.
     */
    public boolean isKeyUp(int keyCode) {
        return !isKeyDown(keyCode);
    }
    
    /**
     * Retorna true apenas no frame em que a tecla foi pressionada.
     */
    public boolean isKeyPressed(int keyCode) {
        if (keyCode < 0 || keyCode >= NUM_KEYS) return false;
        return keys[keyCode] && !keysLast[keyCode];
    }
    
    /**
     * Retorna true apenas no frame em que a tecla foi solta.
     */
    public boolean isKeyReleased(int keyCode) {
        if (keyCode < 0 || keyCode >= NUM_KEYS) return false;
        return !keys[keyCode] && keysLast[keyCode];
    }
    
    /**
     * Verifica se alguma tecla está pressionada.
     */
    public boolean isAnyKeyDown() {
        for (int i = 0; i < NUM_KEYS; i++) {
            if (keys[i]) return true;
        }
        return false;
    }
    
    /**
     * Limpa todos os estados de teclas.
     */
    public void clear() {
        for (int i = 0; i < NUM_KEYS; i++) {
            keys[i] = false;
            keysLast[i] = false;
        }
    }
    
    // ==================== HELPERS ====================
    
    /**
     * Retorna direção horizontal baseada em WASD ou setas.
     * -1 = esquerda, 0 = neutro, 1 = direita
     */
    public int getHorizontalAxis() {
        int axis = 0;
        if (isKeyDown(KEY_LEFT) || isKeyDown(KEY_A)) axis--;
        if (isKeyDown(KEY_RIGHT) || isKeyDown(KEY_D)) axis++;
        return axis;
    }
    
    /**
     * Retorna direção vertical baseada em WASD ou setas.
     * -1 = cima, 0 = neutro, 1 = baixo
     */
    public int getVerticalAxis() {
        int axis = 0;
        if (isKeyDown(KEY_UP) || isKeyDown(KEY_W)) axis--;
        if (isKeyDown(KEY_DOWN) || isKeyDown(KEY_S)) axis++;
        return axis;
    }
    
    // ==================== EVENTOS ====================
    
    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code >= 0 && code < NUM_KEYS) {
            keys[code] = true;
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code >= 0 && code < NUM_KEYS) {
            keys[code] = false;
        }
    }
    
    @Override
    public void keyTyped(KeyEvent e) {
        // Não usado - preferimos keyPressed/keyReleased para games
    }
}

