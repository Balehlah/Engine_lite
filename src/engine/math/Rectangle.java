package engine.math;

/**
 * Retângulo para bounds, colisões e áreas.
 * Usa floats para precisão em física e movimento.
 */
public final class Rectangle {
    
    public final float x;
    public final float y;
    public final float width;
    public final float height;
    
    public Rectangle(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    public Rectangle(Vector2 position, float width, float height) {
        this.x = position.x;
        this.y = position.y;
        this.width = width;
        this.height = height;
    }
    
    public Rectangle(Vector2 position, Vector2 size) {
        this.x = position.x;
        this.y = position.y;
        this.width = size.x;
        this.height = size.y;
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
    
    public float area() {
        return width * height;
    }
    
    // ==================== COLISÃO ====================
    
    public boolean contains(float px, float py) {
        return px >= x && px < x + width && py >= y && py < y + height;
    }
    
    public boolean contains(Vector2 point) {
        return contains(point.x, point.y);
    }
    
    public boolean contains(Rectangle other) {
        return other.x >= x && 
               other.y >= y && 
               other.right() <= right() && 
               other.bottom() <= bottom();
    }
    
    public boolean intersects(Rectangle other) {
        return x < other.right() && 
               right() > other.x && 
               y < other.bottom() && 
               bottom() > other.y;
    }
    
    /**
     * Retorna a interseção entre dois retângulos, ou null se não houver.
     */
    public Rectangle intersection(Rectangle other) {
        if (!intersects(other)) {
            return null;
        }
        
        float ix = Math.max(x, other.x);
        float iy = Math.max(y, other.y);
        float iw = Math.min(right(), other.right()) - ix;
        float ih = Math.min(bottom(), other.bottom()) - iy;
        
        return new Rectangle(ix, iy, iw, ih);
    }
    
    // ==================== TRANSFORMAÇÕES ====================
    
    public Rectangle move(float dx, float dy) {
        return new Rectangle(x + dx, y + dy, width, height);
    }
    
    public Rectangle move(Vector2 delta) {
        return move(delta.x, delta.y);
    }
    
    public Rectangle setPosition(float newX, float newY) {
        return new Rectangle(newX, newY, width, height);
    }
    
    public Rectangle setPosition(Vector2 pos) {
        return new Rectangle(pos.x, pos.y, width, height);
    }
    
    public Rectangle expand(float amount) {
        return new Rectangle(x - amount, y - amount, width + amount * 2, height + amount * 2);
    }
    
    public Rectangle expand(float horizontal, float vertical) {
        return new Rectangle(x - horizontal, y - vertical, width + horizontal * 2, height + vertical * 2);
    }
    
    /**
     * Retorna o menor retângulo que contém ambos.
     */
    public Rectangle union(Rectangle other) {
        float ux = Math.min(x, other.x);
        float uy = Math.min(y, other.y);
        float uw = Math.max(right(), other.right()) - ux;
        float uh = Math.max(bottom(), other.bottom()) - uy;
        return new Rectangle(ux, uy, uw, uh);
    }
    
    // ==================== CONVERSÃO ====================
    
    public int intX() {
        return (int) x;
    }
    
    public int intY() {
        return (int) y;
    }
    
    public int intWidth() {
        return (int) width;
    }
    
    public int intHeight() {
        return (int) height;
    }
    
    public java.awt.Rectangle toAwtRectangle() {
        return new java.awt.Rectangle(intX(), intY(), intWidth(), intHeight());
    }
    
    // ==================== OBJECT OVERRIDES ====================
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Rectangle)) return false;
        Rectangle other = (Rectangle) obj;
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
        return String.format("Rectangle(%.2f, %.2f, %.2f, %.2f)", x, y, width, height);
    }
}

