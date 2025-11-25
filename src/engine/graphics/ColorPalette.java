package engine.graphics;

import java.awt.Color;

/**
 * Paletas de cores para pixel art.
 * Contém paletas clássicas e utilitários para manipulação de cores.
 */
public final class ColorPalette {
    
    private ColorPalette() {}
    
    // ==================== CORES BÁSICAS ====================
    
    public static final Color BLACK = new Color(0x000000);
    public static final Color WHITE = new Color(0xFFFFFF);
    public static final Color RED = new Color(0xFF0000);
    public static final Color GREEN = new Color(0x00FF00);
    public static final Color BLUE = new Color(0x0000FF);
    public static final Color YELLOW = new Color(0xFFFF00);
    public static final Color CYAN = new Color(0x00FFFF);
    public static final Color MAGENTA = new Color(0xFF00FF);
    
    // ==================== PALETA PICO-8 ====================
    
    public static final Color PICO8_BLACK = new Color(0x000000);
    public static final Color PICO8_DARK_BLUE = new Color(0x1D2B53);
    public static final Color PICO8_DARK_PURPLE = new Color(0x7E2553);
    public static final Color PICO8_DARK_GREEN = new Color(0x008751);
    public static final Color PICO8_BROWN = new Color(0xAB5236);
    public static final Color PICO8_DARK_GREY = new Color(0x5F574F);
    public static final Color PICO8_LIGHT_GREY = new Color(0xC2C3C7);
    public static final Color PICO8_WHITE = new Color(0xFFF1E8);
    public static final Color PICO8_RED = new Color(0xFF004D);
    public static final Color PICO8_ORANGE = new Color(0xFFA300);
    public static final Color PICO8_YELLOW = new Color(0xFFEC27);
    public static final Color PICO8_GREEN = new Color(0x00E436);
    public static final Color PICO8_BLUE = new Color(0x29ADFF);
    public static final Color PICO8_LAVENDER = new Color(0x83769C);
    public static final Color PICO8_PINK = new Color(0xFF77A8);
    public static final Color PICO8_LIGHT_PEACH = new Color(0xFFCCAA);
    
    public static final Color[] PICO8 = {
        PICO8_BLACK, PICO8_DARK_BLUE, PICO8_DARK_PURPLE, PICO8_DARK_GREEN,
        PICO8_BROWN, PICO8_DARK_GREY, PICO8_LIGHT_GREY, PICO8_WHITE,
        PICO8_RED, PICO8_ORANGE, PICO8_YELLOW, PICO8_GREEN,
        PICO8_BLUE, PICO8_LAVENDER, PICO8_PINK, PICO8_LIGHT_PEACH
    };
    
    // ==================== PALETA GAMEBOY ====================
    
    public static final Color GB_DARKEST = new Color(0x0F380F);
    public static final Color GB_DARK = new Color(0x306230);
    public static final Color GB_LIGHT = new Color(0x8BAC0F);
    public static final Color GB_LIGHTEST = new Color(0x9BBC0F);
    
    public static final Color[] GAMEBOY = {
        GB_DARKEST, GB_DARK, GB_LIGHT, GB_LIGHTEST
    };
    
    // ==================== PALETA NES ====================
    
    public static final Color NES_BLACK = new Color(0x000000);
    public static final Color NES_DARK_GRAY = new Color(0x626262);
    public static final Color NES_LIGHT_GRAY = new Color(0xABABAB);
    public static final Color NES_WHITE = new Color(0xFFFFFF);
    public static final Color NES_RED = new Color(0xB82424);
    public static final Color NES_ORANGE = new Color(0xE06000);
    public static final Color NES_GOLD = new Color(0xD8A800);
    public static final Color NES_GREEN = new Color(0x008A00);
    public static final Color NES_CYAN = new Color(0x005878);
    public static final Color NES_BLUE = new Color(0x0040A8);
    public static final Color NES_PURPLE = new Color(0x5C00A8);
    public static final Color NES_PINK = new Color(0x8A007C);
    
    public static final Color[] NES = {
        NES_BLACK, NES_DARK_GRAY, NES_LIGHT_GRAY, NES_WHITE,
        NES_RED, NES_ORANGE, NES_GOLD, NES_GREEN,
        NES_CYAN, NES_BLUE, NES_PURPLE, NES_PINK
    };
    
    // ==================== PALETA CGA ====================
    
    public static final Color CGA_BLACK = new Color(0x000000);
    public static final Color CGA_BLUE = new Color(0x0000AA);
    public static final Color CGA_GREEN = new Color(0x00AA00);
    public static final Color CGA_CYAN = new Color(0x00AAAA);
    public static final Color CGA_RED = new Color(0xAA0000);
    public static final Color CGA_MAGENTA = new Color(0xAA00AA);
    public static final Color CGA_BROWN = new Color(0xAA5500);
    public static final Color CGA_LIGHT_GRAY = new Color(0xAAAAAA);
    public static final Color CGA_DARK_GRAY = new Color(0x555555);
    public static final Color CGA_LIGHT_BLUE = new Color(0x5555FF);
    public static final Color CGA_LIGHT_GREEN = new Color(0x55FF55);
    public static final Color CGA_LIGHT_CYAN = new Color(0x55FFFF);
    public static final Color CGA_LIGHT_RED = new Color(0xFF5555);
    public static final Color CGA_LIGHT_MAGENTA = new Color(0xFF55FF);
    public static final Color CGA_YELLOW = new Color(0xFFFF55);
    public static final Color CGA_WHITE = new Color(0xFFFFFF);
    
    public static final Color[] CGA = {
        CGA_BLACK, CGA_BLUE, CGA_GREEN, CGA_CYAN,
        CGA_RED, CGA_MAGENTA, CGA_BROWN, CGA_LIGHT_GRAY,
        CGA_DARK_GRAY, CGA_LIGHT_BLUE, CGA_LIGHT_GREEN, CGA_LIGHT_CYAN,
        CGA_LIGHT_RED, CGA_LIGHT_MAGENTA, CGA_YELLOW, CGA_WHITE
    };
    
    // ==================== UTILITÁRIOS ====================
    
    /**
     * Cria cor a partir de hex string (ex: "#FF0000" ou "FF0000")
     */
    public static Color fromHex(String hex) {
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        return new Color(Integer.parseInt(hex, 16));
    }
    
    /**
     * Cria cor a partir de RGB inteiro (0xRRGGBB)
     */
    public static Color fromRGB(int rgb) {
        return new Color(rgb);
    }
    
    /**
     * Cria cor a partir de RGBA inteiro (0xRRGGBBAA)
     */
    public static Color fromRGBA(int rgba) {
        return new Color(
            (rgba >> 24) & 0xFF,
            (rgba >> 16) & 0xFF,
            (rgba >> 8) & 0xFF,
            rgba & 0xFF
        );
    }
    
    /**
     * Interpola entre duas cores.
     */
    public static Color lerp(Color a, Color b, float t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
            (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
            (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t),
            (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t)
        );
    }
    
    /**
     * Escurece uma cor.
     */
    public static Color darken(Color color, float amount) {
        return lerp(color, BLACK, amount);
    }
    
    /**
     * Clareia uma cor.
     */
    public static Color lighten(Color color, float amount) {
        return lerp(color, WHITE, amount);
    }
    
    /**
     * Retorna cor com alpha modificado.
     */
    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
    
    /**
     * Retorna cor com alpha modificado (0.0 - 1.0).
     */
    public static Color withAlpha(Color color, float alpha) {
        return withAlpha(color, (int) (alpha * 255));
    }
    
    /**
     * Inverte uma cor.
     */
    public static Color invert(Color color) {
        return new Color(
            255 - color.getRed(),
            255 - color.getGreen(),
            255 - color.getBlue(),
            color.getAlpha()
        );
    }
    
    /**
     * Converte para escala de cinza.
     */
    public static Color toGrayscale(Color color) {
        int gray = (int) (color.getRed() * 0.299 + color.getGreen() * 0.587 + color.getBlue() * 0.114);
        return new Color(gray, gray, gray, color.getAlpha());
    }
    
    /**
     * Encontra a cor mais próxima em uma paleta.
     */
    public static Color findClosest(Color target, Color[] palette) {
        Color closest = palette[0];
        int minDistance = Integer.MAX_VALUE;
        
        for (Color color : palette) {
            int dr = target.getRed() - color.getRed();
            int dg = target.getGreen() - color.getGreen();
            int db = target.getBlue() - color.getBlue();
            int distance = dr * dr + dg * dg + db * db;
            
            if (distance < minDistance) {
                minDistance = distance;
                closest = color;
            }
        }
        
        return closest;
    }
    
    /**
     * Gera cor aleatória.
     */
    public static Color random() {
        return new Color(
            (int) (Math.random() * 256),
            (int) (Math.random() * 256),
            (int) (Math.random() * 256)
        );
    }
    
    /**
     * Gera cor aleatória de uma paleta.
     */
    public static Color randomFrom(Color[] palette) {
        return palette[(int) (Math.random() * palette.length)];
    }
}

