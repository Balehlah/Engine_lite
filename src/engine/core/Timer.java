package engine.core;

/**
 * Timer de alta precisão para game loop.
 * Fornece deltaTime real, FPS tracking e controle de tempo.
 */
public final class Timer {
    
    // Constantes de conversão
    private static final double NANOS_PER_SECOND = 1_000_000_000.0;
    private static final double NANOS_PER_MILLI = 1_000_000.0;
    
    // Estado do timer
    private long startTime;
    private long lastFrameTime;
    private long currentFrameTime;
    
    // Delta time
    private double deltaTime;          // Em segundos (fração)
    private double deltaTimeMs;        // Em milissegundos
    private double unscaledDeltaTime;  // Sem time scale
    
    // Time scale (para slow-motion, pause, etc)
    private double timeScale = 1.0;
    
    // FPS tracking
    private int frameCount;
    private int fps;
    private long fpsTimer;
    
    // Acumuladores
    private double totalTime;
    private long totalFrames;
    
    // Limites de segurança
    private double maxDeltaTime = 0.1; // 100ms máximo (evita spiral of death)
    
    public Timer() {
        reset();
    }
    
    // ==================== LIFECYCLE ====================
    
    /**
     * Reseta o timer. Chamar no início do jogo.
     */
    public void reset() {
        startTime = System.nanoTime();
        lastFrameTime = startTime;
        currentFrameTime = startTime;
        fpsTimer = startTime;
        
        deltaTime = 0;
        deltaTimeMs = 0;
        unscaledDeltaTime = 0;
        
        frameCount = 0;
        fps = 0;
        totalTime = 0;
        totalFrames = 0;
    }
    
    /**
     * Atualiza o timer. Chamar no início de cada frame.
     */
    public void update() {
        currentFrameTime = System.nanoTime();
        
        // Calcula delta time real (sem escala)
        long elapsed = currentFrameTime - lastFrameTime;
        unscaledDeltaTime = elapsed / NANOS_PER_SECOND;
        
        // Aplica limite de segurança
        if (unscaledDeltaTime > maxDeltaTime) {
            unscaledDeltaTime = maxDeltaTime;
        }
        
        // Aplica time scale
        deltaTime = unscaledDeltaTime * timeScale;
        deltaTimeMs = deltaTime * 1000.0;
        
        // Atualiza acumuladores
        totalTime += deltaTime;
        totalFrames++;
        
        // FPS tracking
        frameCount++;
        if (currentFrameTime - fpsTimer >= NANOS_PER_SECOND) {
            fps = frameCount;
            frameCount = 0;
            fpsTimer = currentFrameTime;
        }
        
        lastFrameTime = currentFrameTime;
    }
    
    // ==================== GETTERS ====================
    
    /**
     * Delta time em segundos (já com time scale aplicado).
     * Usar para toda lógica de movimento/física.
     */
    public double getDeltaTime() {
        return deltaTime;
    }
    
    /**
     * Delta time como float (conveniência para cálculos).
     */
    public float getDeltaTimeF() {
        return (float) deltaTime;
    }
    
    /**
     * Delta time em milissegundos.
     */
    public double getDeltaTimeMs() {
        return deltaTimeMs;
    }
    
    /**
     * Delta time real, sem time scale.
     */
    public double getUnscaledDeltaTime() {
        return unscaledDeltaTime;
    }
    
    /**
     * FPS atual (atualizado a cada segundo).
     */
    public int getFps() {
        return fps;
    }
    
    /**
     * Tempo total desde o reset (em segundos, com time scale).
     */
    public double getTotalTime() {
        return totalTime;
    }
    
    /**
     * Total de frames desde o reset.
     */
    public long getTotalFrames() {
        return totalFrames;
    }
    
    /**
     * Tempo desde o início em segundos (sem time scale).
     */
    public double getTimeSinceStart() {
        return (System.nanoTime() - startTime) / NANOS_PER_SECOND;
    }
    
    // ==================== TIME SCALE ====================
    
    public double getTimeScale() {
        return timeScale;
    }
    
    public void setTimeScale(double scale) {
        this.timeScale = Math.max(0, scale);
    }
    
    public void pause() {
        timeScale = 0;
    }
    
    public void resume() {
        timeScale = 1.0;
    }
    
    public boolean isPaused() {
        return timeScale == 0;
    }
    
    // ==================== CONFIGURAÇÃO ====================
    
    public void setMaxDeltaTime(double maxDelta) {
        this.maxDeltaTime = maxDelta;
    }
    
    // ==================== UTILITÁRIOS ====================
    
    /**
     * Retorna timestamp atual em nanosegundos.
     */
    public static long nanoTime() {
        return System.nanoTime();
    }
    
    /**
     * Converte nanosegundos para segundos.
     */
    public static double nanosToSeconds(long nanos) {
        return nanos / NANOS_PER_SECOND;
    }
    
    /**
     * Converte nanosegundos para milissegundos.
     */
    public static double nanosToMillis(long nanos) {
        return nanos / NANOS_PER_MILLI;
    }
    
    @Override
    public String toString() {
        return String.format("Timer[fps=%d, dt=%.4f, total=%.2fs, frames=%d]", 
            fps, deltaTime, totalTime, totalFrames);
    }
}

