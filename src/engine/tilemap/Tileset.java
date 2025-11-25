package engine.tilemap;

import engine.graphics.Sprite;
import engine.assets.AssetManager;
import engine.util.Logger;

import java.awt.image.BufferedImage;

/**
 * Conjunto de tiles para uso em tilemaps.
 * Carrega spritesheet e divide em tiles individuais.
 */
public class Tileset {
    
    private final String name;
    private final BufferedImage image;
    private final int tileWidth;
    private final int tileHeight;
    private final int columns;
    private final int rows;
    private final int tileCount;
    
    // Cache de sprites
    private final Sprite[] tiles;
    
    // Propriedades dos tiles
    private final boolean[] solid;
    
    /**
     * Cria tileset a partir de imagem.
     */
    public Tileset(String name, BufferedImage image, int tileWidth, int tileHeight) {
        this.name = name;
        this.image = image;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        
        this.columns = image.getWidth() / tileWidth;
        this.rows = image.getHeight() / tileHeight;
        this.tileCount = columns * rows;
        
        // Cria sprites para cada tile
        this.tiles = new Sprite[tileCount];
        for (int i = 0; i < tileCount; i++) {
            int col = i % columns;
            int row = i / columns;
            tiles[i] = new Sprite(image, col * tileWidth, row * tileHeight, tileWidth, tileHeight);
            tiles[i].setOrigin(0, 0); // Origem no canto superior esquerdo
        }
        
        // Propriedades
        this.solid = new boolean[tileCount];
        
        Logger.debug("Tileset '%s' criado: %dx%d tiles (%dx%d px)", 
            name, columns, rows, tileWidth, tileHeight);
    }
    
    /**
     * Cria tileset a partir de caminho de arquivo.
     */
    public Tileset(String name, String imagePath, int tileWidth, int tileHeight) {
        this(name, AssetManager.getInstance().getTexture(imagePath), tileWidth, tileHeight);
    }
    
    // ==================== TILES ====================
    
    /**
     * Retorna sprite do tile pelo indice.
     */
    public Sprite getTile(int index) {
        if (index < 0 || index >= tileCount) {
            return null;
        }
        return tiles[index];
    }
    
    /**
     * Retorna sprite do tile por coluna/linha.
     */
    public Sprite getTile(int col, int row) {
        return getTile(row * columns + col);
    }
    
    /**
     * Verifica se indice e valido.
     */
    public boolean isValidIndex(int index) {
        return index >= 0 && index < tileCount;
    }
    
    // ==================== PROPRIEDADES ====================
    
    /**
     * Define se um tile e solido (para colisao).
     */
    public void setSolid(int index, boolean isSolid) {
        if (isValidIndex(index)) {
            solid[index] = isSolid;
        }
    }
    
    /**
     * Define range de tiles como solidos.
     */
    public void setSolidRange(int startIndex, int endIndex, boolean isSolid) {
        for (int i = startIndex; i <= endIndex && i < tileCount; i++) {
            solid[i] = isSolid;
        }
    }
    
    /**
     * Verifica se tile e solido.
     */
    public boolean isSolid(int index) {
        if (!isValidIndex(index)) return false;
        return solid[index];
    }
    
    // ==================== GETTERS ====================
    
    public String getName() {
        return name;
    }
    
    public BufferedImage getImage() {
        return image;
    }
    
    public int getTileWidth() {
        return tileWidth;
    }
    
    public int getTileHeight() {
        return tileHeight;
    }
    
    public int getColumns() {
        return columns;
    }
    
    public int getRows() {
        return rows;
    }
    
    public int getTileCount() {
        return tileCount;
    }
    
    @Override
    public String toString() {
        return String.format("Tileset['%s', %d tiles]", name, tileCount);
    }
}

