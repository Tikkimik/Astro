package com.gdx.entities;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.gdx.managers.Camera;

/**
 * Базовый класс для всех врагов в игре
 * Наследуется от SpaceObject и добавляет функциональность врага
 */
public abstract class Enemy extends SpaceObject {
    
    protected boolean remove = false;
    protected int health;
    protected int maxHealth;
    
    public Enemy(float x, float y, int health) {
        this.x = x;
        this.y = y;
        this.health = health;
        this.maxHealth = health;
    }
    
    /**
     * Получить здоровье врага
     */
    public int getHealth() {
        return health;
    }
    
    /**
     * Получить максимальное здоровье
     */
    public int getMaxHealth() {
        return maxHealth;
    }
    
    /**
     * Нанести урон врагу
     */
    public void takeDamage(int damage) {
        health -= damage;
        if (health <= 0) {
            remove = true;
        }
    }
    
    /**
     * Проверить, должен ли враг быть удален
     */
    public boolean shouldRemove() {
        return remove;
    }
    
    /**
     * Получить ширину для коллизий
     */
    public abstract int getWidth();
    
    /**
     * Получить высоту для коллизий
     */
    public abstract int getHeight();
    
    /**
     * Отрисовка врага
     */
    public abstract void draw(ShapeRenderer shapeRenderer, Camera camera);
    
    /**
     * Проверка коллизии с другим объектом (оптимизированная)
     */
    public boolean intersects(SpaceObject other) {
        float dx = x - other.getX();
        float dy = y - other.getY();
        float distanceSquared = dx * dx + dy * dy;
        float radiusSum = getWidth() / 2 + other.getWidth() / 2;
        return distanceSquared < radiusSum * radiusSum;
    }
    
    /**
     * Получить тип врага (для логики игры)
     */
    public abstract String getEnemyType();
}
