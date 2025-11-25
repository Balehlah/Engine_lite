package engine.core;

import engine.display.Window;
import engine.graphics.Renderer;
import engine.input.Input;
import engine.util.Logger;

/**
 * Classe principal da engine.
 * Gerencia o game loop, timing e ciclo de vida.
 * 
 * Design: Fixed timestep para update, render livre para máximo FPS.
 */
public abstract class Game implements Runnable {
    
    // Configuração
    private final String title;
    private final int width;
    private final int height;
    private int targetUps = 60;  // Updates por segundo
    private int targetFps = 0;   // 0 = ilimitado
    
    // Componentes da engine
    private Window window;
    private Renderer renderer;
    private Timer timer;
    
    // Estado
    private Thread gameThread;
    private volatile boolean running = false;
    private boolean initialized = false;
    
    // Métricas
    private int ups;  // Updates reais por segundo
    private int fps;  // Frames reais por segundo
    
    public Game(String title, int width, int height) {
        this.title = title;
        this.width = width;
        this.height = height;
    }
    
    // ==================== LIFECYCLE ====================
    
    /**
     * Inicializa e inicia a engine.
     */
    public final void start() {
        if (running) {
            Logger.warn("Game já está rodando");
            return;
        }
        
        running = true;
        gameThread = new Thread(this, "GameThread");
        gameThread.start();
    }
    
    /**
     * Para a engine de forma segura.
     */
    public final void stop() {
        running = false;
        
        if (gameThread != null) {
            try {
                gameThread.join(1000);
            } catch (InterruptedException e) {
                Logger.error("Interrupção ao parar game thread", e);
            }
        }
    }
    
    /**
     * Inicialização interna da engine.
     */
    private void init() {
        Logger.info("═══════════════════════════════════════════");
        Logger.info("   PIXEL ENGINE - Inicializando...");
        Logger.info("═══════════════════════════════════════════");
        
        // Cria janela
        window = new Window(title, width, height);
        Logger.info("Janela criada: %dx%d", width, height);
        
        // Cria renderer
        renderer = new Renderer(window);
        Logger.info("Renderer inicializado");
        
        // Cria timer
        timer = new Timer();
        Logger.info("Timer inicializado");
        
        // Registra input na janela
        Input.getInstance().register(window);
        Logger.info("Input registrado");
        
        // Callback para usuário
        onCreate();
        
        initialized = true;
        Logger.info("Engine pronta!");
        Logger.separator();
    }
    
    /**
     * Cleanup ao encerrar.
     */
    private void cleanup() {
        Logger.separator();
        Logger.info("Encerrando engine...");
        
        onDestroy();
        
        SceneManager.getInstance().clear();
        
        if (window != null) {
            window.dispose();
        }
        
        Logger.info("Engine encerrada.");
    }
    
    // ==================== GAME LOOP ====================
    
    @Override
    public final void run() {
        init();
        
        final double nsPerUpdate = 1_000_000_000.0 / targetUps;
        final double nsPerFrame = targetFps > 0 ? 1_000_000_000.0 / targetFps : 0;
        
        long lastTime = System.nanoTime();
        long fpsTimer = System.nanoTime();
        
        double deltaAccumulator = 0;
        double frameAccumulator = 0;
        
        int updateCount = 0;
        int frameCount = 0;
        
        while (running) {
            long now = System.nanoTime();
            long elapsed = now - lastTime;
            lastTime = now;
            
            deltaAccumulator += elapsed;
            frameAccumulator += elapsed;
            
            // Fixed timestep updates
            boolean updated = false;
            while (deltaAccumulator >= nsPerUpdate) {
                timer.update();
                
                // Processa input
                Input.getInstance().update();
                
                // Atualiza cenas
                SceneManager.getInstance().update(timer.getDeltaTimeF());
                
                // Callback de update do usuário
                onUpdate(timer.getDeltaTimeF());
                
                deltaAccumulator -= nsPerUpdate;
                updateCount++;
                updated = true;
            }
            
            // Render (com ou sem limite de FPS)
            boolean shouldRender = nsPerFrame == 0 || frameAccumulator >= nsPerFrame;
            
            if (shouldRender && updated) {
                renderer.begin();
                
                // Renderiza cenas
                SceneManager.getInstance().render(renderer);
                
                // Callback de render do usuário
                onRender(renderer);
                
                renderer.end();
                
                frameCount++;
                frameAccumulator = 0;
            }
            
            // Atualiza métricas a cada segundo
            if (now - fpsTimer >= 1_000_000_000) {
                fps = frameCount;
                ups = updateCount;
                frameCount = 0;
                updateCount = 0;
                fpsTimer = now;
                
                // Atualiza título com métricas (debug)
                window.setTitle(title + " | FPS: " + fps + " UPS: " + ups);
            }
            
            // Yield para não queimar CPU desnecessariamente
            if (!updated) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        
        cleanup();
    }
    
    // ==================== CALLBACKS ABSTRATOS ====================
    
    /**
     * Chamado uma vez na inicialização.
     * Use para carregar assets, criar cenas, etc.
     */
    protected abstract void onCreate();
    
    /**
     * Chamado a cada update (fixed timestep).
     * @param deltaTime Tempo desde último update em segundos
     */
    protected abstract void onUpdate(float deltaTime);
    
    /**
     * Chamado a cada frame de render.
     * @param renderer Renderer para desenhar
     */
    protected abstract void onRender(Renderer renderer);
    
    /**
     * Chamado ao encerrar.
     * Use para liberar recursos.
     */
    protected void onDestroy() {}
    
    // ==================== CONFIGURAÇÃO ====================
    
    public void setTargetUps(int ups) {
        this.targetUps = ups;
    }
    
    public void setTargetFps(int fps) {
        this.targetFps = fps;
    }
    
    // ==================== GETTERS ====================
    
    public Window getWindow() {
        return window;
    }
    
    public Renderer getRenderer() {
        return renderer;
    }
    
    public Timer getTimer() {
        return timer;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public int getFps() {
        return fps;
    }
    
    public int getUps() {
        return ups;
    }
    
    public boolean isRunning() {
        return running;
    }
    
    /**
     * Encerra o jogo.
     */
    public void exit() {
        running = false;
    }
}
