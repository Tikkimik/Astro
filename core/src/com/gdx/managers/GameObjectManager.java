package com.gdx.managers;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.gdx.entities.*;
import com.gdx.managers.Camera;
import com.gdx.utils.GameLogger;
import com.gdx.utils.RenderStats;

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
        com.gdx.utils.ParticleBudget.register(rocketParticles);
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
        // След ракеты — чисто декоративный эффект: не создаём частицы, если ракета
        // далеко за пределами расширенной области спавна (след всё равно не виден).
        if (!com.gdx.utils.ParticleBudget.isInSpawnBounds(particle.getX(), particle.getY())) {
            return;
        }
        if (!com.gdx.utils.ParticleBudget.canSpawn(particle.getX(), particle.getY())) {
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
     * Отрисовать все объекты с отсечением (culling) по расширенной области камеры.
     * Объекты за пределами камеры в renderer не отправляются. Счётчики статистики
     * заполняются непосредственно перед фактическим вызовом draw.
     */
    public void draw(ShapeRenderer shapeRenderer, Camera camera, SpriteBatch spriteBatch, RenderStats stats) {
        drawProjectiles(shapeRenderer, camera, spriteBatch, stats);
        drawObstacles(shapeRenderer, camera, spriteBatch, stats);
        drawEnemies(shapeRenderer, camera, spriteBatch, stats);
        drawRocketParticles(shapeRenderer, camera, stats);
    }

    private void drawProjectiles(ShapeRenderer shapeRenderer, Camera camera, SpriteBatch spriteBatch, RenderStats stats) {
        for (GameObject obj : projectiles) {
            if (!obj.isActive()) continue;
            stats.projectilesActive++;
            float radius = Math.max(1f, obj.getWidth() / 2f);
            if (camera.isInRenderBounds(obj.getX(), obj.getY(), radius)) {
                stats.projectilesVisible++;
                obj.draw(shapeRenderer, camera, spriteBatch);
                stats.projectilesDrawn++;
            } else {
                stats.projectilesCulled++;
            }
        }
    }

    private void drawObstacles(ShapeRenderer shapeRenderer, Camera camera, SpriteBatch spriteBatch, RenderStats stats) {
        for (GameObject obj : obstacles) {
            if (!obj.isActive()) continue;
            stats.obstaclesActive++;
            float radius = Math.max(1f, obj.getWidth() / 2f);
            if (camera.isInRenderBounds(obj.getX(), obj.getY(), radius)) {
                stats.obstaclesVisible++;
                obj.draw(shapeRenderer, camera, spriteBatch);
                stats.obstaclesDrawn++;
            } else {
                stats.obstaclesCulled++;
            }
        }
    }

    private void drawEnemies(ShapeRenderer shapeRenderer, Camera camera, SpriteBatch spriteBatch, RenderStats stats) {
        for (GameObject obj : enemies) {
            if (!obj.isActive()) continue;
            stats.enemiesActive++;
            float radius = Math.max(1f, obj.getWidth() / 2f);
            boolean shipInView = camera.isInRenderBounds(obj.getX(), obj.getY(), radius);
            if (shipInView) {
                stats.enemiesVisible++;
                obj.draw(shapeRenderer, camera, spriteBatch);
                stats.enemiesDrawn++;
            } else {
                stats.enemiesCulled++;
            }
            // Пламя двигателя считаем поштучно: внеэкранные частицы не должны
            // расходовать видимый бюджет и забирать слоты у эффектов на экране.
            countEnemyFlameParticles(obj, camera, stats, shipInView);
        }
    }

    private void countEnemyFlameParticles(GameObject obj, Camera camera, RenderStats stats, boolean shipInView) {
        EnemyShip es = obj.as(EnemyShip.class);
        if (es == null) return;
        for (EnemyFlameParticle fp : es.getFlameParticles()) {
            stats.particlesActive++;
            if (camera.isInRenderBounds(fp.getX(), fp.getY(), fp.getCullRadius())) {
                stats.particlesVisible++;
                if (shipInView) {
                    stats.particlesDrawn++;
                }
            } else {
                stats.particlesCulled++;
            }
        }
    }

    private void drawRocketParticles(ShapeRenderer shapeRenderer, Camera camera, RenderStats stats) {
        for (RocketParticle p : rocketParticles) {
            stats.particlesActive++;
            if (camera.isInRenderBounds(p.getX(), p.getY(), p.getCullRadius())) {
                stats.particlesVisible++;
                p.draw(shapeRenderer, camera);
                stats.particlesDrawn++;
            } else {
                stats.particlesCulled++;
            }
        }
    }

    // Перегрузка для обратной совместимости
    public void draw(ShapeRenderer shapeRenderer, Camera camera, SpriteBatch spriteBatch) {
        draw(shapeRenderer, camera, spriteBatch, new RenderStats());
    }
    
    // Перегрузка для обратной совместимости
    public void draw(ShapeRenderer shapeRenderer, Camera camera) {
        draw(shapeRenderer, camera, null, new RenderStats());
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
        
        // Коллизии вражеских снарядов с игроком
        checkProjectilePlayerCollisions();
        
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
            
            // Пули врагов не задевают вражеские корабли (без дружественного огня):
            // иначе корабль убивает сам себя, т.к. OrkBullet спавнится в его центре
            if (projectile.isOrkBullet()) continue;
            
            for (GameObject enemy : enemies) {
                if (!enemy.isActive()) continue;
                
                if (projectile.intersects(enemy)) {
                    handleProjectileEnemyCollision(projectile, enemy);
                }
            }
        }
    }
    
    private void checkProjectilePlayerCollisions() {
        if (player == null || !player.isActive()) return;
        
        Player p = player.as(Player.class);
        if (p == null) return;
        
        for (GameObject projectile : projectiles) {
            if (!projectile.isActive()) continue;
            
            // Игрока задевают только вражеские пули
            if (!projectile.isOrkBullet()) continue;
            
            if (projectile.intersects(player)) {
                projectile.markForRemoval();
                p.hit();
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

        // Освобождаем оставшиеся частицы пламени вражеского корабля и их слоты
        // бюджета: без этого счётчик активных частиц «протекал» при гибели врагов.
        if (obj.getObject() instanceof EnemyShip) {
            ((EnemyShip) obj.getObject()).dispose();
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
