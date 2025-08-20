package com.gdx.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;

public class PinchZoomGesture {
    private Vector2 firstTouch;
    private Vector2 secondTouch;
    private float initialDistance;
    private float currentDistance;
    private boolean isActive;
    private int firstPointer;
    private int secondPointer;
    
    // Пороги для определения жестов
    private static final float MIN_DISTANCE = 50f; // Минимальное расстояние между пальцами
    private static final float ZOOM_SENSITIVITY = 0.01f; // Чувствительность масштабирования
    
    public PinchZoomGesture() {
        this.firstTouch = new Vector2();
        this.secondTouch = new Vector2();
        this.isActive = false;
        this.firstPointer = -1;
        this.secondPointer = -1;
    }
    
    public void update() {
        // Сброс состояния каждый кадр
        if (!isActive) {
            initialDistance = 0;
            currentDistance = 0;
        }
    }
    
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (firstPointer == -1) {
            // Первое касание
            firstPointer = pointer;
            firstTouch.set(screenX, Gdx.graphics.getHeight() - screenY);
            return false; // Не обрабатываем как жест пока нет второго пальца
        } else if (secondPointer == -1 && pointer != firstPointer) {
            // Второе касание - начинаем жест масштабирования
            secondPointer = pointer;
            secondTouch.set(screenX, Gdx.graphics.getHeight() - screenY);
            
            // Вычисляем начальное расстояние
            initialDistance = firstTouch.dst(secondTouch);
            currentDistance = initialDistance;
            
            if (initialDistance > MIN_DISTANCE) {
                isActive = true;
                return true; // Обрабатываем как жест масштабирования
            }
        }
        return false;
    }
    
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (!isActive) return false;
        
        if (pointer == firstPointer) {
            firstTouch.set(screenX, Gdx.graphics.getHeight() - screenY);
        } else if (pointer == secondPointer) {
            secondTouch.set(screenX, Gdx.graphics.getHeight() - screenY);
        }
        
        if (firstPointer != -1 && secondPointer != -1) {
            currentDistance = firstTouch.dst(secondTouch);
            return true;
        }
        
        return false;
    }
    
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (pointer == firstPointer) {
            firstPointer = -1;
        } else if (pointer == secondPointer) {
            secondPointer = -1;
        }
        
        // Если один из пальцев отпущен, прекращаем жест
        if (firstPointer == -1 || secondPointer == -1) {
            isActive = false;
            initialDistance = 0;
            currentDistance = 0;
            return true;
        }
        
        return false;
    }
    
    /**
     * Возвращает коэффициент масштабирования
     * @return положительное значение для увеличения, отрицательное для уменьшения
     */
    public float getZoomFactor() {
        if (!isActive || initialDistance <= 0) return 0;
        
        float zoomDelta = (currentDistance - initialDistance) * ZOOM_SENSITIVITY;
        return zoomDelta;
    }
    
    /**
     * Проверяет, активен ли жест масштабирования
     */
    public boolean isActive() {
        return isActive;
    }
    
    /**
     * Получает центр жеста (середина между двумя пальцами)
     */
    public Vector2 getGestureCenter() {
        if (!isActive) return null;
        
        Vector2 center = new Vector2();
        center.set(firstTouch).add(secondTouch).scl(0.5f);
        return center;
    }
    
    /**
     * Сбрасывает состояние жеста
     */
    public void reset() {
        isActive = false;
        firstPointer = -1;
        secondPointer = -1;
        initialDistance = 0;
        currentDistance = 0;
    }
}
