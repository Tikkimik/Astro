package com.gdx.entities;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.gdx.managers.Camera;

public class Bullet extends SpaceObject{

    private float lifeTime;
    private float lifeTimer;
    private boolean remove;

    public Bullet(float x, float y, float radians){
        this.x = x;
        this.y = y;
        this.radians = radians;

        float speed = 350;
        dx = MathUtils.cos(radians) * speed;
        dy = MathUtils.sin(radians) * speed;

        width = 2;
        height = 2;

        lifeTimer = 0;
        lifeTime = 1;
    }

    public boolean shouldRemove(){
        return  remove;
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
