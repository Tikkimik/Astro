package com.gdx.entities;

final class HealthState {
    private int health;
    private int maxHealth;

    HealthState(int health) {
        restore(health);
        maxHealth = health;
    }

    int getHealth() {
        return health;
    }

    int getMaxHealth() {
        return maxHealth;
    }

    void restore(int health) {
        if (health <= 0) throw new IllegalArgumentException("Health must be positive");
        this.health = health;
        this.maxHealth = Math.max(maxHealth, health);
    }

    void damage(int damage) {
        health -= damage;
    }

    boolean isDepleted() {
        return health <= 0;
    }
}
