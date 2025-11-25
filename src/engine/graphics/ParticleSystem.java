package engine.graphics;

import engine.math.Vector2;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Sistema de particulas.
 * Gerencia pool de particulas e emissores.
 */
public class ParticleSystem {
    
    // Pool de particulas
    private final Particle[] particles;
    private final int maxParticles;
    private int activeCount = 0;
    
    // Emissores
    private final List<ParticleEmitter> emitters;
    
    // Sprite para particulas (opcional)
    private Sprite[] sprites;
    
    // Configuracao visual padrao
    private int defaultSize = 4;
    private boolean useSprites = false;
    
    // Estado
    private boolean active = true;
    
    public ParticleSystem(int maxParticles) {
        this.maxParticles = maxParticles;
        this.particles = new Particle[maxParticles];
        this.emitters = new ArrayList<>();
        
        // Inicializa pool
        for (int i = 0; i < maxParticles; i++) {
            particles[i] = new Particle();
        }
    }
    
    // ==================== EMISSORES ====================
    
    /**
     * Adiciona emissor.
     */
    public void addEmitter(ParticleEmitter emitter) {
        emitters.add(emitter);
    }
    
    /**
     * Remove emissor.
     */
    public void removeEmitter(ParticleEmitter emitter) {
        emitters.remove(emitter);
    }
    
    /**
     * Remove todos os emissores.
     */
    public void clearEmitters() {
        emitters.clear();
    }
    
    // ==================== UPDATE ====================
    
    /**
     * Atualiza particulas e emissores.
     */
    public void update(float dt) {
        if (!active) return;
        
        // Atualiza particulas existentes
        activeCount = 0;
        for (Particle p : particles) {
            if (p.active) {
                p.update(dt);
                if (p.active) {
                    activeCount++;
                }
            }
        }
        
        // Emite novas particulas
        for (ParticleEmitter emitter : emitters) {
            int count = emitter.update(dt);
            for (int i = 0; i < count; i++) {
                emit(emitter);
            }
        }
    }
    
    // ==================== EMISSAO ====================
    
    /**
     * Emite uma particula de um emissor.
     */
    public boolean emit(ParticleEmitter emitter) {
        Particle p = getInactiveParticle();
        if (p == null) {
            return false; // Pool cheio
        }
        
        emitter.configure(p);
        activeCount++;
        return true;
    }
    
    /**
     * Emite burst de particulas de um emissor.
     */
    public int burst(ParticleEmitter emitter) {
        int count = emitter.burst();
        int emitted = 0;
        
        for (int i = 0; i < count; i++) {
            if (emit(emitter)) {
                emitted++;
            }
        }
        
        return emitted;
    }
    
    /**
     * Emite particula simples em uma posicao.
     */
    public void emit(float x, float y, float vx, float vy, Color color, float life) {
        Particle p = getInactiveParticle();
        if (p == null) return;
        
        p.x = x;
        p.y = y;
        p.velocityX = vx;
        p.velocityY = vy;
        p.r = color.getRed() / 255f;
        p.g = color.getGreen() / 255f;
        p.b = color.getBlue() / 255f;
        p.a = color.getAlpha() / 255f;
        p.life = life;
        p.maxLife = life;
        p.fadeSpeed = p.a / life;
        p.active = true;
        activeCount++;
    }
    
    /**
     * Encontra particula inativa no pool.
     */
    private Particle getInactiveParticle() {
        for (Particle p : particles) {
            if (!p.active) {
                return p;
            }
        }
        return null;
    }
    
    // ==================== RENDERING ====================
    
    /**
     * Renderiza todas as particulas ativas.
     */
    public void render(Renderer renderer) {
        render(renderer.getGraphics());
    }
    
    /**
     * Renderiza diretamente no Graphics2D.
     */
    public void render(Graphics2D g) {
        for (Particle p : particles) {
            if (!p.active) continue;
            
            if (useSprites && sprites != null && p.spriteIndex < sprites.length) {
                // Renderiza com sprite
                Sprite sprite = sprites[p.spriteIndex];
                sprite.setAlpha(p.a);
                sprite.setScale(p.scale);
                sprite.setRotation(p.rotation);
                sprite.draw(g, p.x, p.y);
            } else {
                // Renderiza como quadrado
                g.setColor(p.getColor());
                int size = (int) (defaultSize * p.scale);
                int halfSize = size / 2;
                g.fillRect((int) p.x - halfSize, (int) p.y - halfSize, size, size);
            }
        }
    }
    
    /**
     * Renderiza com transformacao de camera.
     */
    public void render(Renderer renderer, Camera camera) {
        Graphics2D g = renderer.getGraphics();
        
        for (Particle p : particles) {
            if (!p.active) continue;
            
            // Transforma posicao
            Vector2 screenPos = camera != null 
                ? camera.worldToScreen(new Vector2(p.x, p.y))
                : new Vector2(p.x, p.y);
            
            if (useSprites && sprites != null && p.spriteIndex < sprites.length) {
                Sprite sprite = sprites[p.spriteIndex];
                sprite.setAlpha(p.a);
                sprite.setScale(p.scale);
                sprite.setRotation(p.rotation);
                sprite.draw(g, screenPos.x, screenPos.y);
            } else {
                g.setColor(p.getColor());
                int size = (int) (defaultSize * p.scale);
                int halfSize = size / 2;
                g.fillRect((int) screenPos.x - halfSize, (int) screenPos.y - halfSize, size, size);
            }
        }
    }
    
    // ==================== CONFIGURACAO ====================
    
    /**
     * Define sprites para particulas.
     */
    public void setSprites(Sprite[] sprites) {
        this.sprites = sprites;
        this.useSprites = true;
    }
    
    /**
     * Define tamanho padrao das particulas (quando nao usa sprites).
     */
    public void setDefaultSize(int size) {
        this.defaultSize = size;
    }
    
    public void setUseSprites(boolean use) {
        this.useSprites = use;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public boolean isActive() {
        return active;
    }
    
    // ==================== CONTROLE ====================
    
    /**
     * Limpa todas as particulas ativas.
     */
    public void clear() {
        for (Particle p : particles) {
            p.active = false;
        }
        activeCount = 0;
    }
    
    /**
     * Para todos os emissores.
     */
    public void stopEmitting() {
        for (ParticleEmitter e : emitters) {
            e.continuous = false;
        }
    }
    
    /**
     * Retoma emissao de todos os emissores.
     */
    public void startEmitting() {
        for (ParticleEmitter e : emitters) {
            e.continuous = true;
        }
    }
    
    // ==================== GETTERS ====================
    
    public int getActiveCount() {
        return activeCount;
    }
    
    public int getMaxParticles() {
        return maxParticles;
    }
    
    public List<ParticleEmitter> getEmitters() {
        return emitters;
    }
    
    public boolean isFull() {
        return activeCount >= maxParticles;
    }
    
    @Override
    public String toString() {
        return String.format("ParticleSystem[%d/%d active, %d emitters]", 
            activeCount, maxParticles, emitters.size());
    }
}

