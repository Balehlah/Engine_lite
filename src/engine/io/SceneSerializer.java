package engine.io;

import engine.core.Entity;
import engine.core.Scene;
import engine.math.Vector2;
import engine.util.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serializador de cenas e entidades.
 * Formato simples baseado em texto para facilitar debug.
 */
public class SceneSerializer {
    
    private static final String VERSION = "1.0";
    private static final String HEADER = "PIXEL_ENGINE_SCENE";
    
    // ==================== SALVAR ====================
    
    /**
     * Salva cena em arquivo.
     */
    public static boolean save(Scene scene, String path) {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(path), StandardCharsets.UTF_8))) {
            
            // Header
            writer.println(HEADER);
            writer.println("version=" + VERSION);
            writer.println("name=" + scene.getName());
            writer.println();
            
            // Entidades
            writer.println("[entities]");
            for (Entity entity : scene.getEntities()) {
                writeEntity(writer, entity);
            }
            
            Logger.info("Cena salva: %s (%d entidades)", path, scene.getEntityCount());
            return true;
            
        } catch (IOException e) {
            Logger.error("Erro ao salvar cena: " + path, e);
            return false;
        }
    }
    
    private static void writeEntity(PrintWriter writer, Entity entity) {
        writer.println("entity {");
        writer.println("  name=" + entity.getName());
        writer.println("  tag=" + entity.getTag());
        writer.println("  x=" + entity.getX());
        writer.println("  y=" + entity.getY());
        writer.println("  rotation=" + entity.getRotation());
        writer.println("  scaleX=" + entity.getScale().x);
        writer.println("  scaleY=" + entity.getScale().y);
        writer.println("  active=" + entity.isActive());
        
        // TODO: Serializar componentes customizados
        
        writer.println("}");
        writer.println();
    }
    
    // ==================== CARREGAR ====================
    
    /**
     * Carrega dados de cena de arquivo.
     * Retorna lista de dados de entidades para criar manualmente.
     */
    public static SceneData load(String path) {
        SceneData data = new SceneData();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(path), StandardCharsets.UTF_8))) {
            
            String line;
            String currentSection = "";
            EntityData currentEntity = null;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // Ignora linhas vazias e comentarios
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                // Header
                if (line.equals(HEADER)) {
                    continue;
                }
                
                // Propriedades do header
                if (line.startsWith("version=")) {
                    data.version = line.substring(8);
                } else if (line.startsWith("name=")) {
                    data.name = line.substring(5);
                }
                
                // Secoes
                else if (line.equals("[entities]")) {
                    currentSection = "entities";
                }
                
                // Inicio de entidade
                else if (line.equals("entity {")) {
                    currentEntity = new EntityData();
                }
                
                // Fim de entidade
                else if (line.equals("}") && currentEntity != null) {
                    data.entities.add(currentEntity);
                    currentEntity = null;
                }
                
                // Propriedades de entidade
                else if (currentEntity != null) {
                    parseEntityProperty(currentEntity, line);
                }
            }
            
            Logger.info("Cena carregada: %s (%d entidades)", path, data.entities.size());
            
        } catch (IOException e) {
            Logger.error("Erro ao carregar cena: " + path, e);
        }
        
        return data;
    }
    
    private static void parseEntityProperty(EntityData entity, String line) {
        int eq = line.indexOf('=');
        if (eq < 0) return;
        
        String key = line.substring(0, eq).trim();
        String value = line.substring(eq + 1).trim();
        
        switch (key) {
            case "name":
                entity.name = value;
                break;
            case "tag":
                entity.tag = value;
                break;
            case "x":
                entity.x = Float.parseFloat(value);
                break;
            case "y":
                entity.y = Float.parseFloat(value);
                break;
            case "rotation":
                entity.rotation = Float.parseFloat(value);
                break;
            case "scaleX":
                entity.scaleX = Float.parseFloat(value);
                break;
            case "scaleY":
                entity.scaleY = Float.parseFloat(value);
                break;
            case "active":
                entity.active = Boolean.parseBoolean(value);
                break;
            default:
                entity.customProperties.put(key, value);
                break;
        }
    }
    
    // ==================== DADOS ====================
    
    /**
     * Dados de uma cena carregada.
     */
    public static class SceneData {
        public String version = "";
        public String name = "";
        public List<EntityData> entities = new ArrayList<>();
        
        /**
         * Cria entidades basicas a partir dos dados.
         */
        public List<Entity> createEntities() {
            List<Entity> result = new ArrayList<>();
            for (EntityData data : entities) {
                result.add(data.createEntity());
            }
            return result;
        }
    }
    
    /**
     * Dados de uma entidade carregada.
     */
    public static class EntityData {
        public String name = "Entity";
        public String tag = "";
        public float x = 0;
        public float y = 0;
        public float rotation = 0;
        public float scaleX = 1;
        public float scaleY = 1;
        public boolean active = true;
        public Map<String, String> customProperties = new HashMap<>();
        
        /**
         * Cria entidade basica a partir dos dados.
         */
        public Entity createEntity() {
            Entity entity = new Entity(name);
            entity.setTag(tag);
            entity.setPosition(x, y);
            entity.setRotation(rotation);
            entity.setScale(new Vector2(scaleX, scaleY));
            entity.setActive(active);
            return entity;
        }
        
        /**
         * Retorna propriedade customizada como String.
         */
        public String getString(String key, String defaultValue) {
            return customProperties.getOrDefault(key, defaultValue);
        }
        
        /**
         * Retorna propriedade customizada como int.
         */
        public int getInt(String key, int defaultValue) {
            String value = customProperties.get(key);
            if (value == null) return defaultValue;
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        
        /**
         * Retorna propriedade customizada como float.
         */
        public float getFloat(String key, float defaultValue) {
            String value = customProperties.get(key);
            if (value == null) return defaultValue;
            try {
                return Float.parseFloat(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        
        /**
         * Retorna propriedade customizada como boolean.
         */
        public boolean getBoolean(String key, boolean defaultValue) {
            String value = customProperties.get(key);
            if (value == null) return defaultValue;
            return Boolean.parseBoolean(value);
        }
    }
    
    // ==================== TILEMAP ====================
    
    /**
     * Salva dados de tilemap em arquivo.
     */
    public static boolean saveTilemap(int[] data, int width, int height, String path) {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(path), StandardCharsets.UTF_8))) {
            
            writer.println("PIXEL_ENGINE_TILEMAP");
            writer.println("version=" + VERSION);
            writer.println("width=" + width);
            writer.println("height=" + height);
            writer.println();
            writer.println("[data]");
            
            for (int y = 0; y < height; y++) {
                StringBuilder sb = new StringBuilder();
                for (int x = 0; x < width; x++) {
                    if (x > 0) sb.append(",");
                    sb.append(data[y * width + x]);
                }
                writer.println(sb.toString());
            }
            
            Logger.info("Tilemap salvo: %s (%dx%d)", path, width, height);
            return true;
            
        } catch (IOException e) {
            Logger.error("Erro ao salvar tilemap: " + path, e);
            return false;
        }
    }
    
    /**
     * Carrega dados de tilemap de arquivo.
     */
    public static TilemapData loadTilemap(String path) {
        TilemapData data = new TilemapData();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(path), StandardCharsets.UTF_8))) {
            
            String line;
            boolean inData = false;
            List<int[]> rows = new ArrayList<>();
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                if (line.isEmpty() || line.startsWith("#")) continue;
                
                if (line.startsWith("width=")) {
                    data.width = Integer.parseInt(line.substring(6));
                } else if (line.startsWith("height=")) {
                    data.height = Integer.parseInt(line.substring(7));
                } else if (line.equals("[data]")) {
                    inData = true;
                } else if (inData) {
                    String[] parts = line.split(",");
                    int[] row = new int[parts.length];
                    for (int i = 0; i < parts.length; i++) {
                        row[i] = Integer.parseInt(parts[i].trim());
                    }
                    rows.add(row);
                }
            }
            
            // Converte para array 1D
            if (!rows.isEmpty()) {
                data.width = rows.get(0).length;
                data.height = rows.size();
                data.tiles = new int[data.width * data.height];
                
                for (int y = 0; y < rows.size(); y++) {
                    int[] row = rows.get(y);
                    for (int x = 0; x < row.length && x < data.width; x++) {
                        data.tiles[y * data.width + x] = row[x];
                    }
                }
            }
            
            Logger.info("Tilemap carregado: %s (%dx%d)", path, data.width, data.height);
            
        } catch (IOException e) {
            Logger.error("Erro ao carregar tilemap: " + path, e);
        }
        
        return data;
    }
    
    /**
     * Dados de tilemap carregado.
     */
    public static class TilemapData {
        public int width = 0;
        public int height = 0;
        public int[] tiles = new int[0];
    }
}

