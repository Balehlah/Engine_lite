package game.test;

import engine.core.Entity;
import engine.core.Scene;
import engine.graphics.Camera;
import engine.graphics.ColorPalette;
import engine.graphics.Renderer;
import engine.graphics.Sprite;
import engine.assets.TextureLoader;
import engine.input.Input;
import engine.input.Keyboard;
import engine.math.Vector2;
import engine.util.Logger;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Cena de teste demonstrando funcionalidades da engine.
 */
public class TestScene extends Scene {
    
    // Entidade do jogador
    private PlayerEntity player;
    
    // Câmera
    private Camera camera;
    
    // Assets procedurais (para teste sem arquivos externos)
    private Sprite playerSprite;
    private Sprite groundSprite;
    
    // Background
    private Color backgroundColor;
    
    public TestScene() {
        super("TestScene");
    }
    
    @Override
    public void onCreate() {
        Logger.info("Criando cena de teste...");
        
        // Define cor de fundo (estilo PICO-8)
        backgroundColor = ColorPalette.PICO8_DARK_BLUE;
        
        // Cria sprites procedurais
        createSprites();
        
        // Cria câmera
        camera = new Camera(800, 600);
        setCamera(camera);
        
        // Cria jogador
        player = new PlayerEntity(playerSprite);
        player.setPosition(400, 300);
        addEntity(player);
        
        // Cria algumas entidades de cenário
        for (int i = 0; i < 10; i++) {
            Entity ground = new GroundEntity(groundSprite);
            ground.setPosition(i * 64, 500);
            addEntity(ground);
        }
        
        Logger.info("Cena criada com %d entidades", getEntityCount());
    }
    
    private void createSprites() {
        // Sprite do jogador (quadrado colorido simples)
        BufferedImage playerImg = TextureLoader.create(32, 32);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                // Borda
                if (x == 0 || x == 31 || y == 0 || y == 31) {
                    playerImg.setRGB(x, y, ColorPalette.PICO8_DARK_PURPLE.getRGB());
                } 
                // Interior
                else {
                    playerImg.setRGB(x, y, ColorPalette.PICO8_PINK.getRGB());
                }
            }
        }
        // Olhos
        playerImg.setRGB(10, 12, ColorPalette.PICO8_WHITE.getRGB());
        playerImg.setRGB(21, 12, ColorPalette.PICO8_WHITE.getRGB());
        playerImg.setRGB(10, 13, ColorPalette.PICO8_BLACK.getRGB());
        playerImg.setRGB(21, 13, ColorPalette.PICO8_BLACK.getRGB());
        
        playerSprite = new Sprite(playerImg);
        playerSprite.setOriginCenter();
        
        // Sprite do chão
        BufferedImage groundImg = TextureLoader.create(64, 32);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 64; x++) {
                if (y < 4) {
                    groundImg.setRGB(x, y, ColorPalette.PICO8_GREEN.getRGB());
                } else {
                    groundImg.setRGB(x, y, ColorPalette.PICO8_BROWN.getRGB());
                }
            }
        }
        groundSprite = new Sprite(groundImg);
        groundSprite.setOrigin(0, 0);
    }
    
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        
        // Câmera segue o jogador suavemente
        camera.follow(player.getPosition());
        
        // ESC para sair
        if (Input.getInstance().isKeyPressed(Keyboard.KEY_ESCAPE)) {
            Logger.info("ESC pressionado - encerrando...");
            System.exit(0);
        }
    }
    
    @Override
    public void render(Renderer renderer) {
        // Limpa com cor de fundo
        renderer.clear(backgroundColor);
        
        // Renderiza entidades
        super.render(renderer);
        
        // HUD (ignora câmera)
        renderer.clearCamera();
        
        // Instruções
        renderer.drawText("WASD/Setas: Mover | SHIFT: Correr | Mouse: Olhar", 10, 20, ColorPalette.PICO8_WHITE);
        renderer.drawText("ESC: Sair", 10, 40, ColorPalette.PICO8_LIGHT_GREY);
        
        // Posição do jogador
        String posText = String.format("Pos: %.0f, %.0f", player.getX(), player.getY());
        renderer.drawText(posText, 10, 580, ColorPalette.PICO8_YELLOW);
        
        // Posição do mouse
        Vector2 mousePos = Input.getInstance().getMousePosition();
        String mouseText = String.format("Mouse: %.0f, %.0f", mousePos.x, mousePos.y);
        renderer.drawText(mouseText, 200, 580, ColorPalette.PICO8_BLUE);
        
        // Restaura câmera
        renderer.setCamera(camera);
    }
    
    // ==================== ENTIDADES INTERNAS ====================
    
    /**
     * Entidade do jogador com movimento.
     */
    private static class PlayerEntity extends Entity {
        
        private final Sprite sprite;
        private float speed = 200f;
        private float runMultiplier = 1.8f;
        
        public PlayerEntity(Sprite sprite) {
            super("Player");
            this.sprite = sprite;
        }
        
        @Override
        public void update(float deltaTime) {
            super.update(deltaTime);
            
            Input input = Input.getInstance();
            
            // Direção de movimento
            float dx = input.getHorizontalAxis();
            float dy = input.getVerticalAxis();
            
            // Normaliza diagonal
            if (dx != 0 && dy != 0) {
                float len = (float) Math.sqrt(dx * dx + dy * dy);
                dx /= len;
                dy /= len;
            }
            
            // Velocidade (shift = correr)
            float currentSpeed = speed;
            if (input.isKeyDown(Keyboard.KEY_SHIFT)) {
                currentSpeed *= runMultiplier;
            }
            
            // Aplica movimento
            move(dx * currentSpeed * deltaTime, dy * currentSpeed * deltaTime);
            
            // Flip baseado na direção
            if (dx < 0) {
                sprite.setFlipX(true);
            } else if (dx > 0) {
                sprite.setFlipX(false);
            }
        }
        
        @Override
        public void render(Renderer renderer) {
            super.render(renderer);
            renderer.drawSprite(sprite, getPosition());
        }
    }
    
    /**
     * Entidade de chão estática.
     */
    private static class GroundEntity extends Entity {
        
        private final Sprite sprite;
        
        public GroundEntity(Sprite sprite) {
            super("Ground");
            this.sprite = sprite;
        }
        
        @Override
        public void render(Renderer renderer) {
            super.render(renderer);
            renderer.drawSprite(sprite, getPosition());
        }
    }
}

