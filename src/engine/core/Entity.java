package engine.core;

import engine.math.Vector2;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entidade base do jogo.
 * Container para componentes com posição, rotação e escala básicas.
 * 
 * Design: Entidades são containers leves. Comportamento vem de Components.
 */
public class Entity {
    
    // Identificação
    private static long nextId = 0;
    private final long id;
    private String name;
    private String tag;
    
    // Transform básico (built-in para conveniência)
    private Vector2 position;
    private float rotation;
    private Vector2 scale;
    
    // Componentes
    private final Map<Class<? extends Component>, Component> components;
    
    // Estado
    private boolean active = true;
    private boolean destroyed = false;
    
    // Hierarquia
    private Entity parent;
    private final List<Entity> children;
    
    // Referência à cena atual
    private Scene scene;
    
    public Entity() {
        this("Entity");
    }
    
    public Entity(String name) {
        this.id = nextId++;
        this.name = name;
        this.tag = "";
        
        this.position = Vector2.ZERO;
        this.rotation = 0;
        this.scale = Vector2.ONE;
        
        this.components = new HashMap<>();
        this.children = new ArrayList<>();
    }
    
    // ==================== LIFECYCLE ====================
    
    /**
     * Chamado quando a entidade é adicionada à cena.
     */
    public void onCreate() {}
    
    /**
     * Atualização lógica. Override para comportamento custom.
     */
    public void update(float deltaTime) {
        if (!active) return;
        
        // Atualiza filhos
        for (Entity child : children) {
            child.update(deltaTime);
        }
    }
    
    /**
     * Renderização. Override para desenho custom.
     */
    public void render(engine.graphics.Renderer renderer) {
        if (!active) return;
        
        // Renderiza filhos
        for (Entity child : children) {
            child.render(renderer);
        }
    }
    
    /**
     * Chamado quando a entidade é destruída.
     */
    public void onDestroy() {
        // Limpa componentes
        for (Component comp : components.values()) {
            comp.onDetach();
        }
        components.clear();
        
        // Destrói filhos
        for (Entity child : children) {
            child.destroy();
        }
        children.clear();
    }
    
    public void destroy() {
        if (!destroyed) {
            destroyed = true;
            onDestroy();
            if (scene != null) {
                scene.removeEntity(this);
            }
        }
    }
    
    // ==================== COMPONENTES ====================
    
    public <T extends Component> T addComponent(T component) {
        Class<? extends Component> type = component.getClass();
        
        // Remove componente existente do mesmo tipo
        if (components.containsKey(type)) {
            removeComponent(type);
        }
        
        components.put(type, component);
        component.setEntity(this);
        component.onAttach();
        
        return component;
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Component> T getComponent(Class<T> type) {
        return (T) components.get(type);
    }
    
    public boolean hasComponent(Class<? extends Component> type) {
        return components.containsKey(type);
    }
    
    public <T extends Component> T removeComponent(Class<T> type) {
        @SuppressWarnings("unchecked")
        T component = (T) components.remove(type);
        if (component != null) {
            component.onDetach();
            component.setEntity(null);
        }
        return component;
    }
    
    public List<Component> getAllComponents() {
        return new ArrayList<>(components.values());
    }
    
    // ==================== HIERARQUIA ====================
    
    public void addChild(Entity child) {
        if (child.parent != null) {
            child.parent.removeChild(child);
        }
        children.add(child);
        child.parent = this;
    }
    
    public void removeChild(Entity child) {
        if (children.remove(child)) {
            child.parent = null;
        }
    }
    
    public Entity getParent() {
        return parent;
    }
    
    public List<Entity> getChildren() {
        return new ArrayList<>(children);
    }
    
    public boolean hasChildren() {
        return !children.isEmpty();
    }
    
    // ==================== TRANSFORM ====================
    
    public Vector2 getPosition() {
        return position;
    }
    
    public void setPosition(Vector2 position) {
        this.position = position;
    }
    
    public void setPosition(float x, float y) {
        this.position = new Vector2(x, y);
    }
    
    public void move(Vector2 delta) {
        this.position = position.add(delta);
    }
    
    public void move(float dx, float dy) {
        this.position = position.add(dx, dy);
    }
    
    public float getX() {
        return position.x;
    }
    
    public float getY() {
        return position.y;
    }
    
    public float getRotation() {
        return rotation;
    }
    
    public void setRotation(float rotation) {
        this.rotation = rotation;
    }
    
    public void rotate(float delta) {
        this.rotation += delta;
    }
    
    public Vector2 getScale() {
        return scale;
    }
    
    public void setScale(Vector2 scale) {
        this.scale = scale;
    }
    
    public void setScale(float uniformScale) {
        this.scale = new Vector2(uniformScale, uniformScale);
    }
    
    /**
     * Posição global considerando hierarquia de pais.
     */
    public Vector2 getGlobalPosition() {
        if (parent == null) {
            return position;
        }
        return parent.getGlobalPosition().add(position);
    }
    
    // ==================== ESTADO ====================
    
    public boolean isActive() {
        return active && !destroyed;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public boolean isDestroyed() {
        return destroyed;
    }
    
    // ==================== IDENTIFICAÇÃO ====================
    
    public long getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getTag() {
        return tag;
    }
    
    public void setTag(String tag) {
        this.tag = tag;
    }
    
    // ==================== CENA ====================
    
    public Scene getScene() {
        return scene;
    }
    
    public void setScene(Scene scene) {
        this.scene = scene;
    }
    
    // ==================== OBJECT ====================
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Entity)) return false;
        return id == ((Entity) obj).id;
    }
    
    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
    
    @Override
    public String toString() {
        return String.format("Entity[%d:'%s']", id, name);
    }
}

