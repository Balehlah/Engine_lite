package engine.graphics;

import engine.math.Vector2;
import engine.math.Rectangle;

/**
 * Câmera 2D para controle de viewport.
 * Suporta posição, zoom, rotação e seguir alvos.
 */
public class Camera {
    
    // Transform
    private Vector2 position;
    private float zoom;
    private float rotation;
    
    // Viewport
    private final int viewportWidth;
    private final int viewportHeight;
    
    // Follow
    private Vector2 targetPosition;
    private float followSpeed;       // 0 = instantâneo, 1+ = lerp suave
    private Vector2 followOffset;
    
    // Bounds (limites do mundo)
    private Rectangle bounds;
    private boolean useBounds;
    
    // Shake
    private float shakeIntensity;
    private float shakeDuration;
    private float shakeTimer;
    private Vector2 shakeOffset;
    
    public Camera(int viewportWidth, int viewportHeight) {
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
        
        this.position = Vector2.ZERO;
        this.zoom = 1.0f;
        this.rotation = 0;
        
        this.targetPosition = null;
        this.followSpeed = 5.0f;
        this.followOffset = Vector2.ZERO;
        
        this.useBounds = false;
        this.shakeOffset = Vector2.ZERO;
    }
    
    // ==================== UPDATE ====================
    
    public void update(float deltaTime) {
        // Follow target
        if (targetPosition != null) {
            Vector2 target = targetPosition.add(followOffset);
            
            if (followSpeed <= 0) {
                position = target;
            } else {
                position = position.lerp(target, followSpeed * deltaTime);
            }
        }
        
        // Shake
        if (shakeTimer > 0) {
            shakeTimer -= deltaTime;
            float intensity = shakeIntensity * (shakeTimer / shakeDuration);
            shakeOffset = new Vector2(
                (float) (Math.random() * 2 - 1) * intensity,
                (float) (Math.random() * 2 - 1) * intensity
            );
        } else {
            shakeOffset = Vector2.ZERO;
        }
        
        // Apply bounds
        if (useBounds && bounds != null) {
            clampToBounds();
        }
    }
    
    // ==================== TRANSFORM ====================
    
    public Vector2 getPosition() {
        return position;
    }
    
    /**
     * Posição efetiva considerando shake.
     */
    public Vector2 getEffectivePosition() {
        return position.add(shakeOffset);
    }
    
    public void setPosition(Vector2 position) {
        this.position = position;
        this.targetPosition = null; // Cancela follow
    }
    
    public void setPosition(float x, float y) {
        setPosition(new Vector2(x, y));
    }
    
    public void move(Vector2 delta) {
        setPosition(position.add(delta));
    }
    
    public void move(float dx, float dy) {
        setPosition(position.add(dx, dy));
    }
    
    public float getZoom() {
        return zoom;
    }
    
    public void setZoom(float zoom) {
        this.zoom = Math.max(0.1f, zoom);
    }
    
    public void zoomIn(float amount) {
        setZoom(zoom + amount);
    }
    
    public void zoomOut(float amount) {
        setZoom(zoom - amount);
    }
    
    public float getRotation() {
        return rotation;
    }
    
    public void setRotation(float rotation) {
        this.rotation = rotation;
    }
    
    // ==================== FOLLOW ====================
    
    /**
     * Faz a câmera seguir uma posição.
     */
    public void follow(Vector2 target) {
        this.targetPosition = target;
    }
    
    /**
     * Para de seguir.
     */
    public void stopFollowing() {
        this.targetPosition = null;
    }
    
    public void setFollowSpeed(float speed) {
        this.followSpeed = speed;
    }
    
    public void setFollowOffset(Vector2 offset) {
        this.followOffset = offset;
    }
    
    public void setFollowOffset(float x, float y) {
        this.followOffset = new Vector2(x, y);
    }
    
    // ==================== BOUNDS ====================
    
    public void setBounds(Rectangle bounds) {
        this.bounds = bounds;
        this.useBounds = bounds != null;
    }
    
    public void setBounds(float x, float y, float width, float height) {
        setBounds(new Rectangle(x, y, width, height));
    }
    
    public void clearBounds() {
        this.bounds = null;
        this.useBounds = false;
    }
    
    private void clampToBounds() {
        float halfWidth = (viewportWidth / 2f) / zoom;
        float halfHeight = (viewportHeight / 2f) / zoom;
        
        float minX = bounds.left() + halfWidth;
        float maxX = bounds.right() - halfWidth;
        float minY = bounds.top() + halfHeight;
        float maxY = bounds.bottom() - halfHeight;
        
        float x = position.x;
        float y = position.y;
        
        if (maxX > minX) {
            x = Math.max(minX, Math.min(maxX, x));
        } else {
            x = bounds.centerX();
        }
        
        if (maxY > minY) {
            y = Math.max(minY, Math.min(maxY, y));
        } else {
            y = bounds.centerY();
        }
        
        position = new Vector2(x, y);
    }
    
    // ==================== SHAKE ====================
    
    public void shake(float intensity, float duration) {
        this.shakeIntensity = intensity;
        this.shakeDuration = duration;
        this.shakeTimer = duration;
    }
    
    public boolean isShaking() {
        return shakeTimer > 0;
    }
    
    // ==================== CONVERSÃO ====================
    
    /**
     * Converte coordenada de mundo para tela.
     */
    public Vector2 worldToScreen(Vector2 worldPos) {
        Vector2 camPos = getEffectivePosition();
        float x = (worldPos.x - camPos.x) * zoom + viewportWidth / 2f;
        float y = (worldPos.y - camPos.y) * zoom + viewportHeight / 2f;
        return new Vector2(x, y);
    }
    
    /**
     * Converte coordenada de tela para mundo.
     */
    public Vector2 screenToWorld(Vector2 screenPos) {
        Vector2 camPos = getEffectivePosition();
        float x = (screenPos.x - viewportWidth / 2f) / zoom + camPos.x;
        float y = (screenPos.y - viewportHeight / 2f) / zoom + camPos.y;
        return new Vector2(x, y);
    }
    
    /**
     * Retorna o retângulo visível no mundo.
     */
    public Rectangle getViewBounds() {
        float halfWidth = (viewportWidth / 2f) / zoom;
        float halfHeight = (viewportHeight / 2f) / zoom;
        Vector2 camPos = getEffectivePosition();
        
        return new Rectangle(
            camPos.x - halfWidth,
            camPos.y - halfHeight,
            halfWidth * 2,
            halfHeight * 2
        );
    }
    
    /**
     * Verifica se um ponto está visível na câmera.
     */
    public boolean isVisible(Vector2 point) {
        return getViewBounds().contains(point);
    }
    
    /**
     * Verifica se um retângulo está visível na câmera.
     */
    public boolean isVisible(Rectangle rect) {
        return getViewBounds().intersects(rect);
    }
    
    // ==================== VIEWPORT ====================
    
    public int getViewportWidth() {
        return viewportWidth;
    }
    
    public int getViewportHeight() {
        return viewportHeight;
    }
    
    /**
     * Centraliza a câmera em uma posição.
     */
    public void centerOn(Vector2 position) {
        setPosition(position);
    }
    
    public void centerOn(float x, float y) {
        setPosition(x, y);
    }
}

