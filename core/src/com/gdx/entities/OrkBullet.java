package com.gdx.entities;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.gdx.managers.Camera;

public class OrkBullet extends SpaceObject {
    
    private float speed = 250f;
    private float lifeTime = 4f;
    private float lifeTimer = 0f;
    private boolean remove = false;
    
    public OrkBullet(float x, float y, float angle) {
        this.x = x;
        this.y = y;
        this.radians = angle;
        
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
        
        // Рисуем пулю орка как красный квадрат
        shapeRenderer.setColor(0.8f, 0.2f, 0.2f, 1); // Красный цвет
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.rect(screenX - width/2, screenY - height/2, width, height);
        shapeRenderer.end();
        
        // Рисуем контур
        shapeRenderer.setColor(1, 0.5f, 0.5f, 1); // Светло-красный контур
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.rect(screenX - width/2, screenY - height/2, width, height);
        shapeRenderer.end();
    }
    
    // Проверяем столкновение с игроком
    public boolean intersects(Player player) {
        float dx = x - player.getX();
        float dy = y - player.getY();
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        return distance < player.getWidth() / 2 + width / 2;
    }
}
