package com.gdx.managers;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.gdx.entities.*;
import com.gdx.managers.Camera;
import com.gdx.utils.GameLogger;

/**
 * GameObjectManager - единый менеджер всех игровых объектов
 * Заменяет множественные списки в PlayState
 */
public class GameObjectManager {
    
    // Единый список всех активных объектов
    private Array<GameObject> allObjects = new Array<>();
    
    // Быстрые списки по типам (для оптимизации)
    private Array<GameObject> projectiles = new Array<>();
    private Array<GameObject> obstacles = new Array<>();
    private Array<GameObject> enemies = new Array<>();
    private Array<GameObject> particles = new Array<>();
    private Array<RocketParticle> rocketParticles = new Array<>();
    
    // Специальные объекты
    private GameObject player;
    
    // Listener для событий уничтожения астероидов
    public interface AsteroidDestroyedListener {
        void onAsteroidDestroyed(Asteroid asteroid, float x, float y);
    }
    private AsteroidDestroyedListener asteroidDestroyedListener;
    
    public void setAsteroidDestroyedListener(AsteroidDestroyedListener listener) {
        this.asteroidDestroyedListener = listener;
    }
    
    // Статистика
    private int totalObjects = 0;
    private int activeObjects = 0;
    
    public GameObjectManager() {
        GameLogger.info("GameObjectManager initialized");
    }
    
    // === ДОБАВЛЕНИЕ ОБЪЕКТОВ ===
    
    /**
     * Добавить объект в менеджер
     */
    public void addObject(SpaceObject object, GameObject.Type type) {
        GameObject gameObject = new GameObject(object, type);
        allObjects.add(gameObject);
        
        // Добавляем в соответствующий быстрый список
        switch (type) {
            case PROJECTILE:
                projectiles.add(gameObject);
                break;
            case OBSTACLE:
                obstacles.add(gameObject);
                break;
            case ENEMY:
                enemies.add(gameObject);
                break;
            case PARTICLE:
                particles.add(gameObject);
                break;
            case PLAYER:
                player = gameObject;
                break;
        }
        
        totalObjects++;
        activeObjects++;
        
        GameLogger.debug("Added " + type + " object, total: " + totalObjects);
    }
    
    /**
     * Добавить пулю
     */
    public void addBullet(Bullet bullet) {
        addObject(bullet, GameObject.Type.PROJECTILE);
    }
    
    /**
     * Добавить астероид
     */
    public void addAsteroid(Asteroid asteroid) {
        addObject(asteroid, GameObject.Type.OBSTACLE);
    }
    
    /**
     * Добавить орк-астероид
     */
    public void addOrkAsteroid(OrkAsteroid orkAsteroid) {
        orkAsteroid.setGameObjectManager(this);
        addObject(orkAsteroid, GameObject.Type.OBSTACLE);
    }
    
    /**
     * Добавить орк-пулю
     */
    public void addOrkBullet(OrkBullet orkBullet) {
        addObject(orkBullet, GameObject.Type.PROJECTILE);
    }
    
    /**
     * Добавить автоматическую ракету
     */
    public void addAutoRocket(AutoRocket autoRocket) {
        autoRocket.setGameObjectManager(this);
        addObject(autoRocket, GameObject.Type.PROJECTILE);
    }

    public void addRocketParticle(RocketParticle particle) {
        // Следы ракет тоже ограничены глобальным лимитом частиц
        if (!com.gdx.utils.ParticleBudget.canSpawn(1)) {
            return;
        }
        rocketParticles.add(particle);
        com.gdx.utils.ParticleBudget.add(1);
    }
    
    /**
     * Добавить вражеский корабль
     */
    public void addEnemyShip(EnemyShip enemyShip) {
        enemyShip.setGameObjectManager(this);
        addObject(enemyShip, GameObject.Type.ENEMY);
    }
    
    /**
     * Добавить частицу
     */
    public void addParticle(Particle particle) {
        addObject(particle, GameObject.Type.PARTICLE);
    }
    
    /**
     * Установить игрока
     */
    public void setPlayer(Player player) {
        this.player = new GameObject(player, GameObject.Type.PLAYER);
        allObjects.add(this.player);
        totalObjects++;
        activeObjects++;
    }
    
    // === ПОЛУЧЕНИЕ ОБЪЕКТОВ ===
    
    /**
     * Получить все объекты
     */
    public Array<GameObject> getAllObjects() {
        return allObjects;
    }
    
    /**
     * Получить все снаряды
     */
    public Array<GameObject> getProjectiles() {
        return projectiles;
    }
    
    /**
     * Получить все препятствия
     */
    public Array<GameObject> getObstacles() {
        return obstacles;
    }
    
    /**
     * Получить всех врагов
     */
    public Array<GameObject> getEnemies() {
        return enemies;
    }
    
    /**
     * Получить все частицы
     */
    public Array<GameObject> getParticles() {
        return particles;
    }
    
    /**
     * Получить игрока
     */
    public GameObject getPlayer() {
        return player;
    }
    
    /**
     * Получить игрока как Player
     */
    public Player getPlayerObject() {
        return player != null ? player.as(Player.class) : null;
    }
    
    // === ОБНОВЛЕНИЕ ===
    
    /**
     * Обновить все объекты
     */
    public void update(float dt) {
        // Обновляем все объекты
        for (int i = allObjects.size - 1; i >= 0; i--) {
            GameObject obj = allObjects.get(i);
            
            if (obj.shouldRemove()) {
                removeObject(obj);
                continue;
            }
            
            if (obj.isActive()) {
                obj.update(dt);
            }
        }
        
        // Обновляем независимые частицы ракет
        for (int i = rocketParticles.size - 1; i >= 0; i--) {
            RocketParticle p = rocketParticles.get(i);
            p.update(dt);
            if (p.shouldRemove()) {
                rocketParticles.removeIndex(i);
                com.gdx.utils.ParticleBudget.release(1);
            }
        }
        
    }
    
    // === ОТРИСОВКА ===
    
    /**
     * Отрисовать все объекты
     */
    public void draw(ShapeRenderer shapeRenderer, Camera camera, SpriteBatch spriteBatch) {
        for (GameObject obj : allObjects) {
            if (obj.isActive()) {
                obj.draw(shapeRenderer, camera, spriteBatch);
            }
        }
        for (RocketParticle p : rocketParticles) {
            p.draw(shapeRenderer, camera);
        }
    }
    
    // Перегрузка для обратной совместимости
    public void draw(ShapeRenderer shapeRenderer, Camera camera) {
        draw(shapeRenderer, camera, null);
    }
    
    // === КОЛЛИЗИИ ===
    
    /**
     * Проверить коллизии между всеми объектами
     */
    public void checkCollisions() {
        // Коллизии снарядов с препятствиями
        checkProjectileObstacleCollisions();
        
        // Коллизии снарядов с врагами
        checkProjectileEnemyCollisions();
        
        // Коллизии игрока с препятствиями
        checkPlayerObstacleCollisions();
        
        // Коллизии игрока с врагами
        checkPlayerEnemyCollisions();
    }
    
    private void checkProjectileObstacleCollisions() {
        for (GameObject projectile : projectiles) {
            if (!projectile.isActive()) continue;
            
            for (GameObject obstacle : obstacles) {
                if (!obstacle.isActive()) continue;
                
                if (projectile.intersects(obstacle)) {
                    handleProjectileObstacleCollision(projectile, obstacle);
                }
            }
        }
    }
    
    private void checkProjectileEnemyCollisions() {
        for (GameObject projectile : projectiles) {
            if (!projectile.isActive()) continue;
            
            for (GameObject enemy : enemies) {
                if (!enemy.isActive()) continue;
                
                if (projectile.intersects(enemy)) {
                    handleProjectileEnemyCollision(projectile, enemy);
                }
            }
        }
    }
    
    private void checkPlayerObstacleCollisions() {
        if (player == null || !player.isActive()) return;
        
        for (GameObject obstacle : obstacles) {
            if (!obstacle.isActive()) continue;
            
            if (player.intersects(obstacle)) {
                handlePlayerObstacleCollision(player, obstacle);
            }
        }
    }
    
    private void checkPlayerEnemyCollisions() {
        if (player == null || !player.isActive()) return;
        
        for (GameObject enemy : enemies) {
            if (!enemy.isActive()) continue;
            
            if (player.intersects(enemy)) {
                handlePlayerEnemyCollision(player, enemy);
            }
        }
    }
    
    // === ОБРАБОТКА КОЛЛИЗИЙ ===
    
    private void handleProjectileObstacleCollision(GameObject projectile, GameObject obstacle) {
        projectile.markForRemoval();
        
        if (obstacle.isAsteroid()) {
            Asteroid asteroid = obstacle.as(Asteroid.class);
            if (asteroid != null) {
                asteroid.takeDamage(1);
                if (asteroid.shouldRemove()) {
                    obstacle.markForRemoval();
                    splitAsteroid(asteroid);
                }
            }
        } else if (obstacle.isOrkAsteroid()) {
            OrkAsteroid orkAsteroid = obstacle.as(OrkAsteroid.class);
            if (orkAsteroid != null) {
                orkAsteroid.takeDamage(1);
                if (orkAsteroid.shouldRemove()) {
                    obstacle.markForRemoval();
                }
            }
        }
    }

    public void splitAsteroid(Asteroid asteroid) {
        if (asteroid.getType() == Asteroid.LARGE) {
            addAsteroid(new Asteroid(asteroid.getX(), asteroid.getY(), Asteroid.MEDIUM));
            addAsteroid(new Asteroid(asteroid.getX(), asteroid.getY(), Asteroid.MEDIUM));
        } else if (asteroid.getType() == Asteroid.MEDIUM) {
            addAsteroid(new Asteroid(asteroid.getX(), asteroid.getY(), Asteroid.SMALL));
            addAsteroid(new Asteroid(asteroid.getX(), asteroid.getY(), Asteroid.SMALL));
        }
        if (asteroidDestroyedListener != null) {
            asteroidDestroyedListener.onAsteroidDestroyed(asteroid, asteroid.getX(), asteroid.getY());
        }
    }
    
    private void handleProjectileEnemyCollision(GameObject projectile, GameObject enemy) {
        // Уничтожаем снаряд
        projectile.markForRemoval();
        
        // Наносим урон врагу
        if (enemy.isEnemyShip()) {
            EnemyShip enemyShip = enemy.as(EnemyShip.class);
            if (enemyShip != null) {
                enemyShip.takeDamage(1);
                if (enemyShip.shouldRemove()) {
                    enemy.markForRemoval();
                }
            }
        }
    }
    
    private void handlePlayerObstacleCollision(GameObject player, GameObject obstacle) {
        // Логика столкновения игрока с препятствием
        GameLogger.debug("Player collided with obstacle");
    }
    
    private void handlePlayerEnemyCollision(GameObject player, GameObject enemy) {
        // Логика столкновения игрока с врагом
        GameLogger.debug("Player collided with enemy");
    }
    
    // === УДАЛЕНИЕ ОБЪЕКТОВ ===
    
    private void removeObject(GameObject obj) {
        // Возвращаем авто-ракеты в пул при удалении
        if (obj.getObject() instanceof AutoRocket) {
            com.gdx.utils.ObjectPools.freeAutoRocket((AutoRocket) obj.getObject());
        }

        allObjects.removeValue(obj, true);
        
        // Удаляем из быстрых списков
        projectiles.removeValue(obj, true);
        obstacles.removeValue(obj, true);
        enemies.removeValue(obj, true);
        particles.removeValue(obj, true);
        
        if (obj == player) {
            player = null;
        }
        
        activeObjects--;
        GameLogger.debug("Removed object, active: " + activeObjects);
    }
    
    // === ОЧИСТКА ===
    
    /**
     * Очистить все объекты
     */
    public void clear() {
        allObjects.clear();
        projectiles.clear();
        obstacles.clear();
        enemies.clear();
        particles.clear();
        rocketParticles.clear();
        player = null;
        
        totalObjects = 0;
        activeObjects = 0;
        
        // Счётчик активных частиц принадлежит миру, сбрасываем вместе с ним
        com.gdx.utils.ParticleBudget.reset();
        
        GameLogger.info("GameObjectManager cleared");
    }
    
    // === СТАТИСТИКА ===
    
    public int getTotalObjects() { return totalObjects; }
    public int getActiveObjects() { return activeObjects; }
    public int getProjectileCount() { return projectiles.size; }
    public int getObstacleCount() { return obstacles.size; }
    public int getEnemyCount() { return enemies.size; }
    public int getRocketParticleCount() { return rocketParticles.size; }
    
    /**
     * Количество реально активных частиц, принадлежащих менеджеру:
     * следы ракет (rocketParticles) + пламя двигателей вражеских кораблей
     * + пламя и щит игрока. Частицы в пулах (ParticlePool) не учитываются.
     */
    public int getActiveParticleCount() {
        int count = rocketParticles.size;
        for (GameObject obj : enemies) {
            EnemyShip es = obj.as(EnemyShip.class);
            if (es != null) {
                count += es.getFlameParticles().size;
            }
        }
        if (player != null) {
            Player p = player.as(Player.class);
            if (p != null) {
                count += p.getFlameParticles().size;
                count += p.getShieldParticles().size;
            }
        }
        return count;
    }
    
    /**
     * Получить статистику
     */
    public String getStats() {
        return String.format("Active: %d / Created: %d | Projectiles: %d | Obstacles: %d | Enemies: %d | Particles: %d",
            activeObjects, totalObjects, getProjectileCount(), getObstacleCount(), getEnemyCount(), getActiveParticleCount());
    }
}
