package engine.input;

import engine.math.Vector2;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * Gerenciador de input de mouse.
 * Rastreia posição, botões e scroll.
 */
public class Mouse implements MouseListener, MouseMotionListener, MouseWheelListener {
    
    // Posição do mouse
    private int x;
    private int y;
    private int lastX;
    private int lastY;
    
    // Botões (até 5 botões)
    private static final int NUM_BUTTONS = 5;
    private final boolean[] buttons;
    private final boolean[] buttonsLast;
    
    // Scroll
    private int scrollDelta;
    private int scrollAccumulator;
    
    // Constantes
    public static final int BUTTON_LEFT = MouseEvent.BUTTON1;
    public static final int BUTTON_MIDDLE = MouseEvent.BUTTON2;
    public static final int BUTTON_RIGHT = MouseEvent.BUTTON3;
    
    // Estado
    private boolean insideWindow = true;
    
    public Mouse() {
        buttons = new boolean[NUM_BUTTONS];
        buttonsLast = new boolean[NUM_BUTTONS];
    }
    
    // ==================== UPDATE ====================
    
    /**
     * Atualiza estados. Chamar no início de cada frame.
     */
    public void update() {
        System.arraycopy(buttons, 0, buttonsLast, 0, NUM_BUTTONS);
        lastX = x;
        lastY = y;
        
        // Scroll é consumido por frame
        scrollDelta = scrollAccumulator;
        scrollAccumulator = 0;
    }
    
    // ==================== POSIÇÃO ====================
    
    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }
    
    public Vector2 getPosition() {
        return new Vector2(x, y);
    }
    
    /**
     * Delta de movimento desde o último frame.
     */
    public int getDeltaX() {
        return x - lastX;
    }
    
    public int getDeltaY() {
        return y - lastY;
    }
    
    public Vector2 getDelta() {
        return new Vector2(getDeltaX(), getDeltaY());
    }
    
    /**
     * Verifica se o mouse está dentro da janela.
     */
    public boolean isInsideWindow() {
        return insideWindow;
    }
    
    // ==================== BOTÕES ====================
    
    /**
     * Retorna true enquanto o botão estiver pressionado.
     */
    public boolean isButtonDown(int button) {
        if (button < 0 || button >= NUM_BUTTONS) return false;
        return buttons[button];
    }
    
    /**
     * Retorna true quando o botão NÃO está pressionado.
     */
    public boolean isButtonUp(int button) {
        return !isButtonDown(button);
    }
    
    /**
     * Retorna true apenas no frame em que o botão foi pressionado.
     */
    public boolean isButtonPressed(int button) {
        if (button < 0 || button >= NUM_BUTTONS) return false;
        return buttons[button] && !buttonsLast[button];
    }
    
    /**
     * Retorna true apenas no frame em que o botão foi solto.
     */
    public boolean isButtonReleased(int button) {
        if (button < 0 || button >= NUM_BUTTONS) return false;
        return !buttons[button] && buttonsLast[button];
    }
    
    // Atalhos convenientes
    public boolean isLeftDown() {
        return isButtonDown(BUTTON_LEFT);
    }
    
    public boolean isRightDown() {
        return isButtonDown(BUTTON_RIGHT);
    }
    
    public boolean isMiddleDown() {
        return isButtonDown(BUTTON_MIDDLE);
    }
    
    public boolean isLeftPressed() {
        return isButtonPressed(BUTTON_LEFT);
    }
    
    public boolean isRightPressed() {
        return isButtonPressed(BUTTON_RIGHT);
    }
    
    public boolean isLeftReleased() {
        return isButtonReleased(BUTTON_LEFT);
    }
    
    public boolean isRightReleased() {
        return isButtonReleased(BUTTON_RIGHT);
    }
    
    // ==================== SCROLL ====================
    
    /**
     * Retorna delta do scroll desde o último frame.
     * Positivo = scroll up, Negativo = scroll down
     */
    public int getScrollDelta() {
        return scrollDelta;
    }
    
    public boolean isScrollingUp() {
        return scrollDelta > 0;
    }
    
    public boolean isScrollingDown() {
        return scrollDelta < 0;
    }
    
    // ==================== LIMPAR ====================
    
    public void clear() {
        for (int i = 0; i < NUM_BUTTONS; i++) {
            buttons[i] = false;
            buttonsLast[i] = false;
        }
        scrollDelta = 0;
        scrollAccumulator = 0;
    }
    
    // ==================== EVENTOS DE MOUSE ====================
    
    @Override
    public void mousePressed(MouseEvent e) {
        int button = e.getButton();
        if (button >= 0 && button < NUM_BUTTONS) {
            buttons[button] = true;
        }
    }
    
    @Override
    public void mouseReleased(MouseEvent e) {
        int button = e.getButton();
        if (button >= 0 && button < NUM_BUTTONS) {
            buttons[button] = false;
        }
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
        // Não usado - preferimos pressed/released
    }
    
    @Override
    public void mouseEntered(MouseEvent e) {
        insideWindow = true;
    }
    
    @Override
    public void mouseExited(MouseEvent e) {
        insideWindow = false;
    }
    
    // ==================== EVENTOS DE MOVIMENTO ====================
    
    @Override
    public void mouseMoved(MouseEvent e) {
        x = e.getX();
        y = e.getY();
    }
    
    @Override
    public void mouseDragged(MouseEvent e) {
        x = e.getX();
        y = e.getY();
    }
    
    // ==================== EVENTOS DE SCROLL ====================
    
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        scrollAccumulator -= e.getWheelRotation();
    }
}

