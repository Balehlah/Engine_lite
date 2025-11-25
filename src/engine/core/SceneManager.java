package engine.core;

import engine.graphics.Renderer;
import engine.util.Logger;
import java.util.HashMap;
import java.util.Map;

/**
 * Gerenciador de cenas.
 * Controla transições entre cenas e mantém registro de cenas disponíveis.
 */
public final class SceneManager {
    
    // Singleton
    private static SceneManager instance;
    
    // Cenas registradas
    private final Map<String, Scene> scenes;
    
    // Cena atual
    private Scene currentScene;
    private Scene pendingScene;
    
    private SceneManager() {
        this.scenes = new HashMap<>();
    }
    
    public static SceneManager getInstance() {
        if (instance == null) {
            instance = new SceneManager();
        }
        return instance;
    }
    
    // ==================== REGISTRO ====================
    
    /**
     * Registra uma cena com um nome.
     */
    public void register(String name, Scene scene) {
        if (scenes.containsKey(name)) {
            Logger.warn("Sobrescrevendo cena existente: %s", name);
        }
        scene.setName(name);
        scenes.put(name, scene);
        Logger.debug("Cena registrada: %s", name);
    }
    
    /**
     * Registra uma cena usando o nome já definido nela.
     */
    public void register(Scene scene) {
        register(scene.getName(), scene);
    }
    
    /**
     * Remove uma cena do registro.
     */
    public void unregister(String name) {
        Scene scene = scenes.remove(name);
        if (scene != null) {
            if (scene == currentScene) {
                currentScene = null;
            }
            scene.onDestroy();
            Logger.debug("Cena removida: %s", name);
        }
    }
    
    /**
     * Verifica se uma cena está registrada.
     */
    public boolean hasScene(String name) {
        return scenes.containsKey(name);
    }
    
    // ==================== TRANSIÇÕES ====================
    
    /**
     * Muda para uma cena registrada pelo nome.
     * A transição acontece no próximo frame.
     */
    public void changeScene(String name) {
        Scene scene = scenes.get(name);
        if (scene == null) {
            Logger.error("Cena não encontrada: %s", name);
            return;
        }
        pendingScene = scene;
        Logger.info("Transição de cena agendada: %s -> %s", 
            currentScene != null ? currentScene.getName() : "null", name);
    }
    
    /**
     * Muda para uma cena diretamente (não precisa estar registrada).
     */
    public void changeScene(Scene scene) {
        pendingScene = scene;
        Logger.info("Transição de cena agendada para: %s", scene.getName());
    }
    
    /**
     * Processa transição pendente.
     * Chamado internamente pelo Game loop.
     */
    public void processPendingTransition() {
        if (pendingScene != null) {
            // Sai da cena atual
            if (currentScene != null) {
                currentScene.onExit();
            }
            
            // Inicializa nova cena se necessário
            if (!pendingScene.isInitialized()) {
                pendingScene.initialize();
            }
            
            // Entra na nova cena
            currentScene = pendingScene;
            pendingScene = null;
            currentScene.onEnter();
            
            Logger.info("Cena ativa: %s", currentScene.getName());
        }
    }
    
    /**
     * Recarrega a cena atual.
     */
    public void reloadCurrentScene() {
        if (currentScene != null) {
            String name = currentScene.getName();
            currentScene.onDestroy();
            
            // Recria a cena (precisa ser uma nova instância)
            try {
                Scene newScene = currentScene.getClass().getDeclaredConstructor().newInstance();
                newScene.setName(name);
                scenes.put(name, newScene);
                pendingScene = newScene;
                Logger.info("Recarregando cena: %s", name);
            } catch (Exception e) {
                Logger.error("Falha ao recarregar cena: " + name, e);
            }
        }
    }
    
    // ==================== UPDATE/RENDER ====================
    
    /**
     * Atualiza a cena atual.
     */
    public void update(float deltaTime) {
        processPendingTransition();
        
        if (currentScene != null) {
            currentScene.update(deltaTime);
        }
    }
    
    /**
     * Renderiza a cena atual.
     */
    public void render(Renderer renderer) {
        if (currentScene != null) {
            currentScene.render(renderer);
        }
    }
    
    // ==================== GETTERS ====================
    
    public Scene getCurrentScene() {
        return currentScene;
    }
    
    public Scene getScene(String name) {
        return scenes.get(name);
    }
    
    public int getSceneCount() {
        return scenes.size();
    }
    
    /**
     * Limpa todas as cenas e reseta o manager.
     */
    public void clear() {
        for (Scene scene : scenes.values()) {
            scene.onDestroy();
        }
        scenes.clear();
        currentScene = null;
        pendingScene = null;
        Logger.info("SceneManager limpo");
    }
}

