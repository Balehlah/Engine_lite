package engine.util;

import engine.math.Vector2;
import java.util.List;
import java.util.Random;

/**
 * Utilitários de aleatoriedade para jogos.
 * Singleton com seed configurável para reprodutibilidade.
 */
public final class RandomUtils {
    
    private static final Random random = new Random();
    
    private RandomUtils() {}
    
    // ==================== SEED ====================
    
    public static void setSeed(long seed) {
        random.setSeed(seed);
    }
    
    // ==================== INTEIROS ====================
    
    /**
     * Retorna int aleatório em [0, bound)
     */
    public static int nextInt(int bound) {
        return random.nextInt(bound);
    }
    
    /**
     * Retorna int aleatório em [min, max]
     */
    public static int range(int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("min > max: " + min + " > " + max);
        }
        return min + random.nextInt(max - min + 1);
    }
    
    // ==================== FLOATS ====================
    
    /**
     * Retorna float aleatório em [0, 1)
     */
    public static float nextFloat() {
        return random.nextFloat();
    }
    
    /**
     * Retorna float aleatório em [min, max)
     */
    public static float range(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }
    
    // ==================== BOOLEANOS ====================
    
    public static boolean nextBoolean() {
        return random.nextBoolean();
    }
    
    /**
     * Retorna true com a probabilidade especificada (0.0 a 1.0)
     */
    public static boolean chance(float probability) {
        return random.nextFloat() < probability;
    }
    
    /**
     * Retorna true com chance de 1 em n
     */
    public static boolean oneIn(int n) {
        return random.nextInt(n) == 0;
    }
    
    // ==================== VETORES ====================
    
    /**
     * Retorna vetor com componentes aleatórios em [0, 1)
     */
    public static Vector2 nextVector2() {
        return new Vector2(random.nextFloat(), random.nextFloat());
    }
    
    /**
     * Retorna vetor dentro de um retângulo
     */
    public static Vector2 inRect(float x, float y, float width, float height) {
        return new Vector2(
            x + random.nextFloat() * width,
            y + random.nextFloat() * height
        );
    }
    
    /**
     * Retorna vetor unitário em direção aleatória
     */
    public static Vector2 direction() {
        float angle = random.nextFloat() * (float) (Math.PI * 2);
        return new Vector2((float) Math.cos(angle), (float) Math.sin(angle));
    }
    
    /**
     * Retorna vetor dentro de um círculo
     */
    public static Vector2 inCircle(float radius) {
        float r = (float) Math.sqrt(random.nextFloat()) * radius;
        float angle = random.nextFloat() * (float) (Math.PI * 2);
        return new Vector2(r * (float) Math.cos(angle), r * (float) Math.sin(angle));
    }
    
    /**
     * Retorna vetor na borda de um círculo
     */
    public static Vector2 onCircle(float radius) {
        float angle = random.nextFloat() * (float) (Math.PI * 2);
        return new Vector2(radius * (float) Math.cos(angle), radius * (float) Math.sin(angle));
    }
    
    // ==================== COLEÇÕES ====================
    
    /**
     * Retorna elemento aleatório de um array
     */
    public static <T> T pick(T[] array) {
        if (array == null || array.length == 0) {
            return null;
        }
        return array[random.nextInt(array.length)];
    }
    
    /**
     * Retorna elemento aleatório de uma lista
     */
    public static <T> T pick(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(random.nextInt(list.size()));
    }
    
    /**
     * Retorna índice aleatório com pesos (weighted random)
     */
    public static int weightedIndex(float[] weights) {
        float total = 0;
        for (float w : weights) {
            total += w;
        }
        
        float roll = random.nextFloat() * total;
        float cumulative = 0;
        
        for (int i = 0; i < weights.length; i++) {
            cumulative += weights[i];
            if (roll < cumulative) {
                return i;
            }
        }
        
        return weights.length - 1;
    }
    
    // ==================== EFEITOS ====================
    
    /**
     * Variação aleatória em torno de um valor base
     */
    public static float vary(float base, float variance) {
        return base + range(-variance, variance);
    }
    
    /**
     * Gaussian (normal distribution) centrado em 0
     */
    public static float gaussian() {
        return (float) random.nextGaussian();
    }
    
    /**
     * Gaussian com média e desvio padrão customizados
     */
    public static float gaussian(float mean, float stdDev) {
        return mean + (float) random.nextGaussian() * stdDev;
    }
    
    /**
     * Sinal aleatório (-1 ou 1)
     */
    public static int sign() {
        return random.nextBoolean() ? 1 : -1;
    }
}

