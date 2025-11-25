package engine.tilemap;

import engine.math.Rectangle;
import engine.math.Vector2;
import engine.util.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapa de tiles 2D.
 * Suporta multiplas camadas e colisao.
 */
public class Tilemap {
    
    private final int width;     // Largura em tiles
    private final int height;    // Altura em tiles
    private final int tileWidth;
    private final int tileHeight;
    
    private final Tileset tileset;
    
    // Camadas de tiles (indice -1 = vazio)
    private final List<int[]> layers;
    private final List<String> layerNames;
    
    // Camada de colisao
    private boolean[] collision;
    private boolean useAutoCollision = true;
    
    public Tilemap(int width, int height, Tileset tileset) {
        this.width = width;
        this.height = height;
        this.tileset = tileset;
        this.tileWidth = tileset.getTileWidth();
        this.tileHeight = tileset.getTileHeight();
        
        this.layers = new ArrayList<>();
        this.layerNames = new ArrayList<>();
        this.collision = new boolean[width * height];
        
        // Cria camada padrao
        addLayer("default");
        
        Logger.debug("Tilemap criado: %dx%d tiles", width, height);
    }
    
    // ==================== CAMADAS ====================
    
    /**
     * Adiciona nova camada.
     */
    public int addLayer(String name) {
        int[] layer = new int[width * height];
        // Inicializa com -1 (vazio)
        for (int i = 0; i < layer.length; i++) {
            layer[i] = -1;
        }
        layers.add(layer);
        layerNames.add(name);
        return layers.size() - 1;
    }
    
    /**
     * Retorna indice da camada pelo nome.
     */
    public int getLayerIndex(String name) {
        return layerNames.indexOf(name);
    }
    
    /**
     * Retorna numero de camadas.
     */
    public int getLayerCount() {
        return layers.size();
    }
    
    // ==================== TILES ====================
    
    /**
     * Define tile em uma posicao e camada.
     */
    public void setTile(int layerIndex, int x, int y, int tileIndex) {
        if (!isValidPosition(x, y) || layerIndex < 0 || layerIndex >= layers.size()) {
            return;
        }
        
        layers.get(layerIndex)[y * width + x] = tileIndex;
        
        // Atualiza colisao automatica
        if (useAutoCollision && layerIndex == 0) {
            updateAutoCollision(x, y, tileIndex);
        }
    }
    
    /**
     * Define tile na camada padrao (0).
     */
    public void setTile(int x, int y, int tileIndex) {
        setTile(0, x, y, tileIndex);
    }
    
    /**
     * Retorna indice do tile em uma posicao e camada.
     */
    public int getTile(int layerIndex, int x, int y) {
        if (!isValidPosition(x, y) || layerIndex < 0 || layerIndex >= layers.size()) {
            return -1;
        }
        return layers.get(layerIndex)[y * width + x];
    }
    
    /**
     * Retorna tile da camada padrao.
     */
    public int getTile(int x, int y) {
        return getTile(0, x, y);
    }
    
    /**
     * Preenche area com um tile.
     */
    public void fill(int layerIndex, int x, int y, int w, int h, int tileIndex) {
        for (int ty = y; ty < y + h; ty++) {
            for (int tx = x; tx < x + w; tx++) {
                setTile(layerIndex, tx, ty, tileIndex);
            }
        }
    }
    
    /**
     * Limpa tile (define como -1).
     */
    public void clearTile(int layerIndex, int x, int y) {
        setTile(layerIndex, x, y, -1);
    }
    
    // ==================== COLISAO ====================
    
    /**
     * Define colisao manual em uma posicao.
     */
    public void setCollision(int x, int y, boolean solid) {
        if (isValidPosition(x, y)) {
            collision[y * width + x] = solid;
        }
    }
    
    /**
     * Verifica colisao em uma posicao de tile.
     */
    public boolean isSolid(int x, int y) {
        if (!isValidPosition(x, y)) {
            return true; // Fora do mapa = solido
        }
        return collision[y * width + x];
    }
    
    /**
     * Verifica colisao em posicao de mundo.
     */
    public boolean isSolidAt(float worldX, float worldY) {
        int tileX = (int) (worldX / tileWidth);
        int tileY = (int) (worldY / tileHeight);
        return isSolid(tileX, tileY);
    }
    
    /**
     * Verifica colisao com um retangulo.
     */
    public boolean checkCollision(Rectangle rect) {
        int startX = (int) (rect.left() / tileWidth);
        int startY = (int) (rect.top() / tileHeight);
        int endX = (int) (rect.right() / tileWidth);
        int endY = (int) (rect.bottom() / tileHeight);
        
        for (int y = startY; y <= endY; y++) {
            for (int x = startX; x <= endX; x++) {
                if (isSolid(x, y)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Retorna tiles solidos que colidem com um retangulo.
     */
    public List<Rectangle> getCollidingTiles(Rectangle rect) {
        List<Rectangle> result = new ArrayList<>();
        
        int startX = (int) (rect.left() / tileWidth);
        int startY = (int) (rect.top() / tileHeight);
        int endX = (int) (rect.right() / tileWidth);
        int endY = (int) (rect.bottom() / tileHeight);
        
        for (int y = startY; y <= endY; y++) {
            for (int x = startX; x <= endX; x++) {
                if (isSolid(x, y)) {
                    result.add(getTileBounds(x, y));
                }
            }
        }
        return result;
    }
    
    /**
     * Atualiza colisao automatica baseada no tileset.
     */
    private void updateAutoCollision(int x, int y, int tileIndex) {
        if (tileIndex < 0) {
            collision[y * width + x] = false;
        } else {
            collision[y * width + x] = tileset.isSolid(tileIndex);
        }
    }
    
    /**
     * Ativa/desativa colisao automatica baseada no tileset.
     */
    public void setAutoCollision(boolean enabled) {
        this.useAutoCollision = enabled;
    }
    
    /**
     * Recalcula toda a colisao baseada no tileset.
     */
    public void rebuildCollision() {
        if (!layers.isEmpty()) {
            int[] layer = layers.get(0);
            for (int i = 0; i < layer.length; i++) {
                int tileIndex = layer[i];
                collision[i] = tileIndex >= 0 && tileset.isSolid(tileIndex);
            }
        }
    }
    
    // ==================== CONVERSAO ====================
    
    /**
     * Converte posicao de mundo para tile.
     */
    public Vector2 worldToTile(float worldX, float worldY) {
        return new Vector2(
            (int) (worldX / tileWidth),
            (int) (worldY / tileHeight)
        );
    }
    
    /**
     * Converte posicao de tile para mundo.
     */
    public Vector2 tileToWorld(int tileX, int tileY) {
        return new Vector2(
            tileX * tileWidth,
            tileY * tileHeight
        );
    }
    
    /**
     * Retorna bounds de um tile em coordenadas de mundo.
     */
    public Rectangle getTileBounds(int tileX, int tileY) {
        return new Rectangle(
            tileX * tileWidth,
            tileY * tileHeight,
            tileWidth,
            tileHeight
        );
    }
    
    // ==================== VALIDACAO ====================
    
    public boolean isValidPosition(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }
    
    // ==================== GETTERS ====================
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public int getTileWidth() {
        return tileWidth;
    }
    
    public int getTileHeight() {
        return tileHeight;
    }
    
    /**
     * Largura do mapa em pixels.
     */
    public int getPixelWidth() {
        return width * tileWidth;
    }
    
    /**
     * Altura do mapa em pixels.
     */
    public int getPixelHeight() {
        return height * tileHeight;
    }
    
    public Rectangle getBounds() {
        return new Rectangle(0, 0, getPixelWidth(), getPixelHeight());
    }
    
    public Tileset getTileset() {
        return tileset;
    }
    
    /**
     * Retorna array da camada (para acesso direto).
     */
    public int[] getLayerData(int layerIndex) {
        if (layerIndex >= 0 && layerIndex < layers.size()) {
            return layers.get(layerIndex);
        }
        return null;
    }
    
    @Override
    public String toString() {
        return String.format("Tilemap[%dx%d, %d layers]", width, height, layers.size());
    }
}

