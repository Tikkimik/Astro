package com.gdx.entities;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.gdx.managers.Camera;

public class Particle extends SpaceObject implements GameObject.Drawable {

    private float timer;
    private float time;
    private boolean remove;

    public Particle(float x, float y) {
        init(x, y);
    }
    
    // Метод инициализации для пула объектов
    public void init(float x, float y) {
        this.x = x;
        this.y = y;
        this.remove = false;
        this.timer = 0;
        this.time = 1f;

        width = height = 2;
        speed = 50;
        radians = MathUtils.random(2 * MathUtils.PI);
        dx = MathUtils.cos(radians) * speed;
        dy = MathUtils.sin(radians) * speed;
    }

    public boolean shouldRemove() {
        return remove;
    }

    public void update(float dt){
        x += dx * dt;
        y += dy * dt;

        timer += dt;

        if(timer > time) {
            remove = true;
        }
    }

    public void draw(ShapeRenderer shapeRenderer, Camera camera) {
        shapeRenderer.setColor(1,1,1,1);
        
        float screenX = camera.worldToScreenX(x - width / 2);
        float screenY = camera.worldToScreenY(y - width / 2);
        shapeRenderer.circle(screenX, screenY, width / 2);
    }
}