package com.gdx.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.gdx.managers.Camera;
import com.gdx.utils.ParticleTextures;

public class OptimizedParticle extends SpaceObject {
    
    private float timer;
    private float time;
    private boolean remove;
    private String particleType;
    private float size;
    private float maxSize;
    
    public OptimizedParticle(float x, float y, String type) {
        init(x, y, type);
    }
    
    // Метод инициализации для пула объектов
    public void init(float x, float y, String type) {
        this.x = x;
        this.y = y;
        this.remove = false;
        this.timer = 0;
        this.particleType = type;
        
        // Настройки в зависимости от типа
        switch (type) {
            case "flame":
                this.time = 0.8f;
                this.maxSize = 8f + MathUtils.random(4f);
                this.speed = 60 + MathUtils.random(40);
                break;
            case "explosion":
                this.time = 1.2f;
                this.maxSize = 6f + MathUtils.random(6f);
                this.speed = 80 + MathUtils.random(60);
                break;
            case "smoke":
                this.time = 2.0f;
                this.maxSize = 4f + MathUtils.random(3f);
                this.speed = 30 + MathUtils.random(20);
                break;
            default: // simple
                this.time = 1.0f;
                this.maxSize = 3f + MathUtils.random(2f);
                this.speed = 50 + MathUtils.random(30);
                break;
        }
        
        this.size = maxSize;
        
        // Случайное направление
        radians = MathUtils.random(2 * MathUtils.PI);
        dx = MathUtils.cos(radians) * speed;
        dy = MathUtils.sin(radians) * speed;
    }
    
    public boolean shouldRemove() {
        return remove;
    }
    
    public void update(float dt) {
        x += dx * dt;
        y += dy * dt;
        
        timer += dt;
        
        // Уменьшаем размер по мере затухания
        float lifeRatio = 1.0f - (timer / time);
        size = maxSize * lifeRatio;
        
        // Замедляем частицу
        dx *= 0.98f;
        dy *= 0.98f;
        
        if (timer > time) {
            remove = true;
        }
    }
    
    public void draw(SpriteBatch batch, Camera camera) {
        if (remove) return;
        
        float screenX = camera.worldToScreenX(x);
        float screenY = camera.worldToScreenY(y);
        float scaledSize = size * camera.getCurrentZoom();
        
        // Прозрачность зависит от времени жизни
        float alpha = 1.0f - (timer / time);
        alpha = Math.max(0, alpha);
        
        // Рисуем частицу с помощью текстур
        ParticleTextures.getInstance().drawParticle(batch, particleType, screenX, screenY, scaledSize, alpha);
    }
    
    // Метод для получения типа частицы
    public String getParticleType() {
        return particleType;
    }

    /**
     * Радиус для проверки видимости при culling (в мировых координатах).
     */
    public float getCullRadius() {
        return size * 0.6f + 1f;
    }
}
