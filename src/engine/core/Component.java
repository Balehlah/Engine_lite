package engine.core;

/**
 * Base para componentes do sistema ECS.
 * Componentes são dados puros associados a entidades.
 * 
 * Design: Componentes devem ser leves e focados em um único aspecto.
 * Lógica complexa vai em Systems, não em Components.
 */
public abstract class Component {
    
    // Referência à entidade dona (set automaticamente)
    private Entity entity;
    
    // Estado do componente
    private boolean enabled = true;
    
    /**
     * Chamado quando o componente é adicionado a uma entidade.
     * Override para inicialização.
     */
    public void onAttach() {}
    
    /**
     * Chamado quando o componente é removido da entidade.
     * Override para cleanup.
     */
    public void onDetach() {}
    
    /**
     * Chamado quando o componente é habilitado.
     */
    public void onEnable() {}
    
    /**
     * Chamado quando o componente é desabilitado.
     */
    public void onDisable() {}
    
    // ==================== GETTERS/SETTERS ====================
    
    public Entity getEntity() {
        return entity;
    }
    
    /**
     * Uso interno - chamado por Entity.addComponent()
     */
    public void setEntity(Entity entity) {
        this.entity = entity;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (enabled) {
                onEnable();
            } else {
                onDisable();
            }
        }
    }
    
    /**
     * Atalho para acessar outras componentes na mesma entidade.
     */
    protected <T extends Component> T getComponent(Class<T> type) {
        if (entity != null) {
            return entity.getComponent(type);
        }
        return null;
    }
    
    /**
     * Verifica se a entidade tem um componente específico.
     */
    protected boolean hasComponent(Class<? extends Component> type) {
        return entity != null && entity.hasComponent(type);
    }
}

