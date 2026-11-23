package com.gdx.entities;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.gdx.managers.Camera;

public abstract class Enemy extends SpaceObject implements GameObject.Updatable, Damageable, Targetable {

    private final HealthState healthState;
    
    public Enemy(float x, float y, int health) {
        this.x = x;
        this.y = y;
        this.healthState = new HealthState(health);
    }

    public int getHealth() {
        return healthState.getHealth();
    }

    public int getMaxHealth() {
        return healthState.getMaxHealth();
    }

    public void setHealth(int health) {
        healthState.restore(health);
    }

    public void takeDamage(int damage) {
        healthState.damage(damage);
    }

    public boolean shouldRemove() {
        return healthState.isDepleted();
    }

    public abstract int getWidth();

    public abstract int getHeight();

    public abstract void draw(ShapeRenderer shapeRenderer, Camera camera);

    public abstract void update(float dt);

    public boolean intersects(SpaceObject other) {
        return intersectsByRadius(other);
    }
}
