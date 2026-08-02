package com.gdx.utils;

/**
 * Менеджер интерполяции для плавной отрисовки между обновлениями игровой логики
 */
public class InterpolationManager {
    
    /**
     * Получить коэффициент интерполяции (0.0 - 1.0)
     * 0.0 = предыдущее состояние
     * 1.0 = текущее состояние
     */
    public static float getInterpolationFactor() {
        float accumulator = TimeManager.getAccumulator();
        float fixedTimestep = TimeManager.getFixedStep();
        
        // Если аккумулятор меньше фиксированного шага, интерполируем
        if (accumulator < fixedTimestep) {
            return accumulator / fixedTimestep;
        }
        
        // Иначе используем текущее состояние
        return 1.0f;
    }
    
    /**
     * Интерполировать между двумя значениями
     */
    public static float interpolate(float previous, float current) {
        float alpha = getInterpolationFactor();
        return previous + (current - previous) * alpha;
    }
    
    /**
     * Интерполировать позицию (x, y)
     */
    public static float[] interpolatePosition(float prevX, float prevY, float currentX, float currentY) {
        float alpha = getInterpolationFactor();
        float interpolatedX = prevX + (currentX - prevX) * alpha;
        float interpolatedY = prevY + (currentY - prevY) * alpha;
        return new float[]{interpolatedX, interpolatedY};
    }
    
    /**
     * Интерполировать угол (радианы)
     */
    public static float interpolateAngle(float prevAngle, float currentAngle) {
        float alpha = getInterpolationFactor();
        
        // Нормализуем углы для правильной интерполяции
        float diff = currentAngle - prevAngle;
        while (diff > Math.PI) diff -= 2 * Math.PI;
        while (diff < -Math.PI) diff += 2 * Math.PI;
        
        return prevAngle + diff * alpha;
    }
}
