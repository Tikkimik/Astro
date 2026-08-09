package com.gdx.entities;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.gdx.managers.Camera;

public class ShieldParticle extends Particle {
    
    private float angle;
    private float radius;
    private float speed;
    private float life;
    private float maxLife;
    
    public ShieldParticle(float x, float y, float angle, float radius) {
        super(x, y);
        init(x, y, angle, radius);
    }
    
    // Метод инициализации для пула объектов
    public void init(float x, float y, float angle, float radius) {
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.radius = radius;
        this.speed = 50 + MathUtils.random() * 100; // Случайная скорость
        this.maxLife = 1.0f + MathUtils.random() * 0.5f; // Случайное время жизни
        this.life = maxLife;
        
        // Начальная позиция на окружности вокруг корабля
        this.x = x + MathUtils.cos(angle) * radius;
        this.y = y + MathUtils.sin(angle) * radius;
        
        // Движение по спирали наружу
        float outwardSpeed = 20 + MathUtils.random() * 30;
        this.dx = MathUtils.cos(angle) * outwardSpeed;
        this.dy = MathUtils.sin(angle) * outwardSpeed;
    }
    
    // Метод инициализации для пула объектов (переопределение базового)
    public void init(float x, float y) {
        init(x, y, MathUtils.random(2 * MathUtils.PI), 20 + MathUtils.random(30));
    }
    
    @Override
    public void update(float dt) {
        super.update(dt);
        life -= dt;
        
        // Добавляем небольшое случайное движение
        dx += (MathUtils.random() - 0.5f) * 50 * dt;
        dy += (MathUtils.random() - 0.5f) * 50 * dt;
        
        // Ограничиваем скорость
        float currentSpeed = (float) Math.sqrt(dx * dx + dy * dy);
        if (currentSpeed > 150) {
            dx = (dx / currentSpeed) * 150;
            dy = (dy / currentSpeed) * 150;
        }
    }
    
    @Override
    public void draw(ShapeRenderer shapeRenderer, Camera camera) {
        float screenX = camera.worldToScreenX(x);
        float screenY = camera.worldToScreenY(y);
        float scaledSize = 2 * camera.getCurrentZoom(); // Маленькие частицы
        
        // Цвет меняется от синего к прозрачному
        float alpha = life / maxLife;
        float blue = 0.5f + alpha * 0.5f; // От 0.5 до 1.0 синего
        shapeRenderer.setColor(0.2f, 0.4f, blue, alpha);
        
        shapeRenderer.circle(screenX, screenY, scaledSize);
    }
    
    @Override
    public boolean shouldRemove() {
        return life <= 0;
    }

    @Override
    public float getCullRadius() {
        return 3f;
    }
}
