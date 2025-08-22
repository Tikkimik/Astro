package com.gdx.utils;

/**
 * Конфигурация игры - централизованное место для всех настроек
 */
public class GameConfig {
    
    // === НАСТРОЙКИ ВРЕМЕНИ ===
    
    /**
     * Целевая частота обновления игровой логики (FPS)
     * Рекомендуемые значения: 30, 60, 120
     */
    public static int TARGET_LOGIC_FPS = 60;
    
    /**
     * Фиксированный временной шаг для игровой логики
     */
    public static float FIXED_TIMESTEP = 1.0f / TARGET_LOGIC_FPS;
    
    /**
     * Максимальное накопление времени (в секундах)
     * Защищает от спирали смерти при очень низком FPS
     */
    public static float MAX_ACCUMULATOR = 0.25f;
    
    // === НАСТРОЙКИ РЕНДЕРИНГА ===
    
    /**
     * Максимальный FPS рендеринга (0 = без ограничений)
     */
    public static int MAX_RENDER_FPS = 0;
    
    /**
     * Включить интерполяцию для плавной отрисовки
     */
    public static boolean ENABLE_INTERPOLATION = false;
    
    // === НАСТРОЙКИ ПРОИЗВОДИТЕЛЬНОСТИ ===
    
    /**
     * Показывать статистику производительности
     */
    public static boolean SHOW_PERFORMANCE_STATS = true;
    
    /**
     * Интервал вывода статистики (в кадрах)
     */
    public static int STATS_INTERVAL = 60;
    
    /**
     * Максимальный deltaTime (защита от больших скачков)
     */
    public static float MAX_DELTA_TIME = 0.1f;
    
    // === НАСТРОЙКИ ИГРЫ ===
    
    /**
     * Размер мира (множитель экрана)
     */
    public static final float WORLD_SCALE = 4.0f;
    
    /**
     * Скорость игрока
     */
    public static final float PLAYER_SPEED = 300.0f;
    
    /**
     * Скорость пуль
     */
    public static final float BULLET_SPEED = 350.0f;
    
    /**
     * Время жизни пуль (в секундах)
     */
    public static final float BULLET_LIFETIME = 1.0f;
    
    // === НАСТРОЙКИ ЧАСТИЦ ===
    
    /**
     * Максимальное количество частиц на экране
     */
    public static final int MAX_PARTICLES = 1000;
    
    /**
     * Частота создания частиц ракет (0.0 - 1.0)
     */
    public static final float ROCKET_PARTICLE_RATE = 0.9f;
    
    /**
     * Время жизни частиц ракет (в секундах)
     */
    public static final float ROCKET_PARTICLE_LIFETIME = 1.2f;
    
    // === НАСТРОЙКИ КОЛЛИЗИЙ ===
    
    /**
     * Включить детекцию коллизий
     */
    public static final boolean ENABLE_COLLISIONS = true;
    
    /**
     * Использовать оптимизированные коллизии
     */
    public static final boolean USE_OPTIMIZED_COLLISIONS = true;
    
    // === НАСТРОЙКИ ЗВУКА ===
    
    /**
     * Включить звук
     */
    public static final boolean ENABLE_SOUND = false;
    
    /**
     * Громкость звука (0.0 - 1.0)
     */
    public static final float SOUND_VOLUME = 0.5f;
    
    // === УТИЛИТЫ ===
    
    /**
     * Получить фиксированный временной шаг
     */
    public static float getFixedTimestep() {
        return FIXED_TIMESTEP;
    }
    
    /**
     * Получить максимальный аккумулятор
     */
    public static float getMaxAccumulator() {
        return MAX_ACCUMULATOR;
    }
    
    /**
     * Проверить, включена ли интерполяция
     */
    public static boolean isInterpolationEnabled() {
        return ENABLE_INTERPOLATION;
    }
    
    /**
     * Проверить, включена ли статистика производительности
     */
    public static boolean isPerformanceStatsEnabled() {
        return SHOW_PERFORMANCE_STATS;
    }
    
    /**
     * Обновить фиксированный временной шаг
     */
    public static void updateFixedTimestep() {
        FIXED_TIMESTEP = 1.0f / TARGET_LOGIC_FPS;
    }
    
    /**
     * Обновить настройки из GameSettings
     */
    public static void updateFromSettings() {
        TARGET_LOGIC_FPS = GameSettings.getTargetLogicFPS();
        FIXED_TIMESTEP = GameSettings.getFixedTimestep();
        MAX_ACCUMULATOR = GameSettings.getMaxAccumulatorValue();
        MAX_RENDER_FPS = GameSettings.getMaxRenderFPS();
        ENABLE_INTERPOLATION = GameSettings.isInterpolationEnabled();
        SHOW_PERFORMANCE_STATS = GameSettings.isPerformanceStatsEnabled();
        STATS_INTERVAL = GameSettings.getStatsInterval();
        MAX_DELTA_TIME = GameSettings.getMaxDeltaTime();
    }
    
    /**
     * Синхронизировать настройки с GameSettings
     */
    public static void syncWithSettings() {
        updateFromSettings();
    }
}
