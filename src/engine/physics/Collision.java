package engine.physics;

import engine.math.Vector2;
import engine.math.Rectangle;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilitarios de colisao.
 * Funcoes estaticas para deteccao de colisao entre diferentes formas.
 */
public final class Collision {
    
    private Collision() {}
    
    // ==================== PONTO ====================
    
    /**
     * Ponto dentro de retangulo.
     */
    public static boolean pointInRect(float px, float py, float rx, float ry, float rw, float rh) {
        return px >= rx && px < rx + rw && py >= ry && py < ry + rh;
    }
    
    public static boolean pointInRect(Vector2 point, Rectangle rect) {
        return pointInRect(point.x, point.y, rect.x, rect.y, rect.width, rect.height);
    }
    
    /**
     * Ponto dentro de circulo.
     */
    public static boolean pointInCircle(float px, float py, float cx, float cy, float radius) {
        float dx = px - cx;
        float dy = py - cy;
        return dx * dx + dy * dy <= radius * radius;
    }
    
    public static boolean pointInCircle(Vector2 point, Vector2 center, float radius) {
        return pointInCircle(point.x, point.y, center.x, center.y, radius);
    }
    
    // ==================== RETANGULO ====================
    
    /**
     * Retangulo vs retangulo.
     */
    public static boolean rectVsRect(float x1, float y1, float w1, float h1,
                                     float x2, float y2, float w2, float h2) {
        return x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2;
    }
    
    public static boolean rectVsRect(Rectangle a, Rectangle b) {
        return rectVsRect(a.x, a.y, a.width, a.height, b.x, b.y, b.width, b.height);
    }
    
    public static boolean rectVsRect(AABB a, AABB b) {
        return a.intersects(b);
    }
    
    // ==================== CIRCULO ====================
    
    /**
     * Circulo vs circulo.
     */
    public static boolean circleVsCircle(float x1, float y1, float r1,
                                         float x2, float y2, float r2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float distSq = dx * dx + dy * dy;
        float radSum = r1 + r2;
        return distSq <= radSum * radSum;
    }
    
    public static boolean circleVsCircle(Vector2 c1, float r1, Vector2 c2, float r2) {
        return circleVsCircle(c1.x, c1.y, r1, c2.x, c2.y, r2);
    }
    
    /**
     * Circulo vs retangulo.
     */
    public static boolean circleVsRect(float cx, float cy, float radius,
                                       float rx, float ry, float rw, float rh) {
        // Encontra ponto mais proximo no retangulo
        float closestX = Math.max(rx, Math.min(cx, rx + rw));
        float closestY = Math.max(ry, Math.min(cy, ry + rh));
        
        // Verifica distancia
        float dx = cx - closestX;
        float dy = cy - closestY;
        return dx * dx + dy * dy <= radius * radius;
    }
    
    public static boolean circleVsRect(Vector2 center, float radius, Rectangle rect) {
        return circleVsRect(center.x, center.y, radius, rect.x, rect.y, rect.width, rect.height);
    }
    
    public static boolean circleVsRect(Vector2 center, float radius, AABB box) {
        return circleVsRect(center.x, center.y, radius, box.x, box.y, box.width, box.height);
    }
    
    // ==================== LINHA ====================
    
    /**
     * Linha vs linha.
     * Retorna ponto de intersecao ou null.
     */
    public static Vector2 lineVsLine(Vector2 p1, Vector2 p2, Vector2 p3, Vector2 p4) {
        float x1 = p1.x, y1 = p1.y;
        float x2 = p2.x, y2 = p2.y;
        float x3 = p3.x, y3 = p3.y;
        float x4 = p4.x, y4 = p4.y;
        
        float denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (Math.abs(denom) < 0.0001f) {
            return null; // Paralelas
        }
        
        float t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / denom;
        float u = -((x1 - x2) * (y1 - y3) - (y1 - y2) * (x1 - x3)) / denom;
        
        if (t >= 0 && t <= 1 && u >= 0 && u <= 1) {
            return new Vector2(x1 + t * (x2 - x1), y1 + t * (y2 - y1));
        }
        
        return null;
    }
    
    /**
     * Linha vs retangulo.
     */
    public static boolean lineVsRect(Vector2 p1, Vector2 p2, Rectangle rect) {
        // Verifica intersecao com cada lado
        Vector2 tl = new Vector2(rect.left(), rect.top());
        Vector2 tr = new Vector2(rect.right(), rect.top());
        Vector2 bl = new Vector2(rect.left(), rect.bottom());
        Vector2 br = new Vector2(rect.right(), rect.bottom());
        
        return lineVsLine(p1, p2, tl, tr) != null ||
               lineVsLine(p1, p2, tr, br) != null ||
               lineVsLine(p1, p2, br, bl) != null ||
               lineVsLine(p1, p2, bl, tl) != null ||
               rect.contains(p1) || rect.contains(p2);
    }
    
    // ==================== RESOLUCAO ====================
    
    /**
     * Calcula vetor de separacao minimo entre dois AABBs.
     */
    public static Vector2 getSeparation(AABB a, AABB b) {
        return a.getMTV(b);
    }
    
    /**
     * Calcula vetor de separacao para circulo vs AABB.
     */
    public static Vector2 getSeparation(Vector2 circleCenter, float radius, AABB box) {
        // Encontra ponto mais proximo
        float closestX = Math.max(box.left(), Math.min(circleCenter.x, box.right()));
        float closestY = Math.max(box.top(), Math.min(circleCenter.y, box.bottom()));
        
        float dx = circleCenter.x - closestX;
        float dy = circleCenter.y - closestY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        
        if (dist >= radius) {
            return Vector2.ZERO;
        }
        
        if (dist == 0) {
            // Centro dentro do box
            return new Vector2(radius, 0);
        }
        
        float overlap = radius - dist;
        return new Vector2(dx / dist * overlap, dy / dist * overlap);
    }
    
    // ==================== SWEEP ====================
    
    /**
     * Sweep test - AABB movendo vs AABB estatico.
     * Retorna t (0-1) do momento da colisao, ou 1 se nao colidir.
     */
    public static float sweepAABB(AABB moving, Vector2 velocity, AABB stationary) {
        // Expandir box estatico pelo tamanho do moving
        AABB expanded = new AABB(
            stationary.x - moving.width / 2,
            stationary.y - moving.height / 2,
            stationary.width + moving.width,
            stationary.height + moving.height
        );
        
        // Raycast do centro do moving
        Vector2 center = moving.center();
        Raycast ray = new Raycast(center, velocity, velocity.length());
        
        if (ray.testAABB(expanded)) {
            return ray.distance / velocity.length();
        }
        
        return 1.0f;
    }
    
    // ==================== BROAD PHASE ====================
    
    /**
     * Broad phase - retorna pares de AABBs que potencialmente colidem.
     * Usa sweep and prune simplificado.
     */
    public static List<int[]> broadPhase(List<AABB> boxes) {
        List<int[]> pairs = new ArrayList<>();
        
        // Ordenar por X (simplificado - nao otimizado)
        for (int i = 0; i < boxes.size(); i++) {
            AABB a = boxes.get(i);
            for (int j = i + 1; j < boxes.size(); j++) {
                AABB b = boxes.get(j);
                if (a.intersects(b)) {
                    pairs.add(new int[]{i, j});
                }
            }
        }
        
        return pairs;
    }
}

