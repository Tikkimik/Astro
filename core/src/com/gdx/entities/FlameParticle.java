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
        this.angle = angle;
        this.speed = speed;
        this.maxLife = 0.3f + MathUtils.random() * 0.2f; // 0.3-0.5 секунды
        this.life = maxLife;
        this.maxSize = 3f + MathUtils.random() * 4f; // 3-7 пикселей
        this.size = maxSize;
        
        // Начальная скорость частицы
        this.dx = MathUtils.cos(angle) * speed;
        this.dy = MathUtils.sin(angle) * speed;
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
        
        // Яркие цвета огня как у истребителей
        float r = 1.0f;
        float g = 0.3f + lifeRatio * 0.7f; // От красного к желтому
        float b = lifeRatio * 0.8f; // От черного к голубому
        
        shapeRenderer.setColor(r, g, b, lifeRatio);
        
        float screenX = camera.worldToScreenX(x);
        float screenY = camera.worldToScreenY(y);
        
        // Масштабируем размер частицы в зависимости от зума камеры
        float scaledSize = size * camera.getCurrentZoom();
        shapeRenderer.circle(screenX, screenY, scaledSize);
    }
    
    @Override
    public boolean shouldRemove() {
        return life <= 0;
    }
}
