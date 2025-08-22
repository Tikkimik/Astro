package com.gdx.utils;

/**
 * Настраиваемые игровые настройки
 */
public class GameSettings {
    
    // === НАСТРОЙКИ ВРЕМЕНИ ===
    
    /**
     * Целевая частота обновления игровой логики (FPS)
     */
    private static int targetLogicFPS = 60;
    
    /**
     * Максимальный FPS рендеринга (0 = без ограничений)
     */
    private static int maxRenderFPS = 0;
    
    /**
     * Максимальное накопление времени (в секундах)
     */
    private static float maxAccumulator = 0.25f;
    
    /**
     * Максимальный deltaTime (защита от больших скачков)
     */
    private static float maxDeltaTime = 0.1f;
    
    // === НАСТРОЙКИ РЕНДЕРИНГА ===
    
    /**
     * Включить интерполяцию для плавной отрисовки
     */
    private static boolean enableInterpolation = false;
    
    /**
     * Показывать статистику производительности
     */
    private static boolean showPerformanceStats = true;
    
    /**
     * Интервал вывода статистики (в кадрах)
     */
    private static int statsInterval = 60;
    
    // === НАСТРОЙКИ ИГРЫ ===
    
    /**
     * Размер мира (множитель экрана)
     */
    private static float worldScale = 4.0f;
    
    /**
     * Скорость игрока
     */
    private static float playerSpeed = 300.0f;
    
    /**
     * Скорость пуль
     */
    private static float bulletSpeed = 350.0f;
    
    /**
     * Время жизни пуль (в секундах)
     */
    private static float bulletLifetime = 1.0f;
    
    // === НАСТРОЙКИ ЧАСТИЦ ===
    
    /**
     * Максимальное количество частиц на экране
     */
    private static int maxParticles = 1000;
    
    /**
     * Частота создания частиц ракет (0.0 - 1.0)
     */
    private static float rocketParticleRate = 0.9f;
    
    /**
     * Время жизни частиц ракет (в секундах)
     */
    private static float rocketParticleLifetime = 1.2f;
    
    // === НАСТРОЙКИ КОЛЛИЗИЙ ===
    
    /**
     * Включить детекцию коллизий
     */
    private static boolean enableCollisions = true;
    
    /**
     * Использовать оптимизированные коллизии
     */
    private static boolean useOptimizedCollisions = true;
    
    // === НАСТРОЙКИ ЗВУКА ===
    
    /**
     * Включить звук
     */
    private static boolean enableSound = false;
    
    /**
     * Громкость звука (0.0 - 1.0)
     */
    private static float soundVolume = 0.5f;
    
    // === ГЕТТЕРЫ И СЕТТЕРЫ ===
    
    // Время
    public static int getTargetLogicFPS() { return targetLogicFPS; }
    public static void setTargetLogicFPS(int fps) { 
        targetLogicFPS = Math.max(30, Math.min(240, fps)); 
        // Обновляем фиксированный шаг
        GameConfig.updateFixedTimestep();
    }
    
    public static int getMaxRenderFPS() { return maxRenderFPS; }
    public static void setMaxRenderFPS(int fps) { 
        maxRenderFPS = Math.max(0, Math.min(240, fps)); 
    }
    
    public static float getMaxAccumulator() { return maxAccumulator; }
    public static void setMaxAccumulator(float value) { 
        maxAccumulator = Math.max(0.1f, Math.min(1.0f, value)); 
    }
    
    public static float getMaxDeltaTime() { return maxDeltaTime; }
    public static void setMaxDeltaTime(float value) { 
        maxDeltaTime = Math.max(0.05f, Math.min(0.5f, value)); 
    }
    
    // Рендеринг
    public static boolean isInterpolationEnabled() { return enableInterpolation; }
    public static void setInterpolationEnabled(boolean enabled) { enableInterpolation = enabled; }
    
    public static boolean isPerformanceStatsEnabled() { return showPerformanceStats; }
    public static void setPerformanceStatsEnabled(boolean enabled) { showPerformanceStats = enabled; }
    
    public static int getStatsInterval() { return statsInterval; }
    public static void setStatsInterval(int interval) { 
        statsInterval = Math.max(10, Math.min(300, interval)); 
    }
    
    // Игра
    public static float getWorldScale() { return worldScale; }
    public static void setWorldScale(float scale) { 
        worldScale = Math.max(1.0f, Math.min(10.0f, scale)); 
    }
    
    public static float getPlayerSpeed() { return playerSpeed; }
    public static void setPlayerSpeed(float speed) { 
        playerSpeed = Math.max(100.0f, Math.min(1000.0f, speed)); 
    }
    
    public static float getBulletSpeed() { return bulletSpeed; }
    public static void setBulletSpeed(float speed) { 
        bulletSpeed = Math.max(100.0f, Math.min(1000.0f, speed)); 
    }
    
    public static float getBulletLifetime() { return bulletLifetime; }
    public static void setBulletLifetime(float lifetime) { 
        bulletLifetime = Math.max(0.1f, Math.min(10.0f, lifetime)); 
    }
    
    // Частицы
    public static int getMaxParticles() { return maxParticles; }
    public static void setMaxParticles(int max) { 
        maxParticles = Math.max(100, Math.min(5000, max)); 
    }
    
    public static float getRocketParticleRate() { return rocketParticleRate; }
    public static void setRocketParticleRate(float rate) { 
        rocketParticleRate = Math.max(0.0f, Math.min(1.0f, rate)); 
    }
    
    public static float getRocketParticleLifetime() { return rocketParticleLifetime; }
    public static void setRocketParticleLifetime(float lifetime) { 
        rocketParticleLifetime = Math.max(0.1f, Math.min(5.0f, lifetime)); 
    }
    
    // Коллизии
    public static boolean isCollisionsEnabled() { return enableCollisions; }
    public static void setCollisionsEnabled(boolean enabled) { enableCollisions = enabled; }
    
    public static boolean isOptimizedCollisionsEnabled() { return useOptimizedCollisions; }
    public static void setOptimizedCollisionsEnabled(boolean enabled) { useOptimizedCollisions = enabled; }
    
    // Звук
    public static boolean isSoundEnabled() { return enableSound; }
    public static void setSoundEnabled(boolean enabled) { enableSound = enabled; }
    
    public static float getSoundVolume() { return soundVolume; }
    public static void setSoundVolume(float volume) { 
        soundVolume = Math.max(0.0f, Math.min(1.0f, volume)); 
    }
    
    // === УТИЛИТЫ ===
    
    /**
     * Сбросить настройки к значениям по умолчанию
     */
    public static void resetToDefaults() {
        targetLogicFPS = 60;
        maxRenderFPS = 0;
        maxAccumulator = 0.25f;
        maxDeltaTime = 0.1f;
        enableInterpolation = false;
        showPerformanceStats = true;
        statsInterval = 60;
        worldScale = 4.0f;
        playerSpeed = 300.0f;
        bulletSpeed = 350.0f;
        bulletLifetime = 1.0f;
        maxParticles = 1000;
        rocketParticleRate = 0.9f;
        rocketParticleLifetime = 1.2f;
        enableCollisions = true;
        useOptimizedCollisions = true;
        enableSound = false;
        soundVolume = 0.5f;
        
        // Обновляем GameConfig
        GameConfig.updateFromSettings();
    }
    
    /**
     * Получить фиксированный временной шаг
     */
    public static float getFixedTimestep() {
        return 1.0f / targetLogicFPS;
    }
    
    /**
     * Получить максимальный аккумулятор
     */
    public static float getMaxAccumulatorValue() {
        return maxAccumulator;
    }
}
