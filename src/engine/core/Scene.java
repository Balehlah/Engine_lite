package engine.core;

import engine.graphics.Renderer;
import engine.graphics.Camera;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Cena do jogo - container para entidades.
 * Gerencia o ciclo de vida das entidades e organiza o mundo do jogo.
 */
public abstract class Scene {
    
    private String name;
    
    // Entidades da cena
    private final List<Entity> entities;
    private final List<Entity> entitiesToAdd;
    private final List<Entity> entitiesToRemove;
    
    // Câmera da cena
    private Camera camera;
    
    // Estado
    private boolean initialized = false;
    private boolean active = true;
    
    public Scene(String name) {
        this.name = name;
        this.entities = new ArrayList<>();
        this.entitiesToAdd = new ArrayList<>();
        this.entitiesToRemove = new ArrayList<>();
    }
    
    public Scene() {
        this("Scene");
    }
    
    // ==================== LIFECYCLE ====================
    
    /**
     * Chamado uma vez quando a cena é inicializada.
     * Use para criar entidades iniciais, carregar assets, etc.
     */
    public abstract void onCreate();
    
    /**
     * Chamado quando a cena é destruída.
     * Override para cleanup customizado.
     */
    public void onDestroy() {
        for (Entity entity : entities) {
            entity.onDestroy();
        }
        entities.clear();
        entitiesToAdd.clear();
        entitiesToRemove.clear();
    }
    
    /**
     * Chamado quando a cena se torna ativa (entra em foco).
     */
    public void onEnter() {}
    
    /**
     * Chamado quando a cena deixa de ser ativa (sai de foco).
     */
    public void onExit() {}
    
    /**
     * Inicialização interna - chamada pelo SceneManager.
     */
    public final void initialize() {
        if (!initialized) {
            onCreate();
            initialized = true;
        }
    }
    
    // ==================== UPDATE/RENDER ====================
    
    /**
     * Atualização da cena. Override para lógica global da cena.
     */
    public void update(float deltaTime) {
        if (!active) return;
        
        // Processa adições pendentes
        for (Entity entity : entitiesToAdd) {
            entities.add(entity);
            entity.setScene(this);
            entity.onCreate();
        }
        entitiesToAdd.clear();
        
        // Processa remoções pendentes
        for (Entity entity : entitiesToRemove) {
            entities.remove(entity);
            entity.setScene(null);
        }
        entitiesToRemove.clear();
        
        // Atualiza entidades
        for (Entity entity : entities) {
            if (entity.isActive()) {
                entity.update(deltaTime);
            }
        }
        
        // Remove entidades destruídas
        Iterator<Entity> it = entities.iterator();
        while (it.hasNext()) {
            Entity entity = it.next();
            if (entity.isDestroyed()) {
                entity.setScene(null);
                it.remove();
            }
        }
        
        // Atualiza câmera
        if (camera != null) {
            camera.update(deltaTime);
        }
    }
    
    /**
     * Renderização da cena.
     */
    public void render(Renderer renderer) {
        if (!active) return;
        
        // Aplica câmera
        if (camera != null) {
            renderer.setCamera(camera);
        }
        
        // Renderiza entidades
        for (Entity entity : entities) {
            if (entity.isActive()) {
                entity.render(renderer);
            }
        }
    }
    
    // ==================== ENTIDADES ====================
    
    /**
     * Adiciona entidade à cena (será processada no próximo frame).
     */
    public void addEntity(Entity entity) {
        entitiesToAdd.add(entity);
    }
    
    /**
     * Adiciona entidade imediatamente (usar com cuidado durante update).
     */
    public void addEntityImmediate(Entity entity) {
        entities.add(entity);
        entity.setScene(this);
        entity.onCreate();
    }
    
    /**
     * Remove entidade da cena (será processada no próximo frame).
     */
    public void removeEntity(Entity entity) {
        entitiesToRemove.add(entity);
    }
    
    /**
     * Busca entidade por nome.
     */
    public Entity findByName(String name) {
        for (Entity entity : entities) {
            if (entity.getName().equals(name)) {
                return entity;
            }
        }
        // Também procura nas pendentes
        for (Entity entity : entitiesToAdd) {
            if (entity.getName().equals(name)) {
                return entity;
            }
        }
        return null;
    }
    
    /**
     * Busca todas as entidades com uma tag.
     */
    public List<Entity> findByTag(String tag) {
        List<Entity> result = new ArrayList<>();
        for (Entity entity : entities) {
            if (entity.getTag().equals(tag)) {
                result.add(entity);
            }
        }
        return result;
    }
    
    /**
     * Busca entidades com um componente específico.
     */
    public <T extends Component> List<Entity> findWithComponent(Class<T> componentType) {
        List<Entity> result = new ArrayList<>();
        for (Entity entity : entities) {
            if (entity.hasComponent(componentType)) {
                result.add(entity);
            }
        }
        return result;
    }
    
    /**
     * Retorna todas as entidades (cópia defensiva).
     */
    public List<Entity> getEntities() {
        return new ArrayList<>(entities);
    }
    
    public int getEntityCount() {
        return entities.size();
    }
    
    // ==================== CÂMERA ====================
    
    public Camera getCamera() {
        return camera;
    }
    
    public void setCamera(Camera camera) {
        this.camera = camera;
    }
    
    // ==================== ESTADO ====================
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    @Override
    public String toString() {
        return String.format("Scene['%s', entities=%d]", name, entities.size());
    }
}

