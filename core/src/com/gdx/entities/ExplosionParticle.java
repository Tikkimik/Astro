package com.gdx.entities;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.gdx.managers.Camera;

public class ExplosionParticle extends Particle {

    private float timer;
    private float time;
    private boolean remove;
    private float size;
    private float maxSize;
    private float colorR, colorG, colorB;

    public ExplosionParticle(float x, float y) {
        super(x, y);
        init(x, y);
    }
    
    // Метод инициализации для пула объектов
    public void init(float x, float y) {
        this.x = x;
        this.y = y;
        this.remove = false;
        
        // Случайное направление взрыва
        speed = MathUtils.random(100, 200);
        radians = MathUtils.random(2 * MathUtils.PI);
        dx = MathUtils.cos(radians) * speed;
        dy = MathUtils.sin(radians) * speed;

        timer = 0;
        time = MathUtils.random(0.8f, 1.5f); // Разное время жизни
        
        // Размер частицы
        size = MathUtils.random(2, 6);
        maxSize = size * MathUtils.random(1.5f, 3f);
        
        // Цвет взрыва (от оранжевого до красного)
        colorR = MathUtils.random(0.8f, 1f);
        colorG = MathUtils.random(0.2f, 0.6f);
        colorB = MathUtils.random(0.1f, 0.3f);
    }

    public boolean shouldRemove() {
        return remove;
    }

    public void update(float dt) {
        x += dx * dt;
        y += dy * dt;

        // Замедляем частицу
        dx *= 0.95f;
        dy *= 0.95f;

        timer += dt;
        
        // Увеличиваем размер в начале, затем уменьшаем
        if (timer < time * 0.3f) {
            size += dt * 10;
        } else {
            size -= dt * 5;
        }
        
        if (size < 0) size = 0;

        if (timer > time) {
            remove = true;
        }
    }

    public void draw(ShapeRenderer shapeRenderer, Camera camera) {
        // Прозрачность зависит от времени жизни
        float alpha = 1f - (timer / time);
        if (alpha < 0) alpha = 0;
        
        // Цвет меняется от яркого к тусклому
        float brightness = 0.3f + 0.7f * alpha;
        
        shapeRenderer.setColor(colorR * brightness, colorG * brightness, colorB * brightness, alpha);
        
        float screenX = camera.worldToScreenX(x);
        float screenY = camera.worldToScreenY(y);
        
        // Рисуем частицу как круг
        shapeRenderer.circle(screenX, screenY, size);
    }
}
