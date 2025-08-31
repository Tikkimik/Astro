package com.gdx.entities;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.gdx.managers.Camera;

public class RocketParticle extends Particle {
    
    private float life;
    private float maxLife;
    private float size;
    
    public RocketParticle(float x, float y, float angle) {
        super(x, y);
        init(x, y, angle);
    }
    
    // Метод инициализации для пула объектов
    public void init(float x, float y, float angle) {
        this.x = x;
        this.y = y;
        this.maxLife = 1.2f + MathUtils.random() * 0.8f; // Время жизни 1.2-2.0 секунды (дольше)
        this.life = maxLife;
        this.size = 1.5f + MathUtils.random() * 1.0f; // Размер 1.5-2.5 пикселя (увеличили для более заметного следа)
        
        // Движение в случайном направлении с небольшим разбросом
        float spreadAngle = angle + (MathUtils.random() - 0.5f) * 0.6f; // Меньший разброс угла
        float speed = 20 + MathUtils.random() * 25; // Скорость 20-45 (медленнее)
        
        this.dx = MathUtils.cos(spreadAngle) * speed;
        this.dy = MathUtils.sin(spreadAngle) * speed;
    }
    
    // Метод инициализации для пула объектов (переопределение базового)
    public void init(float x, float y) {
        init(x, y, MathUtils.random(2 * MathUtils.PI));
    }
    
    @Override
    public void update(float dt) {
        super.update(dt);
        life -= dt;
        
        // Более плавное замедление частиц со временем
        dx *= 0.995f;
        dy *= 0.995f;
    }
    
    @Override
    public void draw(ShapeRenderer shapeRenderer, Camera camera) {
        float screenX = camera.worldToScreenX(x);
        float screenY = camera.worldToScreenY(y);
        float scaledSize = size * camera.getCurrentZoom(); // Масштабируем с зумом
        
        // Плавное исчезновение с кубической интерполяцией для более плавного эффекта
        float lifeRatio = life / maxLife;
        float alpha = lifeRatio * lifeRatio * lifeRatio; // Кубическое затухание для более плавного исчезновения
        
        // Серый цвет с градацией яркости
        float intensity = 0.4f + alpha * 0.6f; // Интенсивность от 0.4 до 1.0
        
        // Серые оттенки - от светло-серого до темно-серого
        shapeRenderer.setColor(intensity, intensity, intensity, alpha);
        
        shapeRenderer.circle(screenX, screenY, scaledSize);
    }
    
    @Override
    public boolean shouldRemove() {
        return life <= 0;
    }
}
