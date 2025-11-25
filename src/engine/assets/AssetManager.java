package engine.assets;

import engine.graphics.Sprite;
import engine.graphics.Animation;
import engine.util.Logger;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Gerenciador central de assets.
 * Cache inteligente para texturas, sprites, animações e fontes.
 * Singleton thread-safe.
 */
public final class AssetManager {
    
    private static AssetManager instance;
    
    // Caches por tipo
    private final Map<String, BufferedImage> textures;
    private final Map<String, Sprite> sprites;
    private final Map<String, Sprite[]> spritesheets;
    private final Map<String, Animation> animations;
    private final Map<String, Font> fonts;
    
    // Estatísticas
    private int loadCount;
    private int cacheHits;
    
    private AssetManager() {
        textures = new HashMap<>();
        sprites = new HashMap<>();
        spritesheets = new HashMap<>();
        animations = new HashMap<>();
        fonts = new HashMap<>();
    }
    
    public static AssetManager getInstance() {
        if (instance == null) {
            synchronized (AssetManager.class) {
                if (instance == null) {
                    instance = new AssetManager();
                }
            }
        }
        return instance;
    }
    
    // ==================== TEXTURAS ====================
    
    /**
     * Carrega ou retorna textura do cache.
     */
    public BufferedImage getTexture(String path) {
        if (textures.containsKey(path)) {
            cacheHits++;
            return textures.get(path);
        }
        
        BufferedImage texture = TextureLoader.load(path);
        textures.put(path, texture);
        loadCount++;
        return texture;
    }
    
    /**
     * Registra textura manualmente no cache.
     */
    public void registerTexture(String key, BufferedImage texture) {
        textures.put(key, texture);
        Logger.debug("Textura registrada: %s", key);
    }
    
    /**
     * Verifica se textura está no cache.
     */
    public boolean hasTexture(String path) {
        return textures.containsKey(path);
    }
    
    // ==================== SPRITES ====================
    
    /**
     * Cria e cacheia sprite de uma textura.
     */
    public Sprite getSprite(String texturePath) {
        if (sprites.containsKey(texturePath)) {
            cacheHits++;
            return sprites.get(texturePath);
        }
        
        BufferedImage texture = getTexture(texturePath);
        Sprite sprite = new Sprite(texture);
        sprites.put(texturePath, sprite);
        return sprite;
    }
    
    /**
     * Cria sprite de região de textura (não cacheia, pois pode haver múltiplas regiões).
     */
    public Sprite getSprite(String texturePath, int x, int y, int width, int height) {
        BufferedImage texture = getTexture(texturePath);
        return new Sprite(texture, x, y, width, height);
    }
    
    /**
     * Registra sprite manualmente.
     */
    public void registerSprite(String key, Sprite sprite) {
        sprites.put(key, sprite);
    }
    
    // ==================== SPRITESHEETS ====================
    
    /**
     * Carrega spritesheet como array de sprites.
     */
    public Sprite[] getSpritesheet(String texturePath, int tileWidth, int tileHeight) {
        String key = texturePath + "_" + tileWidth + "x" + tileHeight;
        
        if (spritesheets.containsKey(key)) {
            cacheHits++;
            return spritesheets.get(key);
        }
        
        BufferedImage texture = getTexture(texturePath);
        Sprite[] sheet = Sprite.fromSpritesheet(texture, tileWidth, tileHeight);
        spritesheets.put(key, sheet);
        Logger.debug("Spritesheet carregada: %s (%d sprites)", texturePath, sheet.length);
        return sheet;
    }
    
    /**
     * Retorna sprite específico de um spritesheet.
     */
    public Sprite getSpritesheetTile(String texturePath, int tileWidth, int tileHeight, int index) {
        Sprite[] sheet = getSpritesheet(texturePath, tileWidth, tileHeight);
        if (index >= 0 && index < sheet.length) {
            return sheet[index];
        }
        Logger.warn("Índice de sprite inválido: %d (max: %d)", index, sheet.length - 1);
        return sheet[0];
    }
    
    /**
     * Retorna sprite de spritesheet por coluna/linha.
     */
    public Sprite getSpritesheetTile(String texturePath, int tileWidth, int tileHeight, int col, int row) {
        BufferedImage texture = getTexture(texturePath);
        int cols = texture.getWidth() / tileWidth;
        int index = row * cols + col;
        return getSpritesheetTile(texturePath, tileWidth, tileHeight, index);
    }
    
    // ==================== ANIMAÇÕES ====================
    
    /**
     * Cria animação de spritesheet.
     */
    public Animation createAnimation(String texturePath, int frameWidth, int frameHeight, 
                                      float frameDuration) {
        BufferedImage texture = getTexture(texturePath);
        return new Animation(texture, frameWidth, frameHeight, frameDuration);
    }
    
    /**
     * Cria animação de subset de spritesheet.
     */
    public Animation createAnimation(String texturePath, int frameWidth, int frameHeight,
                                      int startFrame, int endFrame, float frameDuration) {
        BufferedImage texture = getTexture(texturePath);
        return new Animation(texture, frameWidth, frameHeight, startFrame, endFrame, frameDuration);
    }
    
    /**
     * Cria animação de linha específica de spritesheet.
     */
    public Animation createAnimationFromRow(String texturePath, int frameWidth, int frameHeight,
                                            int row, int frameCount, float frameDuration) {
        BufferedImage texture = getTexture(texturePath);
        return Animation.fromRow(texture, frameWidth, frameHeight, row, frameCount, frameDuration);
    }
    
    /**
     * Registra animação no cache.
     */
    public void registerAnimation(String key, Animation animation) {
        animations.put(key, animation);
    }
    
    /**
     * Retorna animação do cache.
     */
    public Animation getAnimation(String key) {
        return animations.get(key);
    }
    
    // ==================== FONTES ====================
    
    /**
     * Retorna fonte do cache ou cria nova.
     */
    public Font getFont(String name, int style, int size) {
        String key = name + "_" + style + "_" + size;
        
        if (fonts.containsKey(key)) {
            cacheHits++;
            return fonts.get(key);
        }
        
        Font font = new Font(name, style, size);
        fonts.put(key, font);
        return font;
    }
    
    /**
     * Cria fonte padrão para pixel art.
     */
    public Font getPixelFont(int size) {
        return getFont("Monospaced", Font.PLAIN, size);
    }
    
    /**
     * Registra fonte customizada.
     */
    public void registerFont(String key, Font font) {
        fonts.put(key, font);
    }
    
    // ==================== GERENCIAMENTO ====================
    
    /**
     * Remove textura do cache.
     */
    public void unloadTexture(String path) {
        textures.remove(path);
        sprites.remove(path);
        
        // Remove spritesheets relacionados
        spritesheets.entrySet().removeIf(entry -> entry.getKey().startsWith(path + "_"));
        
        Logger.debug("Textura descarregada: %s", path);
    }
    
    /**
     * Limpa todos os caches.
     */
    public void clear() {
        textures.clear();
        sprites.clear();
        spritesheets.clear();
        animations.clear();
        // Fontes geralmente são mantidas
        
        loadCount = 0;
        cacheHits = 0;
        
        Logger.info("AssetManager limpo");
    }
    
    /**
     * Limpa apenas animações (útil para trocar de cena).
     */
    public void clearAnimations() {
        animations.clear();
    }
    
    // ==================== ESTATÍSTICAS ====================
    
    public int getTextureCount() {
        return textures.size();
    }
    
    public int getSpriteCount() {
        return sprites.size();
    }
    
    public int getSpritesheetCount() {
        return spritesheets.size();
    }
    
    public int getAnimationCount() {
        return animations.size();
    }
    
    public int getFontCount() {
        return fonts.size();
    }
    
    public int getTotalAssetCount() {
        return getTextureCount() + getSpriteCount() + getSpritesheetCount() + 
               getAnimationCount() + getFontCount();
    }
    
    public int getLoadCount() {
        return loadCount;
    }
    
    public int getCacheHits() {
        return cacheHits;
    }
    
    public float getCacheHitRate() {
        int total = loadCount + cacheHits;
        return total > 0 ? (float) cacheHits / total : 0;
    }
    
    /**
     * Retorna relatório de uso.
     */
    public String getStats() {
        return String.format(
            "AssetManager Stats:\n" +
            "  Texturas: %d\n" +
            "  Sprites: %d\n" +
            "  Spritesheets: %d\n" +
            "  Animações: %d\n" +
            "  Fontes: %d\n" +
            "  Carregamentos: %d\n" +
            "  Cache hits: %d (%.1f%%)",
            getTextureCount(), getSpriteCount(), getSpritesheetCount(),
            getAnimationCount(), getFontCount(),
            loadCount, cacheHits, getCacheHitRate() * 100
        );
    }
}

