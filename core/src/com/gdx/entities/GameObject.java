package com.gdx.entities;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.gdx.managers.Camera;

/**
 * GameObject - обертка для SpaceObject с системой тегов
 * Позволяет эффективно управлять всеми игровыми объектами
 */
public class GameObject {
    
    public enum Type {
        PLAYER,          // Игрок
        PROJECTILE,      // Пули, ракеты, снаряды
        OBSTACLE,        // Астероиды, препятствия
        ENEMY,           // Враги, вражеские корабли
        PARTICLE,        // Частицы, эффекты
        POWERUP          // Бонусы, улучшения
    }
    
    private SpaceObject object;
    private Type type;
    private boolean active = true;
    private boolean remove = false;
    
    public GameObject(SpaceObject object, Type type) {
        this.object = object;
        this.type = type;
    }
    
    // Геттеры
    public SpaceObject getObject() { return object; }
    public Type getType() { return type; }
    public boolean isActive() { return active; }
    public boolean shouldRemove() { return remove; }
    
    // Сеттеры
    public void setActive(boolean active) { this.active = active; }
    public void markForRemoval() { this.remove = true; }
    
    // Делегирование методов SpaceObject
    public float getX() { return object.getX(); }
    public float getY() { return object.getY(); }
    public float getDx() { return object.getDx(); }
    public float getDy() { return object.getDy(); }
    public float getRadians() { return object.getRadians(); }
    public float getSpeed() { return object.getSpeed(); }
    public float getRotationSpeed() { return object.getRotationSpeed(); }
    public int getWidth() { return object.getWidth(); }
    public int getHeight() { return object.getHeight(); }
    public float[] getShapeX() { return object.getShapeX(); }
    public float[] getShapeY() { return object.getShapeY(); }
    
    // Проверка типа
    public boolean isType(Type type) { return this.type == type; }
    public boolean isProjectile() { return type == Type.PROJECTILE; }
    public boolean isObstacle() { return type == Type.OBSTACLE; }
    public boolean isEnemy() { return type == Type.ENEMY; }
    public boolean isParticle() { return type == Type.PARTICLE; }
    
    // Проверка конкретных классов
    public boolean isAsteroid() { return object instanceof Asteroid; }
    public boolean isOrkAsteroid() { return object instanceof OrkAsteroid; }
    public boolean isBullet() { return object instanceof Bullet; }
    public boolean isOrkBullet() { return object instanceof OrkBullet; }
    public boolean isAutoRocket() { return object instanceof AutoRocket; }
    public boolean isEnemyShip() { return object instanceof EnemyShip; }
    public boolean isPlayer() { return object instanceof Player; }
    
    // Каст к конкретным типам (безопасный)
    @SuppressWarnings("unchecked")
    public <T extends SpaceObject> T as(Class<T> clazz) {
        if (clazz.isInstance(object)) {
            return (T) object;
        }
        return null;
    }
    
    // Проверка коллизий
    public boolean intersects(SpaceObject other) {
        return object.intersects(other);
    }
    
    public boolean intersects(GameObject other) {
        return object.intersects(other.getObject());
    }
    
    // Обновление и отрисовка
    public void update(float dt) {
        if (object instanceof Updatable) {
            ((Updatable) object).update(dt);
            if (object.shouldRemove()) {
                markForRemoval();
            }
        }
    }
    
    public void draw(ShapeRenderer shapeRenderer, Camera camera, SpriteBatch spriteBatch) {
        // Быстрый диспатч без рефлексии (виртуальные вызовы вместо поиска метода в рантайме)
        if (object instanceof Player) {
            ((Player) object).draw(shapeRenderer, camera, spriteBatch);
        } else if (object instanceof Drawable) {
            ((Drawable) object).draw(shapeRenderer, camera);
        } else {
            System.out.println("Warning: Object " + object.getClass().getSimpleName() + " has no compatible draw method");
        }
    }
    
    // Перегрузка для обратной совместимости
    public void draw(ShapeRenderer shapeRenderer, Camera camera) {
        draw(shapeRenderer, camera, null);
    }
    
    // Интерфейсы для объектов с методами update/draw
    public interface Updatable {
        void update(float dt);
    }
    
    public interface Drawable {
        void draw(ShapeRenderer shapeRenderer, Camera camera);
    }
}
