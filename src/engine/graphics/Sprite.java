package engine.graphics;

import engine.math.Vector2;
import engine.math.Rectangle;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Sprite para renderização de imagens.
 * Suporta origem, escala, rotação e recorte de spritesheet.
 */
public class Sprite {
    
    // Imagem fonte
    private final BufferedImage image;
    
    // Região do spritesheet (null = imagem inteira)
    private Rectangle sourceRect;
    
    // Dimensões
    private final int width;
    private final int height;
    
    // Transform
    private Vector2 origin;  // Ponto de pivot (0-1)
    private Vector2 scale;
    private float rotation;
    private boolean flipX;
    private boolean flipY;
    
    // Visibilidade
    private float alpha = 1.0f;
    
    /**
     * Cria sprite de uma imagem inteira.
     */
    public Sprite(BufferedImage image) {
        this.image = image;
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.sourceRect = null;
        
        this.origin = new Vector2(0.5f, 0.5f); // Centro por padrão
        this.scale = Vector2.ONE;
        this.rotation = 0;
    }
    
    /**
     * Cria sprite de uma região de spritesheet.
     */
    public Sprite(BufferedImage spritesheet, int x, int y, int width, int height) {
        this.image = spritesheet;
        this.width = width;
        this.height = height;
        this.sourceRect = new Rectangle(x, y, width, height);
        
        this.origin = new Vector2(0.5f, 0.5f);
        this.scale = Vector2.ONE;
        this.rotation = 0;
    }
    
    /**
     * Cria sprite de uma região usando Rectangle.
     */
    public Sprite(BufferedImage spritesheet, Rectangle region) {
        this(spritesheet, region.intX(), region.intY(), region.intWidth(), region.intHeight());
    }
    
    // ==================== RENDERING ====================
    
    /**
     * Desenha o sprite em uma posição.
     */
    public void draw(Graphics2D g, float x, float y) {
        if (alpha <= 0) return;
        
        // Calcula dimensões finais
        int drawWidth = (int) (width * scale.x);
        int drawHeight = (int) (height * scale.y);
        
        // Calcula posição considerando origem
        int drawX = (int) (x - drawWidth * origin.x);
        int drawY = (int) (y - drawHeight * origin.y);
        
        // Salva estado
        var originalComposite = g.getComposite();
        var originalTransform = g.getTransform();
        
        // Aplica alpha
        if (alpha < 1.0f) {
            g.setComposite(java.awt.AlphaComposite.getInstance(
                java.awt.AlphaComposite.SRC_OVER, alpha));
        }
        
        // Aplica rotação se necessário
        if (rotation != 0) {
            double centerX = x;
            double centerY = y;
            g.rotate(rotation, centerX, centerY);
        }
        
        // Fonte (região ou imagem inteira)
        int sx, sy, sw, sh;
        if (sourceRect != null) {
            sx = sourceRect.intX();
            sy = sourceRect.intY();
            sw = sourceRect.intWidth();
            sh = sourceRect.intHeight();
        } else {
            sx = 0;
            sy = 0;
            sw = image.getWidth();
            sh = image.getHeight();
        }
        
        // Calcula flip
        int dx1 = drawX;
        int dy1 = drawY;
        int dx2 = drawX + drawWidth;
        int dy2 = drawY + drawHeight;
        
        if (flipX) {
            int temp = dx1;
            dx1 = dx2;
            dx2 = temp;
        }
        
        if (flipY) {
            int temp = dy1;
            dy1 = dy2;
            dy2 = temp;
        }
        
        // Desenha
        g.drawImage(image,
            dx1, dy1, dx2, dy2,
            sx, sy, sx + sw, sy + sh,
            null);
        
        // Restaura estado
        g.setTransform(originalTransform);
        g.setComposite(originalComposite);
    }
    
    /**
     * Desenha o sprite em uma posição com Vector2.
     */
    public void draw(Graphics2D g, Vector2 position) {
        draw(g, position.x, position.y);
    }
    
    /**
     * Desenha o sprite em um retângulo de destino.
     */
    public void draw(Graphics2D g, Rectangle dest) {
        if (alpha <= 0) return;
        
        var originalComposite = g.getComposite();
        
        if (alpha < 1.0f) {
            g.setComposite(java.awt.AlphaComposite.getInstance(
                java.awt.AlphaComposite.SRC_OVER, alpha));
        }
        
        int sx, sy, sw, sh;
        if (sourceRect != null) {
            sx = sourceRect.intX();
            sy = sourceRect.intY();
            sw = sourceRect.intWidth();
            sh = sourceRect.intHeight();
        } else {
            sx = 0;
            sy = 0;
            sw = image.getWidth();
            sh = image.getHeight();
        }
        
        g.drawImage(image,
            dest.intX(), dest.intY(),
            dest.intX() + dest.intWidth(), dest.intY() + dest.intHeight(),
            sx, sy, sx + sw, sy + sh,
            null);
        
        g.setComposite(originalComposite);
    }
    
    // ==================== SPRITESHEET ====================
    
    /**
     * Extrai array de sprites de um spritesheet em grid.
     */
    public static Sprite[] fromSpritesheet(BufferedImage sheet, int tileWidth, int tileHeight) {
        int cols = sheet.getWidth() / tileWidth;
        int rows = sheet.getHeight() / tileHeight;
        Sprite[] sprites = new Sprite[cols * rows];
        
        int index = 0;
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                sprites[index++] = new Sprite(sheet, x * tileWidth, y * tileHeight, tileWidth, tileHeight);
            }
        }
        
        return sprites;
    }
    
    /**
     * Extrai sprite específico de um spritesheet em grid.
     */
    public static Sprite fromSpritesheet(BufferedImage sheet, int tileWidth, int tileHeight, int col, int row) {
        return new Sprite(sheet, col * tileWidth, row * tileHeight, tileWidth, tileHeight);
    }
    
    // ==================== GETTERS/SETTERS ====================
    
    public int getWidth() {
        return (int) (width * scale.x);
    }
    
    public int getHeight() {
        return (int) (height * scale.y);
    }
    
    public int getOriginalWidth() {
        return width;
    }
    
    public int getOriginalHeight() {
        return height;
    }
    
    public BufferedImage getImage() {
        return image;
    }
    
    public Rectangle getSourceRect() {
        return sourceRect;
    }
    
    public void setSourceRect(Rectangle rect) {
        this.sourceRect = rect;
    }
    
    public Vector2 getOrigin() {
        return origin;
    }
    
    public void setOrigin(Vector2 origin) {
        this.origin = origin;
    }
    
    public void setOrigin(float x, float y) {
        this.origin = new Vector2(x, y);
    }
    
    public void setOriginCenter() {
        this.origin = new Vector2(0.5f, 0.5f);
    }
    
    public void setOriginTopLeft() {
        this.origin = Vector2.ZERO;
    }
    
    public Vector2 getScale() {
        return scale;
    }
    
    public void setScale(Vector2 scale) {
        this.scale = scale;
    }
    
    public void setScale(float uniform) {
        this.scale = new Vector2(uniform, uniform);
    }
    
    public void setScale(float x, float y) {
        this.scale = new Vector2(x, y);
    }
    
    public float getRotation() {
        return rotation;
    }
    
    public void setRotation(float radians) {
        this.rotation = radians;
    }
    
    public boolean isFlipX() {
        return flipX;
    }
    
    public void setFlipX(boolean flip) {
        this.flipX = flip;
    }
    
    public boolean isFlipY() {
        return flipY;
    }
    
    public void setFlipY(boolean flip) {
        this.flipY = flip;
    }
    
    public float getAlpha() {
        return alpha;
    }
    
    public void setAlpha(float alpha) {
        this.alpha = Math.max(0, Math.min(1, alpha));
    }
    
    /**
     * Retorna bounds do sprite em uma posição.
     */
    public Rectangle getBounds(float x, float y) {
        int w = getWidth();
        int h = getHeight();
        float ox = w * origin.x;
        float oy = h * origin.y;
        return new Rectangle(x - ox, y - oy, w, h);
    }
    
    public Rectangle getBounds(Vector2 position) {
        return getBounds(position.x, position.y);
    }
}

