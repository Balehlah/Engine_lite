package engine.assets;

import engine.util.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Carregador de texturas (imagens).
 * Suporta carregamento de arquivo, classpath e criação procedural.
 */
public final class TextureLoader {
    
    private TextureLoader() {}
    
    // ==================== CARREGAMENTO DE ARQUIVO ====================
    
    /**
     * Carrega imagem de um arquivo.
     */
    public static BufferedImage load(String path) {
        try {
            // Tenta carregar do sistema de arquivos
            File file = new File(path);
            if (file.exists()) {
                BufferedImage image = ImageIO.read(file);
                Logger.debug("Textura carregada: %s (%dx%d)", path, image.getWidth(), image.getHeight());
                return image;
            }
            
            // Tenta carregar do classpath
            InputStream stream = TextureLoader.class.getResourceAsStream("/" + path);
            if (stream != null) {
                BufferedImage image = ImageIO.read(stream);
                stream.close();
                Logger.debug("Textura carregada (classpath): %s (%dx%d)", path, image.getWidth(), image.getHeight());
                return image;
            }
            
            // Tenta sem a barra inicial
            stream = TextureLoader.class.getResourceAsStream(path);
            if (stream != null) {
                BufferedImage image = ImageIO.read(stream);
                stream.close();
                Logger.debug("Textura carregada (classpath): %s (%dx%d)", path, image.getWidth(), image.getHeight());
                return image;
            }
            
            Logger.error("Textura não encontrada: %s", path);
            return createMissingTexture();
            
        } catch (IOException e) {
            Logger.error("Erro ao carregar textura: " + path, e);
            return createMissingTexture();
        }
    }
    
    /**
     * Carrega imagem de um InputStream.
     */
    public static BufferedImage load(InputStream stream) {
        try {
            return ImageIO.read(stream);
        } catch (IOException e) {
            Logger.error("Erro ao carregar textura de stream", e);
            return createMissingTexture();
        }
    }
    
    // ==================== CRIAÇÃO PROCEDURAL ====================
    
    /**
     * Cria textura vazia.
     */
    public static BufferedImage create(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }
    
    /**
     * Cria textura preenchida com uma cor.
     */
    public static BufferedImage create(int width, int height, int color) {
        BufferedImage image = create(width, height);
        int[] pixels = new int[width * height];
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = color;
        }
        image.setRGB(0, 0, width, height, pixels, 0, width);
        return image;
    }
    
    /**
     * Cria textura de "missing texture" (padrão xadrez magenta/preto).
     */
    public static BufferedImage createMissingTexture() {
        return createMissingTexture(32, 32);
    }
    
    public static BufferedImage createMissingTexture(int width, int height) {
        BufferedImage image = create(width, height);
        int cellSize = 8;
        int magenta = 0xFFFF00FF;
        int black = 0xFF000000;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean isEven = ((x / cellSize) + (y / cellSize)) % 2 == 0;
                image.setRGB(x, y, isEven ? magenta : black);
            }
        }
        
        return image;
    }
    
    /**
     * Cria textura de cor sólida.
     */
    public static BufferedImage createSolid(int width, int height, java.awt.Color color) {
        return create(width, height, color.getRGB());
    }
    
    /**
     * Cria textura de gradiente vertical.
     */
    public static BufferedImage createGradient(int width, int height, 
                                                java.awt.Color top, java.awt.Color bottom) {
        BufferedImage image = create(width, height);
        
        for (int y = 0; y < height; y++) {
            float t = (float) y / height;
            int r = (int) (top.getRed() + (bottom.getRed() - top.getRed()) * t);
            int g = (int) (top.getGreen() + (bottom.getGreen() - top.getGreen()) * t);
            int b = (int) (top.getBlue() + (bottom.getBlue() - top.getBlue()) * t);
            int color = 0xFF000000 | (r << 16) | (g << 8) | b;
            
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, color);
            }
        }
        
        return image;
    }
    
    /**
     * Cria textura de ruído.
     */
    public static BufferedImage createNoise(int width, int height) {
        BufferedImage image = create(width, height);
        java.util.Random random = new java.util.Random();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int gray = random.nextInt(256);
                int color = 0xFF000000 | (gray << 16) | (gray << 8) | gray;
                image.setRGB(x, y, color);
            }
        }
        
        return image;
    }
    
    /**
     * Cria textura de grid/grade.
     */
    public static BufferedImage createGrid(int width, int height, int cellSize,
                                           java.awt.Color lineColor, java.awt.Color bgColor) {
        BufferedImage image = createSolid(width, height, bgColor);
        int line = lineColor.getRGB();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x % cellSize == 0 || y % cellSize == 0) {
                    image.setRGB(x, y, line);
                }
            }
        }
        
        return image;
    }
    
    // ==================== MANIPULAÇÃO ====================
    
    /**
     * Redimensiona uma imagem.
     */
    public static BufferedImage resize(BufferedImage source, int newWidth, int newHeight) {
        BufferedImage result = create(newWidth, newHeight);
        java.awt.Graphics2D g = result.createGraphics();
        g.drawImage(source, 0, 0, newWidth, newHeight, null);
        g.dispose();
        return result;
    }
    
    /**
     * Escala uma imagem por um fator.
     */
    public static BufferedImage scale(BufferedImage source, float factor) {
        int newWidth = (int) (source.getWidth() * factor);
        int newHeight = (int) (source.getHeight() * factor);
        return resize(source, newWidth, newHeight);
    }
    
    /**
     * Extrai sub-região de uma imagem.
     */
    public static BufferedImage getSubImage(BufferedImage source, int x, int y, int width, int height) {
        return source.getSubimage(x, y, width, height);
    }
    
    /**
     * Copia uma imagem.
     */
    public static BufferedImage copy(BufferedImage source) {
        BufferedImage copy = create(source.getWidth(), source.getHeight());
        java.awt.Graphics2D g = copy.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return copy;
    }
    
    /**
     * Flip horizontal.
     */
    public static BufferedImage flipHorizontal(BufferedImage source) {
        int w = source.getWidth();
        int h = source.getHeight();
        BufferedImage result = create(w, h);
        java.awt.Graphics2D g = result.createGraphics();
        g.drawImage(source, w, 0, -w, h, null);
        g.dispose();
        return result;
    }
    
    /**
     * Flip vertical.
     */
    public static BufferedImage flipVertical(BufferedImage source) {
        int w = source.getWidth();
        int h = source.getHeight();
        BufferedImage result = create(w, h);
        java.awt.Graphics2D g = result.createGraphics();
        g.drawImage(source, 0, h, w, -h, null);
        g.dispose();
        return result;
    }
    
    // ==================== SALVAMENTO ====================
    
    /**
     * Salva imagem como PNG.
     */
    public static boolean savePNG(BufferedImage image, String path) {
        try {
            ImageIO.write(image, "PNG", new File(path));
            Logger.debug("Textura salva: %s", path);
            return true;
        } catch (IOException e) {
            Logger.error("Erro ao salvar textura: " + path, e);
            return false;
        }
    }
}

