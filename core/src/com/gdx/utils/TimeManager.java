package com.gdx.utils;

import com.badlogic.gdx.Gdx;
import com.gdx.utils.GameConfig;

/**
 * Менеджер времени для обеспечения независимости игровой логики от FPS
 */
public class TimeManager {
    
    // Фиксированный временной шаг (из конфигурации)
    public static final float FIXED_TIMESTEP = GameConfig.getFixedTimestep();
    
    // Максимальное накопление времени для предотвращения спирали смерти
    public static final float MAX_ACCUMULATOR = GameConfig.getMaxAccumulator();
    
    // Аккумулятор времени
    private static float timeAccumulator = 0.0f;
    
    // Счетчики для статистики
    private static int updateCount = 0;
    private static int renderCount = 0;
    private static float lastStatsTime = 0.0f;
    
    /**
     * Получить deltaTime и обновить аккумулятор
     */
    public static float getDeltaTime() {
        float deltaTime = Gdx.graphics.getDeltaTime();
        
        // Защита от некорректных значений
        if (deltaTime <= 0) {
            System.out.println("WARNING: Invalid deltaTime: " + deltaTime + ", using 0.016f");
            deltaTime = 0.016f;
        }
        
        // Ограничиваем максимальный deltaTime
        if (deltaTime > GameConfig.MAX_DELTA_TIME) {
            System.out.println("WARNING: Large deltaTime: " + deltaTime + ", clamping to " + GameConfig.MAX_DELTA_TIME);
            deltaTime = GameConfig.MAX_DELTA_TIME;
        }
        
        return deltaTime;
    }
    
    /**
     * Обновить аккумулятор времени
     */
    public static void updateAccumulator(float deltaTime) {
        timeAccumulator += deltaTime;
        
        // Ограничиваем накопление времени
        if (timeAccumulator > MAX_ACCUMULATOR) {
            timeAccumulator = MAX_ACCUMULATOR;
        }
    }
    
    /**
     * Проверить, нужно ли выполнить обновление игровой логики
     */
    public static boolean shouldUpdate() {
        return timeAccumulator >= FIXED_TIMESTEP;
    }
    
    /**
     * Получить фиксированный временной шаг и уменьшить аккумулятор
     */
    public static float getFixedTimestep() {
        timeAccumulator -= FIXED_TIMESTEP;
        updateCount++;
        return FIXED_TIMESTEP;
    }
    
    /**
     * Увеличить счетчик рендеринга
     */
    public static void incrementRenderCount() {
        renderCount++;
    }
    
    /**
     * Получить статистику производительности
     */
    public static String getStats() {
        if (!GameConfig.isPerformanceStatsEnabled()) {
            return "";
        }
        
        float currentTime = Gdx.graphics.getDeltaTime() > 0 ? 1.0f / Gdx.graphics.getDeltaTime() : 0;
        
        if (currentTime > 0 && currentTime < 100) {
            return String.format("Render FPS: %.1f | Updates: %d | Renders: %d | Fixed Step: %.6fms", 
                currentTime, updateCount, renderCount, FIXED_TIMESTEP * 1000);
        }
        return "";
    }
    
    /**
     * Сбросить статистику
     */
    public static void resetStats() {
        updateCount = 0;
        renderCount = 0;
    }
    
    /**
     * Получить текущий аккумулятор времени (для отладки)
     */
    public static float getAccumulator() {
        return timeAccumulator;
    }
}
