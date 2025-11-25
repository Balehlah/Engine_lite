package engine.graphics;

import engine.math.Vector2;
import engine.util.RandomUtils;

import java.awt.Color;

/**
 * Configuracao de emissao de particulas.
 * Define como particulas sao geradas.
 */
public class ParticleEmitter {
    
    // Posicao
    public float x = 0;
    public float y = 0;
    
    // Area de spawn
    public float spawnWidth = 0;
    public float spawnHeight = 0;
    public boolean spawnCircle = false;
    public float spawnRadius = 0;
    
    // Velocidade
    public float minSpeed = 50;
    public float maxSpeed = 100;
    public float minAngle = 0;          // Em radianos
    public float maxAngle = (float) (Math.PI * 2);
    
    // Gravidade/Aceleracao
    public float gravityX = 0;
    public float gravityY = 0;
    
    // Rotacao
    public float minRotation = 0;
    public float maxRotation = 0;
    public float minRotationSpeed = 0;
    public float maxRotationSpeed = 0;
    
    // Escala
    public float minScale = 1;
    public float maxScale = 1;
    public float minScaleSpeed = 0;
    public float maxScaleSpeed = 0;
    
    // Cor inicial
    public float startR = 1, startG = 1, startB = 1, startA = 1;
    
    // Cor final (opcional)
    public boolean useEndColor = false;
    public float endR = 1, endG = 1, endB = 1, endA = 0;
    
    // Fade
    public float minFadeSpeed = 0;
    public float maxFadeSpeed = 1;
    
    // Vida
    public float minLife = 0.5f;
    public float maxLife = 2f;
    
    // Emissao
    public float emissionRate = 10;     // Particulas por segundo
    public int burstCount = 0;          // Particulas por burst
    public boolean continuous = true;    // Emissao continua ou burst
    
    // Sprite (indice para spritesheet)
    public int minSpriteIndex = 0;
    public int maxSpriteIndex = 0;
    
    // Timer interno
    private float emissionTimer = 0;
    
    public ParticleEmitter() {}
    
    public ParticleEmitter(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    // ==================== EMISSAO ====================
    
    /**
     * Atualiza timer de emissao.
     * Retorna numero de particulas a emitir.
     */
    public int update(float dt) {
        if (!continuous) {
            return 0;
        }
        
        emissionTimer += dt;
        float interval = 1f / emissionRate;
        int count = 0;
        
        while (emissionTimer >= interval) {
            emissionTimer -= interval;
            count++;
        }
        
        return count;
    }
    
    /**
     * Retorna numero de particulas para burst.
     */
    public int burst() {
        return burstCount;
    }
    
    /**
     * Reseta timer de emissao.
     */
    public void reset() {
        emissionTimer = 0;
    }
    
    // ==================== CONFIGURACAO DE PARTICULA ====================
    
    /**
     * Configura uma particula nova.
     */
    public void configure(Particle p) {
        // Posicao
        if (spawnCircle && spawnRadius > 0) {
            Vector2 offset = RandomUtils.inCircle(spawnRadius);
            p.x = x + offset.x;
            p.y = y + offset.y;
        } else {
            p.x = x + RandomUtils.range(-spawnWidth / 2, spawnWidth / 2);
            p.y = y + RandomUtils.range(-spawnHeight / 2, spawnHeight / 2);
        }
        
        // Velocidade
        float speed = RandomUtils.range(minSpeed, maxSpeed);
        float angle = RandomUtils.range(minAngle, maxAngle);
        p.velocityX = (float) Math.cos(angle) * speed;
        p.velocityY = (float) Math.sin(angle) * speed;
        
        // Aceleracao
        p.accelerationX = gravityX;
        p.accelerationY = gravityY;
        
        // Rotacao
        p.rotation = RandomUtils.range(minRotation, maxRotation);
        p.rotationSpeed = RandomUtils.range(minRotationSpeed, maxRotationSpeed);
        
        // Escala
        p.scale = RandomUtils.range(minScale, maxScale);
        p.scaleSpeed = RandomUtils.range(minScaleSpeed, maxScaleSpeed);
        
        // Cor
        p.r = startR;
        p.g = startG;
        p.b = startB;
        p.a = startA;
        
        // Fade
        if (useEndColor) {
            // Fade calculado para chegar na cor final
            float avgLife = (minLife + maxLife) / 2;
            p.fadeSpeed = (startA - endA) / avgLife;
        } else {
            p.fadeSpeed = RandomUtils.range(minFadeSpeed, maxFadeSpeed);
        }
        
        // Vida
        p.life = RandomUtils.range(minLife, maxLife);
        p.maxLife = p.life;
        
        // Sprite
        p.spriteIndex = RandomUtils.range(minSpriteIndex, maxSpriteIndex + 1);
        if (p.spriteIndex > maxSpriteIndex) p.spriteIndex = maxSpriteIndex;
        
        p.active = true;
    }
    
    // ==================== BUILDERS ====================
    
    public ParticleEmitter setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }
    
    public ParticleEmitter setSpawnArea(float width, float height) {
        this.spawnWidth = width;
        this.spawnHeight = height;
        this.spawnCircle = false;
        return this;
    }
    
    public ParticleEmitter setSpawnCircle(float radius) {
        this.spawnRadius = radius;
        this.spawnCircle = true;
        return this;
    }
    
    public ParticleEmitter setSpeed(float min, float max) {
        this.minSpeed = min;
        this.maxSpeed = max;
        return this;
    }
    
    public ParticleEmitter setAngle(float min, float max) {
        this.minAngle = min;
        this.maxAngle = max;
        return this;
    }
    
    public ParticleEmitter setAngleDegrees(float min, float max) {
        this.minAngle = (float) Math.toRadians(min);
        this.maxAngle = (float) Math.toRadians(max);
        return this;
    }
    
    public ParticleEmitter setGravity(float x, float y) {
        this.gravityX = x;
        this.gravityY = y;
        return this;
    }
    
    public ParticleEmitter setScale(float min, float max) {
        this.minScale = min;
        this.maxScale = max;
        return this;
    }
    
    public ParticleEmitter setScaleSpeed(float min, float max) {
        this.minScaleSpeed = min;
        this.maxScaleSpeed = max;
        return this;
    }
    
    public ParticleEmitter setColor(Color color) {
        this.startR = color.getRed() / 255f;
        this.startG = color.getGreen() / 255f;
        this.startB = color.getBlue() / 255f;
        this.startA = color.getAlpha() / 255f;
        return this;
    }
    
    public ParticleEmitter setColorRange(Color start, Color end) {
        setColor(start);
        this.useEndColor = true;
        this.endR = end.getRed() / 255f;
        this.endG = end.getGreen() / 255f;
        this.endB = end.getBlue() / 255f;
        this.endA = end.getAlpha() / 255f;
        return this;
    }
    
    public ParticleEmitter setLife(float min, float max) {
        this.minLife = min;
        this.maxLife = max;
        return this;
    }
    
    public ParticleEmitter setEmissionRate(float rate) {
        this.emissionRate = rate;
        this.continuous = true;
        return this;
    }
    
    public ParticleEmitter setBurst(int count) {
        this.burstCount = count;
        this.continuous = false;
        return this;
    }
    
    // ==================== PRESETS ====================
    
    /**
     * Preset: Fogo.
     */
    public static ParticleEmitter createFire(float x, float y) {
        return new ParticleEmitter(x, y)
            .setSpawnArea(20, 5)
            .setSpeed(30, 80)
            .setAngleDegrees(250, 290)
            .setGravity(0, -50)
            .setScale(0.5f, 1.5f)
            .setScaleSpeed(-0.5f, -0.3f)
            .setColorRange(new Color(255, 200, 50), new Color(255, 50, 0, 0))
            .setLife(0.5f, 1.5f)
            .setEmissionRate(30);
    }
    
    /**
     * Preset: Fumaca.
     */
    public static ParticleEmitter createSmoke(float x, float y) {
        return new ParticleEmitter(x, y)
            .setSpawnCircle(10)
            .setSpeed(10, 30)
            .setAngleDegrees(240, 300)
            .setGravity(0, -20)
            .setScale(1, 2)
            .setScaleSpeed(0.3f, 0.5f)
            .setColorRange(new Color(100, 100, 100, 150), new Color(50, 50, 50, 0))
            .setLife(1f, 3f)
            .setEmissionRate(10);
    }
    
    /**
     * Preset: Explosao.
     */
    public static ParticleEmitter createExplosion(float x, float y) {
        return new ParticleEmitter(x, y)
            .setSpawnCircle(5)
            .setSpeed(100, 300)
            .setAngle(0, (float) (Math.PI * 2))
            .setGravity(0, 200)
            .setScale(0.5f, 1.5f)
            .setScaleSpeed(-1f, -0.5f)
            .setColorRange(new Color(255, 255, 100), new Color(255, 100, 0, 0))
            .setLife(0.3f, 0.8f)
            .setBurst(50);
    }
    
    /**
     * Preset: Faiscas.
     */
    public static ParticleEmitter createSparks(float x, float y) {
        return new ParticleEmitter(x, y)
            .setSpeed(50, 150)
            .setAngle(0, (float) (Math.PI * 2))
            .setGravity(0, 300)
            .setScale(0.2f, 0.5f)
            .setColor(new Color(255, 255, 200))
            .setLife(0.2f, 0.5f)
            .setBurst(20);
    }
    
    /**
     * Preset: Neve.
     */
    public static ParticleEmitter createSnow(float x, float y, float width) {
        return new ParticleEmitter(x, y)
            .setSpawnArea(width, 0)
            .setSpeed(20, 50)
            .setAngleDegrees(80, 100)
            .setGravity(0, 10)
            .setScale(0.3f, 0.8f)
            .setColor(Color.WHITE)
            .setLife(3f, 6f)
            .setEmissionRate(20);
    }
}

