package engine.input;

import engine.display.Window;
import engine.math.Vector2;

import java.awt.Canvas;

/**
 * Facade unificada para todo input do jogo.
 * Singleton que agrega Keyboard e Mouse.
 */
public final class Input {
    
    private static Input instance;
    
    private final Keyboard keyboard;
    private final Mouse mouse;
    
    private Input() {
        keyboard = new Keyboard();
        mouse = new Mouse();
    }
    
    public static Input getInstance() {
        if (instance == null) {
            instance = new Input();
        }
        return instance;
    }
    
    // ==================== REGISTRO ====================
    
    /**
     * Registra listeners na janela.
     */
    public void register(Window window) {
        Canvas canvas = window.getCanvas();
        
        // Keyboard
        canvas.addKeyListener(keyboard);
        
        // Mouse
        canvas.addMouseListener(mouse);
        canvas.addMouseMotionListener(mouse);
        canvas.addMouseWheelListener(mouse);
    }
    
    /**
     * Remove listeners da janela.
     */
    public void unregister(Window window) {
        Canvas canvas = window.getCanvas();
        
        canvas.removeKeyListener(keyboard);
        canvas.removeMouseListener(mouse);
        canvas.removeMouseMotionListener(mouse);
        canvas.removeMouseWheelListener(mouse);
    }
    
    // ==================== UPDATE ====================
    
    /**
     * Atualiza estados de input. Chamar no início de cada frame.
     */
    public void update() {
        keyboard.update();
        mouse.update();
    }
    
    // ==================== KEYBOARD ====================
    
    public boolean isKeyDown(int keyCode) {
        return keyboard.isKeyDown(keyCode);
    }
    
    public boolean isKeyUp(int keyCode) {
        return keyboard.isKeyUp(keyCode);
    }
    
    public boolean isKeyPressed(int keyCode) {
        return keyboard.isKeyPressed(keyCode);
    }
    
    public boolean isKeyReleased(int keyCode) {
        return keyboard.isKeyReleased(keyCode);
    }
    
    public boolean isAnyKeyDown() {
        return keyboard.isAnyKeyDown();
    }
    
    public int getHorizontalAxis() {
        return keyboard.getHorizontalAxis();
    }
    
    public int getVerticalAxis() {
        return keyboard.getVerticalAxis();
    }
    
    // ==================== MOUSE ====================
    
    public int getMouseX() {
        return mouse.getX();
    }
    
    public int getMouseY() {
        return mouse.getY();
    }
    
    public Vector2 getMousePosition() {
        return mouse.getPosition();
    }
    
    public Vector2 getMouseDelta() {
        return mouse.getDelta();
    }
    
    public boolean isMouseButtonDown(int button) {
        return mouse.isButtonDown(button);
    }
    
    public boolean isMouseButtonPressed(int button) {
        return mouse.isButtonPressed(button);
    }
    
    public boolean isMouseButtonReleased(int button) {
        return mouse.isButtonReleased(button);
    }
    
    public boolean isLeftMouseDown() {
        return mouse.isLeftDown();
    }
    
    public boolean isRightMouseDown() {
        return mouse.isRightDown();
    }
    
    public boolean isLeftMousePressed() {
        return mouse.isLeftPressed();
    }
    
    public boolean isRightMousePressed() {
        return mouse.isRightPressed();
    }
    
    public int getScrollDelta() {
        return mouse.getScrollDelta();
    }
    
    public boolean isMouseInsideWindow() {
        return mouse.isInsideWindow();
    }
    
    // ==================== ACESSO DIRETO ====================
    
    public Keyboard getKeyboard() {
        return keyboard;
    }
    
    public Mouse getMouse() {
        return mouse;
    }
    
    // ==================== UTILITÁRIOS ====================
    
    /**
     * Limpa todos os estados de input.
     */
    public void clear() {
        keyboard.clear();
        mouse.clear();
    }
}
