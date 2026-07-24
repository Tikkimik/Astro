package com.gdx.utils;

import com.badlogic.gdx.Gdx;
import com.gdx.game.MyGdxGame;

/**
 * Мониторинг производительности и оптимизаций
 */
public class PerformanceMonitor {
    
    private static long frameStartTime;
    private static long lastReportTime;
    private static int frameCount;
    private static final long REPORT_INTERVAL = 5000000000L; // 5 секунд в наносекундах
    
    // Счетчики для оптимизаций
    private static int particlesCreated = 0;
    private static int particlesReused = 0;
    private static int shadersCreated = 0;
    private static int shadersReused = 0;
    
    /**
     * Начать отсчет времени для кадра
     */
    public static void startFrame() {
        frameStartTime = System.nanoTime();
    }
    
    /**
     * Завершить кадр и обновить статистику
     */
    public static void endFrame() {
        frameCount++;
        
        long currentTime = System.nanoTime();
        if (currentTime - lastReportTime > REPORT_INTERVAL) {
            reportPerformance();
            lastReportTime = currentTime;
        }
    }
    
    /**
     * Увеличить счетчик созданных частиц
     */
    public static void incrementParticlesCreated() {
        particlesCreated++;
    }
    
    /**
     * Увеличить счетчик переиспользованных частиц
     */
    public static void incrementParticlesReused() {
        particlesReused++;
    }
    
    /**
     * Увеличить счетчик созданных шейдеров
     */
    public static void incrementShadersCreated() {
        shadersCreated++;
    }
    
    /**
     * Увеличить счетчик переиспользованных шейдеров
     */
    public static void incrementShadersReused() {
        shadersReused++;
    }
    
    /**
     * Получить статистику производительности
     */
    private static void reportPerformance() {
        if (!MyGdxGame.DEBUG_MODE) return;
        
        float fps = frameCount / 5.0f; // 5 секунд
        float particleEfficiency = frameCount > 0 ? 
            (float) particlesReused / (particlesCreated + particlesReused) * 100 : 0;
        float shaderEfficiency = frameCount > 0 ? 
            (float) shadersReused / (shadersCreated + shadersReused) * 100 : 0;
        
        System.out.println("=== PERFORMANCE REPORT ===");
        System.out.println("FPS: " + String.format("%.1f", fps));
        System.out.println("Particles - Created: " + particlesCreated + ", Reused: " + particlesReused);
        System.out.println("Particle Efficiency: " + String.format("%.1f", particleEfficiency) + "%");
        System.out.println("Shaders - Created: " + shadersCreated + ", Reused: " + shadersReused);
        System.out.println("Shader Efficiency: " + String.format("%.1f", shaderEfficiency) + "%");
        System.out.println("Particle Pool Stats: " + ParticlePool.getPoolStats());
        System.out.println("==========================");
        
        // Сбрасываем счетчики
        frameCount = 0;
    }
    
    /**
     * Получить текущий FPS
     */
    public static float getCurrentFPS() {
        return Gdx.graphics.getFramesPerSecond();
    }
    
    /**
     * Получить эффективность пула частиц
     */
    public static float getParticleEfficiency() {
        int total = particlesCreated + particlesReused;
        return total > 0 ? (float) particlesReused / total * 100 : 0;
    }
    
    /**
     * Сбросить все счетчики
     */
    public static void resetCounters() {
        particlesCreated = 0;
        particlesReused = 0;
        shadersCreated = 0;
        shadersReused = 0;
        frameCount = 0;
    }
}
