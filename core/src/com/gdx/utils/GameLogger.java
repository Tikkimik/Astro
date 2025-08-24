package com.gdx.utils;

/**
 * Система логирования для игры с режимом отладки
 */
public class GameLogger {
    
    // Режимы логирования
    public static final int LOG_NONE = 0;      // Без логов
    public static final int LOG_ERROR = 1;     // Только ошибки
    public static final int LOG_WARN = 2;      // Предупреждения и ошибки
    public static final int LOG_INFO = 3;      // Информация, предупреждения и ошибки
    public static final int LOG_DEBUG = 4;     // Все логи включая отладку
    
    private static int logLevel = LOG_INFO;    // По умолчанию показываем INFO и выше
    private static boolean debugMode = false;  // Режим отладки
    
    /**
     * Установить уровень логирования
     */
    public static void setLogLevel(int level) {
        logLevel = Math.max(LOG_NONE, Math.min(LOG_DEBUG, level));
    }
    
    /**
     * Получить текущий уровень логирования
     */
    public static int getLogLevel() {
        return logLevel;
    }
    
    /**
     * Включить/выключить режим отладки
     */
    public static void setDebugMode(boolean enabled) {
        debugMode = enabled;
        if (enabled && logLevel < LOG_DEBUG) {
            logLevel = LOG_DEBUG;
        }
    }
    
    /**
     * Проверить, включен ли режим отладки
     */
    public static boolean isDebugMode() {
        return debugMode;
    }
    
    /**
     * Логирование ошибок
     */
    public static void error(String message) {
        if (logLevel >= LOG_ERROR) {
            System.err.println("[ERROR] " + message);
        }
    }
    
    /**
     * Логирование ошибок с исключением
     */
    public static void error(String message, Throwable e) {
        if (logLevel >= LOG_ERROR) {
            System.err.println("[ERROR] " + message);
            if (e != null) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Логирование предупреждений
     */
    public static void warn(String message) {
        if (logLevel >= LOG_WARN) {
            System.out.println("[WARN] " + message);
        }
    }
    
    /**
     * Логирование информации
     */
    public static void info(String message) {
        if (logLevel >= LOG_INFO) {
            System.out.println("[INFO] " + message);
        }
    }
    
    /**
     * Логирование отладочной информации
     */
    public static void debug(String message) {
        if (logLevel >= LOG_DEBUG && debugMode) {
            System.out.println("[DEBUG] " + message);
        }
    }
    
    /**
     * Логирование отладочной информации с форматированием
     */
    public static void debug(String format, Object... args) {
        if (logLevel >= LOG_DEBUG && debugMode) {
            System.out.println("[DEBUG] " + String.format(format, args));
        }
    }
    
    /**
     * Логирование производительности
     */
    public static void performance(String message) {
        if (logLevel >= LOG_INFO) {
            System.out.println("[PERF] " + message);
        }
    }
    
    /**
     * Логирование ввода
     */
    public static void input(String message) {
        if (logLevel >= LOG_DEBUG && debugMode) {
            System.out.println("[INPUT] " + message);
        }
    }
    
    /**
     * Логирование состояния игры
     */
    public static void gameState(String message) {
        if (logLevel >= LOG_DEBUG && debugMode) {
            System.out.println("[STATE] " + message);
        }
    }
    
    /**
     * Логирование настроек
     */
    public static void settings(String message) {
        if (logLevel >= LOG_INFO) {
            System.out.println("[SETTINGS] " + message);
        }
    }
    
    /**
     * Логирование дисплея
     */
    public static void display(String message) {
        if (logLevel >= LOG_INFO) {
            System.out.println("[DISPLAY] " + message);
        }
    }
    
    /**
     * Получить название уровня логирования
     */
    public static String getLogLevelName(int level) {
        switch (level) {
            case LOG_NONE: return "NONE";
            case LOG_ERROR: return "ERROR";
            case LOG_WARN: return "WARN";
            case LOG_INFO: return "INFO";
            case LOG_DEBUG: return "DEBUG";
            default: return "UNKNOWN";
        }
    }
    
    /**
     * Получить название текущего уровня логирования
     */
    public static String getCurrentLogLevelName() {
        return getLogLevelName(logLevel);
    }
    
    /**
     * Сбросить настройки логирования к значениям по умолчанию
     */
    public static void resetToDefaults() {
        logLevel = LOG_INFO;
        debugMode = false;
    }
}

