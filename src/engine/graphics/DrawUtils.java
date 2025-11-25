package engine.graphics;

import engine.math.Vector2;
import engine.math.Rectangle;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;

/**
 * Utilitários de desenho para primitivas geométricas.
 * Funções estáticas para uso direto com Graphics2D.
 */
public final class DrawUtils {
    
    private static final Stroke DEFAULT_STROKE = new BasicStroke(1);
    
    private DrawUtils() {}
    
    // ==================== CONFIGURAÇÃO ====================
    
    /**
     * Desabilita anti-aliasing para visual pixel-perfect.
     */
    public static void setPixelPerfect(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    }
    
    /**
     * Habilita anti-aliasing para visual suave.
     */
    public static void setSmooth(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }
    
    // ==================== RETÂNGULOS ====================
    
    public static void fillRect(Graphics2D g, int x, int y, int width, int height, Color color) {
        g.setColor(color);
        g.fillRect(x, y, width, height);
    }
    
    public static void fillRect(Graphics2D g, Rectangle rect, Color color) {
        fillRect(g, rect.intX(), rect.intY(), rect.intWidth(), rect.intHeight(), color);
    }
    
    public static void drawRect(Graphics2D g, int x, int y, int width, int height, Color color) {
        g.setColor(color);
        g.drawRect(x, y, width - 1, height - 1);
    }
    
    public static void drawRect(Graphics2D g, Rectangle rect, Color color) {
        drawRect(g, rect.intX(), rect.intY(), rect.intWidth(), rect.intHeight(), color);
    }
    
    public static void drawRect(Graphics2D g, int x, int y, int width, int height, Color color, int thickness) {
        g.setColor(color);
        g.setStroke(new BasicStroke(thickness));
        g.drawRect(x, y, width - 1, height - 1);
        g.setStroke(DEFAULT_STROKE);
    }
    
    // ==================== RETÂNGULOS ARREDONDADOS ====================
    
    public static void fillRoundRect(Graphics2D g, int x, int y, int width, int height, int arcSize, Color color) {
        g.setColor(color);
        g.fillRoundRect(x, y, width, height, arcSize, arcSize);
    }
    
    public static void drawRoundRect(Graphics2D g, int x, int y, int width, int height, int arcSize, Color color) {
        g.setColor(color);
        g.drawRoundRect(x, y, width - 1, height - 1, arcSize, arcSize);
    }
    
    // ==================== CÍRCULOS ====================
    
    public static void fillCircle(Graphics2D g, int centerX, int centerY, int radius, Color color) {
        g.setColor(color);
        g.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
    }
    
    public static void fillCircle(Graphics2D g, Vector2 center, int radius, Color color) {
        fillCircle(g, center.intX(), center.intY(), radius, color);
    }
    
    public static void drawCircle(Graphics2D g, int centerX, int centerY, int radius, Color color) {
        g.setColor(color);
        g.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
    }
    
    public static void drawCircle(Graphics2D g, Vector2 center, int radius, Color color) {
        drawCircle(g, center.intX(), center.intY(), radius, color);
    }
    
    public static void drawCircle(Graphics2D g, int centerX, int centerY, int radius, Color color, int thickness) {
        g.setColor(color);
        g.setStroke(new BasicStroke(thickness));
        g.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
        g.setStroke(DEFAULT_STROKE);
    }
    
    // ==================== ELIPSES ====================
    
    public static void fillEllipse(Graphics2D g, int centerX, int centerY, int radiusX, int radiusY, Color color) {
        g.setColor(color);
        g.fillOval(centerX - radiusX, centerY - radiusY, radiusX * 2, radiusY * 2);
    }
    
    public static void drawEllipse(Graphics2D g, int centerX, int centerY, int radiusX, int radiusY, Color color) {
        g.setColor(color);
        g.drawOval(centerX - radiusX, centerY - radiusY, radiusX * 2, radiusY * 2);
    }
    
    // ==================== LINHAS ====================
    
    public static void drawLine(Graphics2D g, int x1, int y1, int x2, int y2, Color color) {
        g.setColor(color);
        g.drawLine(x1, y1, x2, y2);
    }
    
    public static void drawLine(Graphics2D g, Vector2 start, Vector2 end, Color color) {
        drawLine(g, start.intX(), start.intY(), end.intX(), end.intY(), color);
    }
    
    public static void drawLine(Graphics2D g, int x1, int y1, int x2, int y2, Color color, int thickness) {
        g.setColor(color);
        g.setStroke(new BasicStroke(thickness));
        g.drawLine(x1, y1, x2, y2);
        g.setStroke(DEFAULT_STROKE);
    }
    
    // ==================== POLÍGONOS ====================
    
    public static void fillPolygon(Graphics2D g, int[] xPoints, int[] yPoints, Color color) {
        g.setColor(color);
        g.fillPolygon(xPoints, yPoints, xPoints.length);
    }
    
    public static void fillPolygon(Graphics2D g, Vector2[] points, Color color) {
        int[] xPoints = new int[points.length];
        int[] yPoints = new int[points.length];
        for (int i = 0; i < points.length; i++) {
            xPoints[i] = points[i].intX();
            yPoints[i] = points[i].intY();
        }
        fillPolygon(g, xPoints, yPoints, color);
    }
    
    public static void drawPolygon(Graphics2D g, int[] xPoints, int[] yPoints, Color color) {
        g.setColor(color);
        g.drawPolygon(xPoints, yPoints, xPoints.length);
    }
    
    public static void drawPolygon(Graphics2D g, Vector2[] points, Color color) {
        int[] xPoints = new int[points.length];
        int[] yPoints = new int[points.length];
        for (int i = 0; i < points.length; i++) {
            xPoints[i] = points[i].intX();
            yPoints[i] = points[i].intY();
        }
        drawPolygon(g, xPoints, yPoints, color);
    }
    
    // ==================== TRIÂNGULOS ====================
    
    public static void fillTriangle(Graphics2D g, int x1, int y1, int x2, int y2, int x3, int y3, Color color) {
        g.setColor(color);
        g.fillPolygon(new int[]{x1, x2, x3}, new int[]{y1, y2, y3}, 3);
    }
    
    public static void fillTriangle(Graphics2D g, Vector2 p1, Vector2 p2, Vector2 p3, Color color) {
        fillTriangle(g, p1.intX(), p1.intY(), p2.intX(), p2.intY(), p3.intX(), p3.intY(), color);
    }
    
    public static void drawTriangle(Graphics2D g, int x1, int y1, int x2, int y2, int x3, int y3, Color color) {
        g.setColor(color);
        g.drawPolygon(new int[]{x1, x2, x3}, new int[]{y1, y2, y3}, 3);
    }
    
    // ==================== PIXELS ====================
    
    public static void drawPixel(Graphics2D g, int x, int y, Color color) {
        g.setColor(color);
        g.fillRect(x, y, 1, 1);
    }
    
    public static void drawPixel(Graphics2D g, Vector2 pos, Color color) {
        drawPixel(g, pos.intX(), pos.intY(), color);
    }
    
    // ==================== TEXTO ====================
    
    public static void drawText(Graphics2D g, String text, int x, int y, Color color) {
        g.setColor(color);
        g.drawString(text, x, y);
    }
    
    public static void drawText(Graphics2D g, String text, int x, int y, Color color, Font font) {
        g.setFont(font);
        g.setColor(color);
        g.drawString(text, x, y);
    }
    
    public static void drawTextCentered(Graphics2D g, String text, int x, int y, Color color) {
        var metrics = g.getFontMetrics();
        int textWidth = metrics.stringWidth(text);
        int textHeight = metrics.getHeight();
        g.setColor(color);
        g.drawString(text, x - textWidth / 2, y + textHeight / 4);
    }
    
    public static void drawTextCentered(Graphics2D g, String text, int x, int y, Color color, Font font) {
        g.setFont(font);
        drawTextCentered(g, text, x, y, color);
    }
    
    // ==================== ARCOS ====================
    
    public static void fillArc(Graphics2D g, int x, int y, int width, int height, 
                               int startAngle, int arcAngle, Color color) {
        g.setColor(color);
        g.fillArc(x, y, width, height, startAngle, arcAngle);
    }
    
    public static void drawArc(Graphics2D g, int x, int y, int width, int height,
                               int startAngle, int arcAngle, Color color) {
        g.setColor(color);
        g.drawArc(x, y, width, height, startAngle, arcAngle);
    }
    
    // ==================== GRADIENTES ====================
    
    public static void fillGradientRect(Graphics2D g, int x, int y, int width, int height,
                                        Color colorTop, Color colorBottom) {
        for (int i = 0; i < height; i++) {
            float t = (float) i / height;
            Color color = ColorPalette.lerp(colorTop, colorBottom, t);
            g.setColor(color);
            g.drawLine(x, y + i, x + width, y + i);
        }
    }
    
    public static void fillGradientRectHorizontal(Graphics2D g, int x, int y, int width, int height,
                                                   Color colorLeft, Color colorRight) {
        for (int i = 0; i < width; i++) {
            float t = (float) i / width;
            Color color = ColorPalette.lerp(colorLeft, colorRight, t);
            g.setColor(color);
            g.drawLine(x + i, y, x + i, y + height);
        }
    }
    
    // ==================== GRIDS ====================
    
    public static void drawGrid(Graphics2D g, int x, int y, int width, int height, 
                                int cellSize, Color color) {
        g.setColor(color);
        
        // Linhas verticais
        for (int i = x; i <= x + width; i += cellSize) {
            g.drawLine(i, y, i, y + height);
        }
        
        // Linhas horizontais
        for (int i = y; i <= y + height; i += cellSize) {
            g.drawLine(x, i, x + width, i);
        }
    }
    
    // ==================== DITHERING (Pixel Art) ====================
    
    /**
     * Preenche retângulo com padrão de dithering checkerboard.
     */
    public static void fillDithered(Graphics2D g, int x, int y, int width, int height,
                                    Color color1, Color color2) {
        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                g.setColor((px + py) % 2 == 0 ? color1 : color2);
                g.fillRect(x + px, y + py, 1, 1);
            }
        }
    }
    
    /**
     * Desenha borda tracejada.
     */
    public static void drawDashedRect(Graphics2D g, int x, int y, int width, int height,
                                      Color color, int dashLength) {
        g.setColor(color);
        g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 
                                    10, new float[]{dashLength}, 0));
        g.drawRect(x, y, width - 1, height - 1);
        g.setStroke(DEFAULT_STROKE);
    }
}

