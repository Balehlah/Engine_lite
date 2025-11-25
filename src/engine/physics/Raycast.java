package engine.physics;

import engine.math.Vector2;

/**
 * Resultado de um raycast.
 */
public class Raycast {
    
    /**
     * Origem do raio.
     */
    public final Vector2 origin;
    
    /**
     * Direcao do raio (normalizada).
     */
    public final Vector2 direction;
    
    /**
     * Distancia maxima do raio.
     */
    public final float maxDistance;
    
    /**
     * Se houve hit.
     */
    public boolean hit;
    
    /**
     * Ponto de colisao.
     */
    public Vector2 point;
    
    /**
     * Normal da superficie no ponto de colisao.
     */
    public Vector2 normal;
    
    /**
     * Distancia ate o ponto de colisao.
     */
    public float distance;
    
    /**
     * Objeto atingido (pode ser qualquer coisa).
     */
    public Object hitObject;
    
    public Raycast(Vector2 origin, Vector2 direction, float maxDistance) {
        this.origin = origin;
        this.direction = direction.normalize();
        this.maxDistance = maxDistance;
        this.hit = false;
    }
    
    public Raycast(float originX, float originY, float dirX, float dirY, float maxDistance) {
        this(new Vector2(originX, originY), new Vector2(dirX, dirY), maxDistance);
    }
    
    // ==================== FACTORY METHODS ====================
    
    /**
     * Cria raycast de um ponto a outro.
     */
    public static Raycast fromTo(Vector2 from, Vector2 to) {
        Vector2 dir = to.sub(from);
        float dist = dir.length();
        return new Raycast(from, dir.normalize(), dist);
    }
    
    /**
     * Cria raycast de um ponto em uma direcao.
     */
    public static Raycast fromDirection(Vector2 origin, Vector2 direction, float maxDistance) {
        return new Raycast(origin, direction, maxDistance);
    }
    
    // ==================== TESTES ====================
    
    /**
     * Testa colisao com AABB.
     * Retorna true se houver colisao, e atualiza hit, point, normal, distance.
     */
    public boolean testAABB(AABB box) {
        float tmin = 0;
        float tmax = maxDistance;
        
        float[] normals = new float[4];
        
        // Teste eixo X
        if (direction.x != 0) {
            float tx1 = (box.left() - origin.x) / direction.x;
            float tx2 = (box.right() - origin.x) / direction.x;
            
            float tminX = Math.min(tx1, tx2);
            float tmaxX = Math.max(tx1, tx2);
            
            if (tminX > tmin) {
                tmin = tminX;
                normals[0] = tx1 < tx2 ? -1 : 1;
                normals[1] = 0;
            }
            if (tmaxX < tmax) {
                tmax = tmaxX;
            }
        } else if (origin.x < box.left() || origin.x > box.right()) {
            return false;
        }
        
        if (tmin > tmax) return false;
        
        // Teste eixo Y
        if (direction.y != 0) {
            float ty1 = (box.top() - origin.y) / direction.y;
            float ty2 = (box.bottom() - origin.y) / direction.y;
            
            float tminY = Math.min(ty1, ty2);
            float tmaxY = Math.max(ty1, ty2);
            
            if (tminY > tmin) {
                tmin = tminY;
                normals[0] = 0;
                normals[1] = ty1 < ty2 ? -1 : 1;
            }
            if (tmaxY < tmax) {
                tmax = tmaxY;
            }
        } else if (origin.y < box.top() || origin.y > box.bottom()) {
            return false;
        }
        
        if (tmin > tmax || tmin < 0 || tmin > maxDistance) {
            return false;
        }
        
        // Hit!
        hit = true;
        distance = tmin;
        point = origin.add(direction.mul(distance));
        normal = new Vector2(normals[0], normals[1]);
        hitObject = box;
        
        return true;
    }
    
    /**
     * Testa colisao com circulo.
     */
    public boolean testCircle(Vector2 center, float radius) {
        Vector2 oc = origin.sub(center);
        
        float a = direction.dot(direction);
        float b = 2.0f * oc.dot(direction);
        float c = oc.dot(oc) - radius * radius;
        
        float discriminant = b * b - 4 * a * c;
        
        if (discriminant < 0) {
            return false;
        }
        
        float t = (-b - (float) Math.sqrt(discriminant)) / (2 * a);
        
        if (t < 0 || t > maxDistance) {
            // Tenta a outra raiz
            t = (-b + (float) Math.sqrt(discriminant)) / (2 * a);
            if (t < 0 || t > maxDistance) {
                return false;
            }
        }
        
        hit = true;
        distance = t;
        point = origin.add(direction.mul(distance));
        normal = point.sub(center).normalize();
        
        return true;
    }
    
    /**
     * Testa colisao com linha.
     */
    public boolean testLine(Vector2 p1, Vector2 p2) {
        Vector2 v1 = origin.sub(p1);
        Vector2 v2 = p2.sub(p1);
        Vector2 v3 = new Vector2(-direction.y, direction.x);
        
        float dot = v2.dot(v3);
        if (Math.abs(dot) < 0.0001f) {
            return false; // Paralelo
        }
        
        float t1 = v2.cross(v1) / dot;
        float t2 = v1.dot(v3) / dot;
        
        if (t1 >= 0 && t1 <= maxDistance && t2 >= 0 && t2 <= 1) {
            hit = true;
            distance = t1;
            point = origin.add(direction.mul(distance));
            normal = new Vector2(-v2.y, v2.x).normalize();
            return true;
        }
        
        return false;
    }
    
    // ==================== GETTERS ====================
    
    /**
     * Retorna ponto ao longo do raio.
     */
    public Vector2 getPoint(float t) {
        return origin.add(direction.mul(t));
    }
    
    /**
     * Retorna ponto final do raio (na distancia maxima).
     */
    public Vector2 getEndPoint() {
        return origin.add(direction.mul(maxDistance));
    }
    
    /**
     * Reseta resultado para reutilizar.
     */
    public void reset() {
        hit = false;
        point = null;
        normal = null;
        distance = 0;
        hitObject = null;
    }
    
    @Override
    public String toString() {
        if (hit) {
            return String.format("Raycast[hit at %.2f, point=%s, normal=%s]", distance, point, normal);
        }
        return "Raycast[no hit]";
    }
}

