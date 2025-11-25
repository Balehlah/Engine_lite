package engine.math;

/**
 * Vetor 2D imutável para operações matemáticas.
 * Fundamental para posições, velocidades, direções e escalas.
 * 
 * Design: Imutável para evitar bugs de referência compartilhada.
 * Operações retornam novos vetores.
 */
public final class Vector2 {
    
    public static final Vector2 ZERO = new Vector2(0, 0);
    public static final Vector2 ONE = new Vector2(1, 1);
    public static final Vector2 UP = new Vector2(0, -1);
    public static final Vector2 DOWN = new Vector2(0, 1);
    public static final Vector2 LEFT = new Vector2(-1, 0);
    public static final Vector2 RIGHT = new Vector2(1, 0);
    
    public final float x;
    public final float y;
    
    public Vector2(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    public Vector2(double x, double y) {
        this.x = (float) x;
        this.y = (float) y;
    }
    
    // ==================== OPERAÇÕES BÁSICAS ====================
    
    public Vector2 add(Vector2 other) {
        return new Vector2(x + other.x, y + other.y);
    }
    
    public Vector2 add(float dx, float dy) {
        return new Vector2(x + dx, y + dy);
    }
    
    public Vector2 sub(Vector2 other) {
        return new Vector2(x - other.x, y - other.y);
    }
    
    public Vector2 sub(float dx, float dy) {
        return new Vector2(x - dx, y - dy);
    }
    
    public Vector2 mul(float scalar) {
        return new Vector2(x * scalar, y * scalar);
    }
    
    public Vector2 mul(Vector2 other) {
        return new Vector2(x * other.x, y * other.y);
    }
    
    public Vector2 div(float scalar) {
        if (scalar == 0) {
            throw new ArithmeticException("Divisão por zero");
        }
        return new Vector2(x / scalar, y / scalar);
    }
    
    // ==================== OPERAÇÕES VETORIAIS ====================
    
    public float length() {
        return (float) Math.sqrt(x * x + y * y);
    }
    
    public float lengthSquared() {
        return x * x + y * y;
    }
    
    public Vector2 normalize() {
        float len = length();
        if (len == 0) {
            return ZERO;
        }
        return new Vector2(x / len, y / len);
    }
    
    public float dot(Vector2 other) {
        return x * other.x + y * other.y;
    }
    
    /**
     * Produto cruzado 2D (retorna escalar - componente Z do vetor 3D resultante)
     */
    public float cross(Vector2 other) {
        return x * other.y - y * other.x;
    }
    
    public float distance(Vector2 other) {
        float dx = x - other.x;
        float dy = y - other.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    public float distanceSquared(Vector2 other) {
        float dx = x - other.x;
        float dy = y - other.y;
        return dx * dx + dy * dy;
    }
    
    // ==================== INTERPOLAÇÃO ====================
    
    public Vector2 lerp(Vector2 target, float t) {
        return new Vector2(
            x + (target.x - x) * t,
            y + (target.y - y) * t
        );
    }
    
    // ==================== TRANSFORMAÇÕES ====================
    
    public Vector2 negate() {
        return new Vector2(-x, -y);
    }
    
    public Vector2 perpendicular() {
        return new Vector2(-y, x);
    }
    
    public Vector2 rotate(float radians) {
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        return new Vector2(
            x * cos - y * sin,
            x * sin + y * cos
        );
    }
    
    public float angle() {
        return (float) Math.atan2(y, x);
    }
    
    public float angleTo(Vector2 other) {
        return (float) Math.atan2(other.y - y, other.x - x);
    }
    
    // ==================== UTILITÁRIOS ====================
    
    public int intX() {
        return (int) x;
    }
    
    public int intY() {
        return (int) y;
    }
    
    public Vector2 floor() {
        return new Vector2((float) Math.floor(x), (float) Math.floor(y));
    }
    
    public Vector2 ceil() {
        return new Vector2((float) Math.ceil(x), (float) Math.ceil(y));
    }
    
    public Vector2 round() {
        return new Vector2(Math.round(x), Math.round(y));
    }
    
    public Vector2 abs() {
        return new Vector2(Math.abs(x), Math.abs(y));
    }
    
    public Vector2 clamp(Vector2 min, Vector2 max) {
        return new Vector2(
            Math.max(min.x, Math.min(max.x, x)),
            Math.max(min.y, Math.min(max.y, y))
        );
    }
    
    // ==================== OBJECT OVERRIDES ====================
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Vector2)) return false;
        Vector2 other = (Vector2) obj;
        return Float.compare(x, other.x) == 0 && Float.compare(y, other.y) == 0;
    }
    
    @Override
    public int hashCode() {
        return 31 * Float.hashCode(x) + Float.hashCode(y);
    }
    
    @Override
    public String toString() {
        return String.format("Vector2(%.2f, %.2f)", x, y);
    }
}

