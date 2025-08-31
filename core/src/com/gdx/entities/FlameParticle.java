package com.gdx.entities;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.gdx.managers.Camera;

public class FlameParticle extends Particle {
    
    private float life;
    private float maxLife;
    private float size;
    private float maxSize;
    private float speed;
    private float angle;
    
    public FlameParticle(float x, float y, float angle, float speed) {
        super(x, y);
        init(x, y, angle, speed);
    }
    
    // Метод инициализации для пула объектов
    public void init(float x, float y, float angle, float speed) {
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.speed = speed;
        this.maxLife = 0.5f + MathUtils.random() * 0.3f; // 0.5-0.8 секунды - дольше живут
        this.life = maxLife;
        this.maxSize = 4f + MathUtils.random() * 6f; // 4-10 пикселей - больше размер
        this.size = maxSize;
        
        // Начальная скорость частицы
        this.dx = MathUtils.cos(angle) * speed;
        this.dy = MathUtils.sin(angle) * speed;
    }
    
    // Метод инициализации для пула объектов (переопределение базового)
    public void init(float x, float y) {
        init(x, y, MathUtils.random(2 * MathUtils.PI), 50 + MathUtils.random(50));
    }
    
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        
        // Уменьшаем жизнь частицы
        life -= deltaTime;
        
        // Уменьшаем размер по мере затухания
        size = maxSize * (life / maxLife);
        
        // Замедляем частицу
        dx *= 0.95f;
        dy *= 0.95f;
    }
    
    @Override
    public void draw(ShapeRenderer shapeRenderer, Camera camera) {
        if (life <= 0) return;
        
        // Цвет частицы зависит от оставшейся жизни
        float lifeRatio = life / maxLife;
        
        // Красивые цвета огня - от ярко-оранжевого к красному
        float r = 1.0f;
        float g = 0.2f + lifeRatio * 0.6f; // От красного к оранжевому
        float b = lifeRatio * 0.1f; // Немного голубого для реалистичности
        
        shapeRenderer.setColor(r, g, b, lifeRatio);
        
        float screenX = camera.worldToScreenX(x);
        float screenY = camera.worldToScreenY(y);
        
        // Масштабируем размер частицы в зависимости от зума камеры
        float scaledSize = size * camera.getCurrentZoom();
        
        // Рисуем красивый огненный след - несколько кругов разного размера
        shapeRenderer.circle(screenX, screenY, scaledSize);
        
        // Внутренний яркий центр
        shapeRenderer.setColor(1.0f, 0.8f, 0.3f, lifeRatio * 0.7f);
        shapeRenderer.circle(screenX, screenY, scaledSize * 0.6f);
        
        // Самый яркий центр
        shapeRenderer.setColor(1.0f, 1.0f, 0.5f, lifeRatio * 0.5f);
        shapeRenderer.circle(screenX, screenY, scaledSize * 0.3f);
    }
    
    @Override
    public boolean shouldRemove() {
        return life <= 0;
    }
}
