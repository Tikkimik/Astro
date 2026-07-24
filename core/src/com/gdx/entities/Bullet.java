package com.gdx.entities;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.gdx.managers.Camera;

public class Bullet extends SpaceObject implements GameObject.Updatable {

    private float lifeTime;
    private float lifeTimer;
    private boolean remove;
    private float damage = 1f;
    private float speed = 350f;

    public Bullet(float x, float y, float radians){
        init(x, y, radians);
    }
    
    // Метод инициализации для пула объектов
    public void init(float x, float y, float radians){
        this.x = x;
        this.y = y;
        this.radians = radians;
        this.remove = false;
        this.lifeTimer = 0;
        this.lifeTime = 1f;
        this.damage = 1f;

        dx = MathUtils.cos(radians) * speed;
        dy = MathUtils.sin(radians) * speed;

        width = 2;
        height = 2;
    }

    public boolean shouldRemove(){
        return remove;
    }
    
    // Методы для работы с уроном
    public void setDamage(float damage) {
        this.damage = damage;
    }
    
    public float getDamage() {
        return damage;
    }

    public boolean intersects(SpaceObject other) {
        float dx = x - other.getX();
        float dy = y - other.getY();
        float distSq = dx * dx + dy * dy;
        float radiusSum = width / 2f + other.getWidth() / 2f;
        return distSq < radiusSum * radiusSum;
    }

    public void update(float dt){
        x += dx * dt;
        y += dy * dt;

        wrap();

        lifeTimer += dt;
        if(lifeTimer > lifeTime){
            remove = true;
        }
    }

    public void draw(ShapeRenderer shapeRenderer, Camera camera){
        shapeRenderer.setColor(1,1,1,1);
        
        float screenX = camera.worldToScreenX(x);
        float screenY = camera.worldToScreenY(y);
        float scaledRadius = (width / 2) * camera.getCurrentZoom();
        shapeRenderer.circle(screenX, screenY, scaledRadius);
    }
}
