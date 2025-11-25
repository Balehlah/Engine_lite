package engine.graphics;

import engine.display.Window;
import engine.math.Vector2;
import engine.math.Rectangle;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Renderer principal da engine.
 * Abstrai operações de desenho e gerencia o pipeline de renderização.
 * Otimizado para pixel art com suporte a câmera e transformações.
 */
public class Renderer {
    
    // Janela e contexto
    private final Window window;
    private Graphics2D graphics;
    
    // Buffer interno (para resolução virtual)
    private BufferedImage frameBuffer;
    private Graphics2D bufferGraphics;
    private boolean useInternalBuffer;
    private int internalWidth;
    private int internalHeight;
    
    // Câmera
    private Camera camera;
    
    // Estado de renderização
    private Color clearColor;
    private Font currentFont;
    private float globalAlpha;
    
    // Flags
    private boolean pixelPerfect;
    
    public Renderer(Window window) {
        this.window = window;
        this.clearColor = ColorPalette.PICO8_BLACK;
        this.currentFont = new Font("Monospaced", Font.PLAIN, 12);
        this.globalAlpha = 1.0f;
        this.pixelPerfect = true;
        this.useInternalBuffer = false;
    }
    
    // ==================== RESOLUÇÃO VIRTUAL ====================
    
    /**
     * Define resolução interna (para upscaling pixel-perfect).
     * Ex: Jogo roda em 320x180 mas é escalado para 1280x720.
     */
    public void setInternalResolution(int width, int height) {
        this.internalWidth = width;
        this.internalHeight = height;
        this.frameBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        this.useInternalBuffer = true;
    }
    
    /**
     * Remove resolução interna, renderiza direto na janela.
     */
    public void clearInternalResolution() {
        this.frameBuffer = null;
        this.bufferGraphics = null;
        this.useInternalBuffer = false;
    }
    
    // ==================== LIFECYCLE ====================
    
    /**
     * Inicia frame de renderização.
     */
    public void begin() {
        if (useInternalBuffer) {
            bufferGraphics = frameBuffer.createGraphics();
            graphics = bufferGraphics;
        } else {
            graphics = window.getGraphics();
        }
        
        applyRenderingHints();
        clear();
    }
    
    /**
     * Finaliza frame e apresenta na tela.
     */
    public void end() {
        if (useInternalBuffer) {
            bufferGraphics.dispose();
            
            // Desenha buffer escalado na janela
            Graphics2D windowGraphics = window.getGraphics();
            applyRenderingHints(windowGraphics);
            
            // Escala para preencher janela mantendo aspect ratio
            int windowWidth = window.getWidth();
            int windowHeight = window.getHeight();
            
            float scaleX = (float) windowWidth / internalWidth;
            float scaleY = (float) windowHeight / internalHeight;
            float scale = Math.min(scaleX, scaleY);
            
            int scaledWidth = (int) (internalWidth * scale);
            int scaledHeight = (int) (internalHeight * scale);
            int offsetX = (windowWidth - scaledWidth) / 2;
            int offsetY = (windowHeight - scaledHeight) / 2;
            
            // Limpa bordas
            windowGraphics.setColor(Color.BLACK);
            windowGraphics.fillRect(0, 0, windowWidth, windowHeight);
            
            // Desenha buffer escalado
            windowGraphics.drawImage(frameBuffer, 
                offsetX, offsetY, offsetX + scaledWidth, offsetY + scaledHeight,
                0, 0, internalWidth, internalHeight,
                null);
            
            windowGraphics.dispose();
        } else {
            graphics.dispose();
        }
        
        window.show();
    }
    
    private void applyRenderingHints() {
        applyRenderingHints(graphics);
    }
    
    private void applyRenderingHints(Graphics2D g) {
        if (pixelPerfect) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        } else {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        }
    }
    
    // ==================== CLEAR ====================
    
    public void clear() {
        clear(clearColor);
    }
    
    public void clear(Color color) {
        graphics.setColor(color);
        graphics.fillRect(0, 0, getWidth(), getHeight());
    }
    
    public void setClearColor(Color color) {
        this.clearColor = color;
    }
    
    // ==================== PRIMITIVAS ====================
    
    public void drawRect(int x, int y, int width, int height, Color color) {
        Vector2 pos = transformPosition(x, y);
        graphics.setColor(color);
        graphics.drawRect(pos.intX(), pos.intY(), width - 1, height - 1);
    }
    
    public void drawRect(Rectangle rect, Color color) {
        drawRect(rect.intX(), rect.intY(), rect.intWidth(), rect.intHeight(), color);
    }
    
    public void fillRect(int x, int y, int width, int height, Color color) {
        Vector2 pos = transformPosition(x, y);
        graphics.setColor(color);
        graphics.fillRect(pos.intX(), pos.intY(), width, height);
    }
    
    public void fillRect(Rectangle rect, Color color) {
        fillRect(rect.intX(), rect.intY(), rect.intWidth(), rect.intHeight(), color);
    }
    
    public void drawCircle(int centerX, int centerY, int radius, Color color) {
        Vector2 pos = transformPosition(centerX, centerY);
        graphics.setColor(color);
        graphics.drawOval(pos.intX() - radius, pos.intY() - radius, radius * 2, radius * 2);
    }
    
    public void fillCircle(int centerX, int centerY, int radius, Color color) {
        Vector2 pos = transformPosition(centerX, centerY);
        graphics.setColor(color);
        graphics.fillOval(pos.intX() - radius, pos.intY() - radius, radius * 2, radius * 2);
    }
    
    public void drawLine(int x1, int y1, int x2, int y2, Color color) {
        Vector2 pos1 = transformPosition(x1, y1);
        Vector2 pos2 = transformPosition(x2, y2);
        graphics.setColor(color);
        graphics.drawLine(pos1.intX(), pos1.intY(), pos2.intX(), pos2.intY());
    }
    
    public void drawLine(Vector2 start, Vector2 end, Color color) {
        drawLine(start.intX(), start.intY(), end.intX(), end.intY(), color);
    }
    
    public void drawPixel(int x, int y, Color color) {
        Vector2 pos = transformPosition(x, y);
        graphics.setColor(color);
        graphics.fillRect(pos.intX(), pos.intY(), 1, 1);
    }
    
    // ==================== SPRITES ====================
    
    public void drawSprite(Sprite sprite, float x, float y) {
        Vector2 pos = transformPosition(x, y);
        sprite.draw(graphics, pos.x, pos.y);
    }
    
    public void drawSprite(Sprite sprite, Vector2 position) {
        drawSprite(sprite, position.x, position.y);
    }
    
    public void drawImage(BufferedImage image, int x, int y) {
        Vector2 pos = transformPosition(x, y);
        graphics.drawImage(image, pos.intX(), pos.intY(), null);
    }
    
    public void drawImage(BufferedImage image, int x, int y, int width, int height) {
        Vector2 pos = transformPosition(x, y);
        graphics.drawImage(image, pos.intX(), pos.intY(), width, height, null);
    }
    
    public void drawImage(BufferedImage image, Rectangle dest) {
        drawImage(image, dest.intX(), dest.intY(), dest.intWidth(), dest.intHeight());
    }
    
    /**
     * Desenha região de uma imagem (spritesheet).
     */
    public void drawImage(BufferedImage image, int destX, int destY, int destWidth, int destHeight,
                          int srcX, int srcY, int srcWidth, int srcHeight) {
        Vector2 pos = transformPosition(destX, destY);
        graphics.drawImage(image,
            pos.intX(), pos.intY(), pos.intX() + destWidth, pos.intY() + destHeight,
            srcX, srcY, srcX + srcWidth, srcY + srcHeight,
            null);
    }
    
    // ==================== TEXTO ====================
    
    public void drawText(String text, int x, int y, Color color) {
        Vector2 pos = transformPosition(x, y);
        graphics.setFont(currentFont);
        graphics.setColor(color);
        graphics.drawString(text, pos.intX(), pos.intY());
    }
    
    public void drawText(String text, int x, int y, Color color, Font font) {
        graphics.setFont(font);
        drawText(text, x, y, color);
        graphics.setFont(currentFont);
    }
    
    public void drawTextCentered(String text, int x, int y, Color color) {
        Vector2 pos = transformPosition(x, y);
        var metrics = graphics.getFontMetrics();
        int textWidth = metrics.stringWidth(text);
        int textHeight = metrics.getHeight();
        graphics.setColor(color);
        graphics.drawString(text, pos.intX() - textWidth / 2, pos.intY() + textHeight / 4);
    }
    
    public void setFont(Font font) {
        this.currentFont = font;
        if (graphics != null) {
            graphics.setFont(font);
        }
    }
    
    // ==================== ANIMAÇÃO ====================
    
    public void drawAnimation(Animation animation, float x, float y) {
        drawSprite(animation.getCurrentSprite(), x, y);
    }
    
    public void drawAnimation(Animation animation, Vector2 position) {
        drawAnimation(animation, position.x, position.y);
    }
    
    // ==================== CÂMERA ====================
    
    public void setCamera(Camera camera) {
        this.camera = camera;
    }
    
    public Camera getCamera() {
        return camera;
    }
    
    public void clearCamera() {
        this.camera = null;
    }
    
    /**
     * Transforma posição de mundo para tela considerando câmera.
     */
    private Vector2 transformPosition(float x, float y) {
        if (camera == null) {
            return new Vector2(x, y);
        }
        return camera.worldToScreen(new Vector2(x, y));
    }
    
    // ==================== ESTADO ====================
    
    public void setAlpha(float alpha) {
        this.globalAlpha = Math.max(0, Math.min(1, alpha));
        if (graphics != null) {
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, globalAlpha));
        }
    }
    
    public void resetAlpha() {
        setAlpha(1.0f);
    }
    
    public void setPixelPerfect(boolean pixelPerfect) {
        this.pixelPerfect = pixelPerfect;
    }
    
    public boolean isPixelPerfect() {
        return pixelPerfect;
    }
    
    // ==================== GETTERS ====================
    
    public int getWidth() {
        return useInternalBuffer ? internalWidth : window.getWidth();
    }
    
    public int getHeight() {
        return useInternalBuffer ? internalHeight : window.getHeight();
    }
    
    public Window getWindow() {
        return window;
    }
    
    /**
     * Acesso direto ao Graphics2D para operações avançadas.
     */
    public Graphics2D getGraphics() {
        return graphics;
    }
    
    /**
     * Retorna o buffer interno (se estiver usando resolução virtual).
     */
    public BufferedImage getFrameBuffer() {
        return frameBuffer;
    }
}
