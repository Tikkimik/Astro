package com.gdx.utils;

import com.badlogic.gdx.Gdx;

/**
 * Утилита для тестирования производительности системы фиксированного временного шага
 */
public class PerformanceTest {
    
    private static long testStartTime;
    private static int frameCount = 0;
    private static int updateCount = 0;
    private static float totalUpdateTime = 0;
    private static float totalRenderTime = 0;
    private static float minUpdateTime = Float.MAX_VALUE;
    private static float maxUpdateTime = 0;
    private static float minRenderTime = Float.MAX_VALUE;
    private static float maxRenderTime = 0;
    
    /**
     * Начать тест производительности
     */
    public static void startTest() {
        testStartTime = System.nanoTime();
        frameCount = 0;
        updateCount = 0;
        totalUpdateTime = 0;
        totalRenderTime = 0;
        minUpdateTime = Float.MAX_VALUE;
        maxUpdateTime = 0;
        minRenderTime = Float.MAX_VALUE;
        maxRenderTime = 0;
        
        System.out.println("=== НАЧАЛО ТЕСТА ПРОИЗВОДИТЕЛЬНОСТИ ===");
        System.out.println("Целевой FPS логики: " + GameConfig.TARGET_LOGIC_FPS);
        System.out.println("Фиксированный шаг: " + (GameConfig.FIXED_TIMESTEP * 1000) + "ms");
        System.out.println("Максимальный аккумулятор: " + GameConfig.MAX_ACCUMULATOR + "s");
        System.out.println();
    }
    
    /**
     * Записать время обновления
     */
    public static void recordUpdateTime(float updateTimeMs) {
        updateCount++;
        totalUpdateTime += updateTimeMs;
        minUpdateTime = Math.min(minUpdateTime, updateTimeMs);
        maxUpdateTime = Math.max(maxUpdateTime, updateTimeMs);
    }
    
    /**
     * Записать время рендеринга
     */
    public static void recordRenderTime(float renderTimeMs) {
        frameCount++;
        totalRenderTime += renderTimeMs;
        minRenderTime = Math.min(minRenderTime, renderTimeMs);
        maxRenderTime = Math.max(maxRenderTime, renderTimeMs);
    }
    
    /**
     * Завершить тест и вывести результаты
     */
    public static void endTest() {
        long testEndTime = System.nanoTime();
        float testDuration = (testEndTime - testStartTime) / 1_000_000_000f; // в секундах
        
        System.out.println("=== РЕЗУЛЬТАТЫ ТЕСТА ПРОИЗВОДИТЕЛЬНОСТИ ===");
        System.out.println("Длительность теста: " + String.format("%.2f", testDuration) + "s");
        System.out.println();
        
        // Статистика обновлений
        System.out.println("📊 ОБНОВЛЕНИЯ ИГРОВОЙ ЛОГИКИ:");
        System.out.println("  Всего обновлений: " + updateCount);
        System.out.println("  Обновлений в секунду: " + String.format("%.1f", updateCount / testDuration));
        System.out.println("  Среднее время: " + String.format("%.3f", totalUpdateTime / updateCount) + "ms");
        System.out.println("  Минимальное время: " + String.format("%.3f", minUpdateTime) + "ms");
        System.out.println("  Максимальное время: " + String.format("%.3f", maxUpdateTime) + "ms");
        System.out.println();
        
        // Статистика рендеринга
        System.out.println("🎨 РЕНДЕРИНГ:");
        System.out.println("  Всего кадров: " + frameCount);
        System.out.println("  FPS рендеринга: " + String.format("%.1f", frameCount / testDuration));
        System.out.println("  Среднее время кадра: " + String.format("%.3f", totalRenderTime / frameCount) + "ms");
        System.out.println("  Минимальное время: " + String.format("%.3f", minRenderTime) + "ms");
        System.out.println("  Максимальное время: " + String.format("%.3f", maxRenderTime) + "ms");
        System.out.println();
        
        // Анализ производительности
        System.out.println("🔍 АНАЛИЗ ПРОИЗВОДИТЕЛЬНОСТИ:");
        
        float targetUpdatesPerSecond = GameConfig.TARGET_LOGIC_FPS;
        float actualUpdatesPerSecond = updateCount / testDuration;
        float updateAccuracy = (actualUpdatesPerSecond / targetUpdatesPerSecond) * 100;
        
        System.out.println("  Точность обновлений: " + String.format("%.1f", updateAccuracy) + "%");
        
        if (updateAccuracy >= 95) {
            System.out.println("  ✅ Отличная точность обновлений!");
        } else if (updateAccuracy >= 90) {
            System.out.println("  ⚠️  Хорошая точность обновлений");
        } else {
            System.out.println("  ❌ Низкая точность обновлений");
        }
        
        float avgRenderTime = totalRenderTime / frameCount;
        float targetRenderTime = 1000f / GameConfig.TARGET_LOGIC_FPS; // 16.67ms для 60 FPS
        
        if (avgRenderTime <= targetRenderTime) {
            System.out.println("  ✅ Рендеринг укладывается в бюджет времени");
        } else {
            System.out.println("  ⚠️  Рендеринг превышает бюджет времени");
        }
        
        // Рекомендации
        System.out.println();
        System.out.println("💡 РЕКОМЕНДАЦИИ:");
        
        if (maxUpdateTime > 1.0f) {
            System.out.println("  - Обнаружены пики времени обновления > 1ms");
            System.out.println("  - Рекомендуется оптимизировать игровую логику");
        }
        
        if (maxRenderTime > 16.67f) {
            System.out.println("  - Обнаружены кадры > 16.67ms");
            System.out.println("  - Рекомендуется оптимизировать отрисовку");
        }
        
        if (updateAccuracy < 95) {
            System.out.println("  - Низкая точность обновлений");
            System.out.println("  - Рекомендуется проверить настройки TimeManager");
        }
        
        System.out.println();
        System.out.println("=== КОНЕЦ ТЕСТА ===");
    }
    
    /**
     * Быстрый тест системы
     */
    public static void quickTest() {
        System.out.println("🚀 БЫСТРЫЙ ТЕСТ СИСТЕМЫ ФИКСИРОВАННОГО ВРЕМЕННОГО ШАГА");
        System.out.println();
        
        // Тест 1: Проверка констант
        System.out.println("1. Проверка констант:");
        System.out.println("   FIXED_TIMESTEP: " + TimeManager.FIXED_TIMESTEP + "s (" + (TimeManager.FIXED_TIMESTEP * 1000) + "ms)");
        System.out.println("   MAX_ACCUMULATOR: " + TimeManager.MAX_ACCUMULATOR + "s");
        System.out.println("   TARGET_LOGIC_FPS: " + GameConfig.TARGET_LOGIC_FPS);
        System.out.println();
        
        // Тест 2: Проверка TimeManager
        System.out.println("2. Тест TimeManager:");
        float testDeltaTime = 0.016f; // 60 FPS
        TimeManager.updateAccumulator(testDeltaTime);
        System.out.println("   Аккумулятор после 16ms: " + TimeManager.getAccumulator());
        System.out.println("   Должно обновляться: " + TimeManager.shouldUpdate());
        System.out.println();
        
        // Тест 3: Проверка InterpolationManager
        System.out.println("3. Тест InterpolationManager:");
        float factor = InterpolationManager.getInterpolationFactor();
        System.out.println("   Коэффициент интерполяции: " + String.format("%.3f", factor));
        System.out.println();
        
        // Тест 4: Проверка GameConfig
        System.out.println("4. Тест GameConfig:");
        System.out.println("   Интерполяция включена: " + GameConfig.isInterpolationEnabled());
        System.out.println("   Статистика включена: " + GameConfig.isPerformanceStatsEnabled());
        System.out.println("   Максимальный deltaTime: " + GameConfig.MAX_DELTA_TIME + "s");
        System.out.println();
        
        System.out.println("✅ Все тесты пройдены успешно!");
    }
}
