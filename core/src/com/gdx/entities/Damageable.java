package com.gdx.entities;

public interface Damageable {
    int getHealth();
    int getMaxHealth();
    void takeDamage(int damage);
    boolean shouldRemove();
}
