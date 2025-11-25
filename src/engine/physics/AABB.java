package engine.physics;

import engine.math.Rectangle;
import engine.math.Vector2;

/**
 * Axis-Aligned Bounding Box para colisao.
 * Retangulo alinhado aos eixos - colisao rapida e simples.
 */
public class AABB {
    
    public float x;
    public float y;
    public float width;
    public float height;
    
    public AABB(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    public AABB(Vector2 position, Vector2 size) {
        this.x = position.x;
        this.y = position.y;
        this.width = size.x;
        this.height = size.y;
    }
    
    public AABB(Rectangle rect) {
        this.x = rect.x;
        this.y = rect.y;
        this.width = rect.width;
        this.height = rect.height;
    }
    
    // ==================== PROPRIEDADES ====================
    
    public float left() {
        return x;
    }
    
    public float right() {
        return x + width;
    }
    
    public float top() {
        return y;
    }
    
    public float bottom() {
        return y + height;
    }
    
    public float centerX() {
        return x + width / 2;
    }
    
    public float centerY() {
        return y + height / 2;
    }
    
    public Vector2 center() {
        return new Vector2(centerX(), centerY());
    }
    
    public Vector2 position() {
        return new Vector2(x, y);
    }
    
    public Vector2 size() {
        return new Vector2(width, height);
    }
    
    // ==================== COLISAO ====================
    
    /**
     * Verifica se contem um ponto.
     */
    public boolean contains(float px, float py) {
        return px >= x && px < x + width && py >= y && py < y + height;
    }
    
    public boolean contains(Vector2 point) {
        return contains(point.x, point.y);
    }
    
    /**
     * Verifica se contem outro AABB completamente.
     */
    public boolean contains(AABB other) {
        return other.x >= x && 
               other.y >= y && 
               other.right() <= right() && 
               other.bottom() <= bottom();
    }
    
    /**
     * Verifica intersecao com outro AABB.
     */
    public boolean intersects(AABB other) {
        return x < other.right() && 
               right() > other.x && 
               y < other.bottom() && 
               bottom() > other.y;
    }
    
    /**
     * Retorna a area de intersecao, ou null se nao houver.
     */
    public AABB intersection(AABB other) {
        if (!intersects(other)) {
            return null;
        }
        
        float ix = Math.max(x, other.x);
        float iy = Math.max(y, other.y);
        float iw = Math.min(right(), other.right()) - ix;
        float ih = Math.min(bottom(), other.bottom()) - iy;
        
        return new AABB(ix, iy, iw, ih);
    }
    
    /**
     * Calcula overlap (penetracao) com outro AABB.
     * Retorna vetor de resolucao minima.
     */
    public Vector2 getOverlap(AABB other) {
        if (!intersects(other)) {
            return Vector2.ZERO;
        }
        
        float overlapX, overlapY;
        
        // Overlap horizontal
        if (centerX() < other.centerX()) {
            overlapX = right() - other.left();
        } else {
            overlapX = -(other.right() - left());
        }
        
        // Overlap vertical
        if (centerY() < other.centerY()) {
            overlapY = bottom() - other.top();
        } else {
            overlapY = -(other.bottom() - top());
        }
        
        return new Vector2(overlapX, overlapY);
    }
    
    /**
     * Retorna vetor de resolucao minima (MTV - Minimum Translation Vector).
     */
    public Vector2 getMTV(AABB other) {
        Vector2 overlap = getOverlap(other);
        
        if (Math.abs(overlap.x) < Math.abs(overlap.y)) {
            return new Vector2(-overlap.x, 0);
        } else {
            return new Vector2(0, -overlap.y);
        }
    }
    
    // ==================== TRANSFORMACOES ====================
    
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    public void setPosition(Vector2 pos) {
        this.x = pos.x;
        this.y = pos.y;
    }
    
    public void move(float dx, float dy) {
        this.x += dx;
        this.y += dy;
    }
    
    public void move(Vector2 delta) {
        this.x += delta.x;
        this.y += delta.y;
    }
    
    public void setCenter(float cx, float cy) {
        this.x = cx - width / 2;
        this.y = cy - height / 2;
    }
    
    public void setCenter(Vector2 center) {
        setCenter(center.x, center.y);
    }
    
    /**
     * Expande o AABB em todas as direcoes.
     */
    public AABB expand(float amount) {
        return new AABB(x - amount, y - amount, width + amount * 2, height + amount * 2);
    }
    
    /**
     * Retorna AABB expandido para incluir movimento.
     * Util para broad-phase collision detection.
     */
    public AABB expandToInclude(Vector2 velocity) {
        float minX = velocity.x < 0 ? x + velocity.x : x;
        float minY = velocity.y < 0 ? y + velocity.y : y;
        float maxX = velocity.x > 0 ? right() + velocity.x : right();
        float maxY = velocity.y > 0 ? bottom() + velocity.y : bottom();
        
        return new AABB(minX, minY, maxX - minX, maxY - minY);
    }
    
    // ==================== CONVERSAO ====================
    
    public Rectangle toRectangle() {
        return new Rectangle(x, y, width, height);
    }
    
    public AABB copy() {
        return new AABB(x, y, width, height);
    }
    
    // ==================== OBJECT ====================
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof AABB)) return false;
        AABB other = (AABB) obj;
        return Float.compare(x, other.x) == 0 && 
               Float.compare(y, other.y) == 0 &&
               Float.compare(width, other.width) == 0 &&
               Float.compare(height, other.height) == 0;
    }
    
    @Override
    public int hashCode() {
        int result = Float.hashCode(x);
        result = 31 * result + Float.hashCode(y);
        result = 31 * result + Float.hashCode(width);
        result = 31 * result + Float.hashCode(height);
        return result;
    }
    
    @Override
    public String toString() {
        return String.format("AABB(%.2f, %.2f, %.2f, %.2f)", x, y, width, height);
    }
}

