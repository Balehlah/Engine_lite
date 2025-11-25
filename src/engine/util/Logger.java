package engine.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Sistema de logging simples e eficiente.
 * Sem dependências externas, configurável por nível.
 */
public final class Logger {
    
    public enum Level {
        DEBUG(0, "DEBUG"),
        INFO(1, "INFO"),
        WARN(2, "WARN"),
        ERROR(3, "ERROR"),
        NONE(4, "NONE");
        
        final int priority;
        final String label;
        
        Level(int priority, String label) {
            this.priority = priority;
            this.label = label;
        }
    }
    
    private static Level currentLevel = Level.DEBUG;
    private static boolean showTimestamp = true;
    private static boolean showCaller = true;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    
    private Logger() {}
    
    // ==================== CONFIGURAÇÃO ====================
    
    public static void setLevel(Level level) {
        currentLevel = level;
    }
    
    public static Level getLevel() {
        return currentLevel;
    }
    
    public static void setShowTimestamp(boolean show) {
        showTimestamp = show;
    }
    
    public static void setShowCaller(boolean show) {
        showCaller = show;
    }
    
    // ==================== LOGGING ====================
    
    public static void debug(String message) {
        log(Level.DEBUG, message);
    }
    
    public static void debug(String format, Object... args) {
        log(Level.DEBUG, String.format(format, args));
    }
    
    public static void info(String message) {
        log(Level.INFO, message);
    }
    
    public static void info(String format, Object... args) {
        log(Level.INFO, String.format(format, args));
    }
    
    public static void warn(String message) {
        log(Level.WARN, message);
    }
    
    public static void warn(String format, Object... args) {
        log(Level.WARN, String.format(format, args));
    }
    
    public static void error(String message) {
        log(Level.ERROR, message);
    }
    
    public static void error(String format, Object... args) {
        log(Level.ERROR, String.format(format, args));
    }
    
    public static void error(String message, Throwable throwable) {
        log(Level.ERROR, message);
        if (throwable != null && currentLevel.priority <= Level.ERROR.priority) {
            throwable.printStackTrace(System.err);
        }
    }
    
    // ==================== CORE ====================
    
    private static void log(Level level, String message) {
        if (level.priority < currentLevel.priority) {
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        
        // Timestamp
        if (showTimestamp) {
            sb.append("[").append(LocalDateTime.now().format(TIME_FORMAT)).append("] ");
        }
        
        // Level
        sb.append("[").append(level.label).append("] ");
        
        // Caller
        if (showCaller) {
            String caller = getCallerInfo();
            if (caller != null) {
                sb.append("[").append(caller).append("] ");
            }
        }
        
        // Message
        sb.append(message);
        
        // Output
        if (level.priority >= Level.WARN.priority) {
            System.err.println(sb);
        } else {
            System.out.println(sb);
        }
    }
    
    private static String getCallerInfo() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        // [0] = getStackTrace, [1] = getCallerInfo, [2] = log, [3] = debug/info/etc, [4] = caller
        if (stack.length >= 5) {
            StackTraceElement caller = stack[4];
            String className = caller.getClassName();
            int lastDot = className.lastIndexOf('.');
            if (lastDot != -1) {
                className = className.substring(lastDot + 1);
            }
            return className + ":" + caller.getLineNumber();
        }
        return null;
    }
    
    // ==================== UTILITÁRIOS ====================
    
    /**
     * Log de performance - mede tempo de execução.
     */
    public static void timed(String operation, Runnable task) {
        long start = System.nanoTime();
        task.run();
        long elapsed = System.nanoTime() - start;
        debug("%s completed in %.3f ms", operation, elapsed / 1_000_000.0);
    }
    
    /**
     * Separador visual para organizar output.
     */
    public static void separator() {
        if (currentLevel.priority <= Level.DEBUG.priority) {
            System.out.println("─".repeat(60));
        }
    }
}

