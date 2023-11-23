package com.gdx.entities;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

public class Particle extends SpaceObject {

    private float timer;
    private final float time;
    private boolean remove;

    public Particle(float x, float y, int time) {
        this.x = x;
        this.y = y;
        width = height = 2;

        speed = MathUtils.random(25, 60);
        radians = MathUtils.random(2 * MathUtils.PI);
        dx = MathUtils.cos(radians) * speed;
        dy = MathUtils.sin(radians) * speed;

        timer = 0;
        this.time = time;
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

    public void draw(ShapeRenderer shapeRenderer) {
        shapeRenderer.setColor(1,1,1,1);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.circle(x - width / 2, y - width / 2, width / 2);
        shapeRenderer.end();
    }
}