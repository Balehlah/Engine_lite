package engine.tilemap;

import engine.graphics.Camera;
import engine.graphics.Renderer;
import engine.graphics.Sprite;
import engine.math.Rectangle;

/**
 * Renderizador otimizado para tilemaps.
 * Renderiza apenas tiles visiveis (culling).
 */
public class TilemapRenderer {
    
    private final Tilemap tilemap;
    private final Tileset tileset;
    
    // Opcoes de debug
    private boolean showGrid = false;
    private boolean showCollision = false;
    private java.awt.Color gridColor = new java.awt.Color(100, 100, 100, 100);
    private java.awt.Color collisionColor = new java.awt.Color(255, 0, 0, 100);
    
    public TilemapRenderer(Tilemap tilemap) {
        this.tilemap = tilemap;
        this.tileset = tilemap.getTileset();
    }
    
    // ==================== RENDERING ====================
    
    /**
     * Renderiza todas as camadas visiveis.
     */
    public void render(Renderer renderer) {
        render(renderer, null);
    }
    
    /**
     * Renderiza todas as camadas com culling baseado na camera.
     */
    public void render(Renderer renderer, Camera camera) {
        for (int layer = 0; layer < tilemap.getLayerCount(); layer++) {
            renderLayer(renderer, camera, layer);
        }
        
        // Debug
        if (showGrid) {
            renderGrid(renderer, camera);
        }
        if (showCollision) {
            renderCollision(renderer, camera);
        }
    }
    
    /**
     * Renderiza uma camada especifica.
     */
    public void renderLayer(Renderer renderer, Camera camera, int layerIndex) {
        int[] layerData = tilemap.getLayerData(layerIndex);
        if (layerData == null) return;
        
        int tileWidth = tilemap.getTileWidth();
        int tileHeight = tilemap.getTileHeight();
        
        // Calcula tiles visiveis
        int startX = 0;
        int startY = 0;
        int endX = tilemap.getWidth();
        int endY = tilemap.getHeight();
        
        if (camera != null) {
            Rectangle view = camera.getViewBounds();
            startX = Math.max(0, (int) (view.left() / tileWidth));
            startY = Math.max(0, (int) (view.top() / tileHeight));
            endX = Math.min(tilemap.getWidth(), (int) (view.right() / tileWidth) + 1);
            endY = Math.min(tilemap.getHeight(), (int) (view.bottom() / tileHeight) + 1);
        }
        
        // Renderiza tiles visiveis
        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                int tileIndex = layerData[y * tilemap.getWidth() + x];
                
                if (tileIndex >= 0) {
                    Sprite tile = tileset.getTile(tileIndex);
                    if (tile != null) {
                        float worldX = x * tileWidth;
                        float worldY = y * tileHeight;
                        renderer.drawSprite(tile, worldX, worldY);
                    }
                }
            }
        }
    }
    
    /**
     * Renderiza apenas a camada de background (primeira).
     */
    public void renderBackground(Renderer renderer, Camera camera) {
        renderLayer(renderer, camera, 0);
    }
    
    /**
     * Renderiza camadas acima da primeira (overlays).
     */
    public void renderForeground(Renderer renderer, Camera camera) {
        for (int layer = 1; layer < tilemap.getLayerCount(); layer++) {
            renderLayer(renderer, camera, layer);
        }
    }
    
    // ==================== DEBUG ====================
    
    /**
     * Renderiza grid.
     */
    private void renderGrid(Renderer renderer, Camera camera) {
        int tileWidth = tilemap.getTileWidth();
        int tileHeight = tilemap.getTileHeight();
        
        int startX = 0;
        int startY = 0;
        int endX = tilemap.getWidth();
        int endY = tilemap.getHeight();
        
        if (camera != null) {
            Rectangle view = camera.getViewBounds();
            startX = Math.max(0, (int) (view.left() / tileWidth));
            startY = Math.max(0, (int) (view.top() / tileHeight));
            endX = Math.min(tilemap.getWidth(), (int) (view.right() / tileWidth) + 1);
            endY = Math.min(tilemap.getHeight(), (int) (view.bottom() / tileHeight) + 1);
        }
        
        // Linhas verticais
        for (int x = startX; x <= endX; x++) {
            int worldX = x * tileWidth;
            renderer.drawLine(worldX, startY * tileHeight, worldX, endY * tileHeight, gridColor);
        }
        
        // Linhas horizontais
        for (int y = startY; y <= endY; y++) {
            int worldY = y * tileHeight;
            renderer.drawLine(startX * tileWidth, worldY, endX * tileWidth, worldY, gridColor);
        }
    }
    
    /**
     * Renderiza tiles de colisao.
     */
    private void renderCollision(Renderer renderer, Camera camera) {
        int tileWidth = tilemap.getTileWidth();
        int tileHeight = tilemap.getTileHeight();
        
        int startX = 0;
        int startY = 0;
        int endX = tilemap.getWidth();
        int endY = tilemap.getHeight();
        
        if (camera != null) {
            Rectangle view = camera.getViewBounds();
            startX = Math.max(0, (int) (view.left() / tileWidth));
            startY = Math.max(0, (int) (view.top() / tileHeight));
            endX = Math.min(tilemap.getWidth(), (int) (view.right() / tileWidth) + 1);
            endY = Math.min(tilemap.getHeight(), (int) (view.bottom() / tileHeight) + 1);
        }
        
        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                if (tilemap.isSolid(x, y)) {
                    renderer.fillRect(x * tileWidth, y * tileHeight, tileWidth, tileHeight, collisionColor);
                }
            }
        }
    }
    
    // ==================== CONFIGURACAO ====================
    
    public void setShowGrid(boolean show) {
        this.showGrid = show;
    }
    
    public boolean isShowGrid() {
        return showGrid;
    }
    
    public void setShowCollision(boolean show) {
        this.showCollision = show;
    }
    
    public boolean isShowCollision() {
        return showCollision;
    }
    
    public void setGridColor(java.awt.Color color) {
        this.gridColor = color;
    }
    
    public void setCollisionColor(java.awt.Color color) {
        this.collisionColor = color;
    }
    
    public Tilemap getTilemap() {
        return tilemap;
    }
}

