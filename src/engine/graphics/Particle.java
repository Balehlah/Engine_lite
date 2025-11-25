package engine.graphics;

import engine.math.Vector2;
import java.awt.Color;

/**
 * Particula individual.
 * Dados puros para uso no sistema de particulas.
 */
public class Particle {
    
    // Posicao e movimento
    public float x;
    public float y;
    public float velocityX;
    public float velocityY;
    public float accelerationX;
    public float accelerationY;
    
    // Rotacao
    public float rotation;
    public float rotationSpeed;
    
    // Escala
    public float scale;
    public float scaleSpeed;
    
    // Cor
    public float r, g, b, a;
    public float fadeSpeed;
    
    // Tempo de vida
    public float life;
    public float maxLife;
    
    // Estado
    public boolean active;
    
    // Dados extras
    public int spriteIndex;
    
    public Particle() {
        reset();
    }
    
    /**
     * Reseta particula para reutilizacao.
     */
    public void reset() {
        x = y = 0;
        velocityX = velocityY = 0;
        accelerationX = accelerationY = 0;
        rotation = 0;
        rotationSpeed = 0;
        scale = 1;
        scaleSpeed = 0;
        r = g = b = a = 1;
        fadeSpeed = 0;
        life = maxLife = 1;
        active = false;
        spriteIndex = 0;
    }
    
    /**
     * Atualiza a particula.
     */
    public void update(float dt) {
        if (!active) return;
        
        // Movimento
        velocityX += accelerationX * dt;
        velocityY += accelerationY * dt;
        x += velocityX * dt;
        y += velocityY * dt;
        
        // Rotacao
        rotation += rotationSpeed * dt;
        
        // Escala
        scale += scaleSpeed * dt;
        if (scale < 0) scale = 0;
        
        // Fade
        a -= fadeSpeed * dt;
        if (a < 0) a = 0;
        
        // Vida
        life -= dt;
        if (life <= 0) {
            active = false;
        }
    }
    
    /**
     * Retorna progresso de vida (0 = inicio, 1 = fim).
     */
    public float getLifeProgress() {
        return 1f - (life / maxLife);
    }
    
    /**
     * Retorna cor como objeto Color.
     */
    public Color getColor() {
        return new Color(
            Math.max(0, Math.min(1, r)),
            Math.max(0, Math.min(1, g)),
            Math.max(0, Math.min(1, b)),
            Math.max(0, Math.min(1, a))
        );
    }
    
    public Vector2 getPosition() {
        return new Vector2(x, y);
    }
    
    public Vector2 getVelocity() {
        return new Vector2(velocityX, velocityY);
    }
}

