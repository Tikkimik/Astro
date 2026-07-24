package com.gdx.entities;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.gdx.managers.Camera;

public class OrkBullet extends SpaceObject implements GameObject.Updatable {
    
    private float speed = 250f;
    private float lifeTime = 4f;
    private float lifeTimer = 0f;
    private boolean remove = false;
    
    public OrkBullet(float x, float y, float angle) {
        init(x, y, angle);
    }
    
    // Метод инициализации для пула объектов
    public void init(float x, float y, float angle) {
        this.x = x;
        this.y = y;
        this.radians = angle;
        this.remove = false;
        this.lifeTimer = 0f;
        
        // Задаем направление движения
        dx = MathUtils.cos(angle) * speed;
        dy = MathUtils.sin(angle) * speed;
        
        width = height = 2;
    }
    
    public void update(float dt) {
        // Обновляем позицию
        x += dx * dt;
        y += dy * dt;
        
        // Обновляем таймер жизни
        lifeTimer += dt;
        if (lifeTimer > lifeTime) {
            remove = true;
        }
        
        // Проверяем границы мира
        wrap();
    }
    
    public boolean shouldRemove() {
        return remove;
    }
    
    public void draw(ShapeRenderer shapeRenderer, Camera camera) {
        float screenX = camera.worldToScreenX(x);
        float screenY = camera.worldToScreenY(y);
        
        // Масштабируем размеры с зумом
        float scaledWidth = width * camera.getCurrentZoom();
        float scaledHeight = height * camera.getCurrentZoom();
        
        // Рисуем пулю орка как красный квадрат
        shapeRenderer.setColor(0.8f, 0.2f, 0.2f, 1); // Красный цвет
        shapeRenderer.rect(screenX - scaledWidth/2, screenY - scaledHeight/2, scaledWidth, scaledHeight);
    }
    
    public boolean intersects(SpaceObject other) {
        float dx = x - other.getX();
        float dy = y - other.getY();
        float distSq = dx * dx + dy * dy;
        float radiusSum = width / 2f + other.getWidth() / 2f;
        return distSq < radiusSum * radiusSum;
    }

    // Проверяем столкновение с игроком (оптимизировано без Math.sqrt)
    public boolean intersects(Player player) {
        float dx = x - player.getX();
        float dy = y - player.getY();
        float distanceSq = dx * dx + dy * dy;
        float radiusSum = player.getWidth() / 2 + width / 2;
        return distanceSq < radiusSum * radiusSum;
    }
}
