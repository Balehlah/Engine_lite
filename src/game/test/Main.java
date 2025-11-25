package game.test;

import engine.core.Game;
import engine.core.SceneManager;
import engine.graphics.Renderer;
import engine.util.Logger;

/**
 * Exemplo de jogo usando a Pixel Engine.
 * 
 * Demonstra:
 * - Game loop profissional com deltaTime
 * - Sistema de cenas
 * - Input (teclado + mouse)
 * - Rendering com câmera 2D
 * - Sprites e entidades
 * - Paleta de cores estilo PICO-8
 */
public class Main extends Game {
    
    public Main() {
        super("Pixel Engine - Demo", 800, 600);
        
        // Configurações opcionais
        setTargetUps(60);   // 60 updates por segundo
        setTargetFps(0);    // FPS ilimitado
    }
    
    @Override
    protected void onCreate() {
        Logger.info("Inicializando jogo de demonstração...");
        
        // Registra cena de teste
        TestScene testScene = new TestScene();
        SceneManager.getInstance().register("test", testScene);
        
        // Inicia na cena de teste
        SceneManager.getInstance().changeScene("test");
        
        Logger.info("Jogo inicializado!");
    }
    
    @Override
    protected void onUpdate(float deltaTime) {
        // Lógica global do jogo (se necessário)
        // A maior parte da lógica está nas cenas e entidades
    }
    
    @Override
    protected void onRender(Renderer renderer) {
        // Rendering global (após cenas)
        // Útil para overlays, debug, etc.
    }
    
    @Override
    protected void onDestroy() {
        Logger.info("Encerrando jogo...");
    }
    
    // ==================== PONTO DE ENTRADA ====================
    
    public static void main(String[] args) {
        // Configurações iniciais de logging
        Logger.setLevel(Logger.Level.INFO);
        Logger.setShowTimestamp(true);
        Logger.setShowCaller(false);
        
        Logger.info("═══════════════════════════════════════════");
        Logger.info("   PIXEL ENGINE GABRIEL 2025");
        Logger.info("   Demonstração Funcional");
        Logger.info("═══════════════════════════════════════════");
        
        // Cria e inicia o jogo
        Main game = new Main();
        game.start();
    }
}

