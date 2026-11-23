package com.gdx.managers;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.gdx.combat.DestructionEvent;
import com.gdx.entities.*;
import com.gdx.managers.Camera;
import com.gdx.utils.GameLogger;
import com.gdx.utils.RenderStats;

/**
 * GameObjectManager - единый менеджер всех игровых объектов
 * Заменяет множественные списки в PlayState
 */
public class GameObjectManager {

    private record ProjectileCollision(GameObject projectile, GameObject target) {}

    public static final class Snapshot {
        private final Array<GameObject> allObjects;
        private final Array<GameObject> projectiles;
        private final Array<GameObject> obstacles;
        private final Array<GameObject> enemies;
        private final Array<GameObject> particles;
        private final Array<RocketParticle> rocketParticles;
        private final Player player;
        private final int totalObjects;
        private final int activeObjects;

        private Snapshot(GameObjectManager source) {
            allObjects = new Array<>(source.allObjects);
            projectiles = new Array<>(source.projectiles);
            obstacles = new Array<>(source.obstacles);
            enemies = new Array<>(source.enemies);
            particles = new Array<>(source.particles);
            rocketParticles = new Array<>(source.rocketParticles);
            player = source.player;
            totalObjects = source.totalObjects;
            activeObjects = source.activeObjects;
        }
    }

    // Единый список всех активных объектов
    private Array<GameObject> allObjects = new Array<>();

    // Быстрые списки по типам (для оптимизации)
    private Array<GameObject> projectiles = new Array<>();
    private Array<GameObject> obstacles = new Array<>();
    private Array<GameObject> enemies = new Array<>();
    private Array<GameObject> particles = new Array<>();
    private Array<RocketParticle> rocketParticles = new Array<>();

    // Специальные объекты
    private Player player;

    public interface DestructionListener {
        void onDestroyed(DestructionEvent event);
    }
    private final Array<DestructionListener> destructionListeners = new Array<>();

    public interface RocketExplosionListener {
        void onRocketExplosion(float x, float y, float radius);
    }
    private RocketExplosionListener rocketExplosionListener;

    public interface PlayerObstacleCollisionListener {
        void onPlayerObstacleCollision(float x, float y);
    }
    private PlayerObstacleCollisionListener playerObstacleCollisionListener;

    public void addDestructionListener(DestructionListener listener) {
        destructionListeners.add(listener);
    }

    public void setRocketExplosionListener(RocketExplosionListener listener) {
        this.rocketExplosionListener = listener;
    }

    public void setPlayerObstacleCollisionListener(PlayerObstacleCollisionListener listener) {
        this.playerObstacleCollisionListener = listener;
    }

    // Статистика
    private int totalObjects = 0;
    private int activeObjects = 0;

    public GameObjectManager() {
        GameLogger.info("GameObjectManager initialized");
        com.gdx.utils.ParticleBudget.register(rocketParticles);
    }

    public Snapshot createSnapshot() {
        return new Snapshot(this);
    }

    public void restore(Snapshot snapshot) {
        allObjects.clear();
        allObjects.addAll(snapshot.allObjects);
        projectiles.clear();
        projectiles.addAll(snapshot.projectiles);
        obstacles.clear();
        obstacles.addAll(snapshot.obstacles);
        enemies.clear();
        enemies.addAll(snapshot.enemies);
        particles.clear();
        particles.addAll(snapshot.particles);
        rocketParticles.clear();
        rocketParticles.addAll(snapshot.rocketParticles);
        player = snapshot.player;
        totalObjects = snapshot.totalObjects;
        activeObjects = snapshot.activeObjects;
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
            com.gdx.utils.ParticlePool.freeRocketParticle(particle);
            return;
        }
        if (!com.gdx.utils.ParticleBudget.canSpawn(particle.getX(), particle.getY())) {
            com.gdx.utils.ParticlePool.freeRocketParticle(particle);
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
        if (this.player == player) return;
        if (this.player != null) {
            throw new IllegalStateException("Player is already registered");
        }
        this.player = player;
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

    public Enemy findNearestEnemy(float x, float y, float maxDistance) {
        Enemy nearest = null;
        float nearestDistanceSquared = maxDistance * maxDistance;

        for (GameObject object : obstacles) {
            nearest = nearerEnemy(object, x, y, nearest, nearestDistanceSquared);
            if (nearest != null) nearestDistanceSquared = distanceSquared(nearest, x, y);
        }
        for (GameObject object : enemies) {
            nearest = nearerEnemy(object, x, y, nearest, nearestDistanceSquared);
            if (nearest != null) nearestDistanceSquared = distanceSquared(nearest, x, y);
        }
        return nearest;
    }

    private Enemy nearerEnemy(GameObject object, float x, float y, Enemy current, float currentDistanceSquared) {
        if (object.shouldRemove()) return current;
        Enemy candidate = object.as(Enemy.class);
        if (candidate == null) return current;
        float candidateDistanceSquared = distanceSquared(candidate, x, y);
        return candidateDistanceSquared < currentDistanceSquared
                || (current == null && candidateDistanceSquared == currentDistanceSquared)
                ? candidate : current;
    }

    private float distanceSquared(SpaceObject object, float x, float y) {
        float dx = object.getX() - x;
        float dy = object.getY() - y;
        return dx * dx + dy * dy;
    }

    /**
     * Получить все частицы
     */
    public Array<GameObject> getParticles() {
        return particles;
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

            obj.update(dt);
            if (obj.shouldRemove()) {
                removeObject(obj);
            }
        }

        // Обновляем независимые частицы ракет
        for (int i = rocketParticles.size - 1; i >= 0; i--) {
            RocketParticle p = rocketParticles.get(i);
            p.update(dt);
            if (p.shouldRemove()) {
                rocketParticles.removeIndex(i);
                com.gdx.utils.ParticlePool.freeRocketParticle(p);
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
        // Следы ракет рисуются отдельно через SpriteBatch (аддитивные текстуры),
        // из PlayState — чтобы не смешивать состояние SpriteBatch и ShapeRenderer.
    }

    private void drawProjectiles(ShapeRenderer shapeRenderer, Camera camera, SpriteBatch spriteBatch, RenderStats stats) {
        for (GameObject obj : projectiles) {
            if (obj.shouldRemove()) continue;
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
            if (obj.shouldRemove()) continue;
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
            if (obj.shouldRemove()) continue;
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

    /**
     * Отрисовка следов ракет через SpriteBatch: мягкие аддитивные текстуры.
     * Вызывается из PlayState вне shapeRenderer-блока, чтобы не смешивать
     * GL-состояние двух рендереров.
     */
    public void drawRocketParticles(SpriteBatch spriteBatch, Camera camera, RenderStats stats) {
        if (rocketParticles.size == 0) return;
        spriteBatch.begin();
        spriteBatch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (RocketParticle p : rocketParticles) {
            stats.particlesActive++;
            if (camera.isInRenderBounds(p.getX(), p.getY(), p.getCullRadius())) {
                stats.particlesVisible++;
                p.draw(spriteBatch, camera);
                stats.particlesDrawn++;
            } else {
                stats.particlesCulled++;
            }
        }
        spriteBatch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        spriteBatch.end();
    }

    /**
     * Отрисовка дыма вражеских кораблей через SpriteBatch (мягкая текстура,
     * обычное смешивание). Вызывается из PlayState вне shapeRenderer-блока.
     */
    public void drawEnemySmoke(SpriteBatch spriteBatch, Camera camera, RenderStats stats) {
        spriteBatch.begin();
        for (GameObject obj : enemies) {
            if (obj.shouldRemove()) continue;
            EnemyShip es = obj.as(EnemyShip.class);
            if (es == null) continue;
            for (SmokeParticle p : es.getSmokeParticles()) {
                stats.particlesActive++;
                if (camera.isInRenderBounds(p.getX(), p.getY(), p.getCullRadius())) {
                    stats.particlesVisible++;
                    p.draw(spriteBatch, camera);
                    stats.particlesDrawn++;
                } else {
                    stats.particlesCulled++;
                }
            }
        }
        spriteBatch.end();
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
        checkProjectileCollisions(obstacles);
        checkProjectileCollisions(enemies);

        // Коллизии вражеских снарядов с игроком
        checkProjectilePlayerCollisions();

        // Коллизии игрока с препятствиями
        checkPlayerObstacleCollisions();

        // Коллизии игрока с врагами
        checkPlayerEnemyCollisions();
    }

    private void checkProjectileCollisions(Array<GameObject> targets) {
        for (GameObject projectile : projectiles) {
            if (projectile.shouldRemove()) continue;
            ProjectileCollision collision = detectProjectileCollision(projectile, targets);
            if (collision != null) resolveProjectileCollision(collision);
        }
    }

    private ProjectileCollision detectProjectileCollision(GameObject projectile, Array<GameObject> targets) {
        OrkBullet enemyBullet = projectile.as(OrkBullet.class);
        for (GameObject target : targets) {
            if (target.shouldRemove()) continue;
            if (enemyBullet != null && enemyBullet.getOwner() == target.getObject()) continue;
            if (projectile.intersects(target)) return new ProjectileCollision(projectile, target);
        }
        return null;
    }

    private void resolveProjectileCollision(ProjectileCollision collision) {
        if (collision.projectile().isOrkBullet()) {
            collision.projectile().markForRemoval();
            return;
        }
        handleProjectileTargetCollision(collision.projectile(), collision.target());
    }

    private void checkProjectilePlayerCollisions() {
        if (player == null) return;

        for (GameObject projectile : projectiles) {
            if (projectile.shouldRemove()) continue;

            // Игрока задевают только вражеские пули
            if (!projectile.isOrkBullet()) continue;

            if (projectile.intersects(player)) {
                projectile.markForRemoval();
                player.hit();
            }
        }
    }

    private void checkPlayerObstacleCollisions() {
        if (player == null) return;

        for (GameObject obstacle : obstacles) {
            if (obstacle.shouldRemove()) continue;

            if (player.intersects(obstacle.getObject())) {
                handlePlayerObstacleCollision(obstacle);
                break;
            }
        }
    }

    private void checkPlayerEnemyCollisions() {
        if (player == null) return;

        for (GameObject enemy : enemies) {
            if (enemy.shouldRemove()) continue;

            if (player.intersects(enemy.getObject())) {
                handlePlayerEnemyCollision();
            }
        }
    }

    // === ОБРАБОТКА КОЛЛИЗИЙ ===

    private void handleProjectileTargetCollision(GameObject projectile, GameObject target) {
        projectile.markForRemoval();
        Array<GameObject> splashTargets = projectile.isAutoRocket()
                ? collectSplashTargets(target.getX(), target.getY(), target)
                : null;
        damageTarget(target, projectileDamage(projectile),
                DestructionEvent.Cause.DIRECT_PROJECTILE, projectile.getObject());
        if (splashTargets != null) {
            applyRocketExplosion(target.getX(), target.getY(), splashTargets, projectile.getObject());
        }
    }

    private void damageTarget(GameObject target, int damage, DestructionEvent.Cause cause,
                              SpaceObject source) {
        if (target.isObstacle()) damageObstacle(target, damage, cause, source);
        else if (target.isEnemy()) damageEnemy(target, damage, cause, source);
    }

    private void damageObstacle(GameObject obstacle, int damage, DestructionEvent.Cause cause,
                                SpaceObject source) {
        if (obstacle.isAsteroid()) {
            Asteroid asteroid = obstacle.as(Asteroid.class);
            if (asteroid != null && applyDamage(obstacle, damage)) {
                obstacle.markForRemoval();
                splitAsteroid(asteroid, cause, source);
            }
        } else if (obstacle.isOrkAsteroid()) {
            OrkAsteroid orkAsteroid = obstacle.as(OrkAsteroid.class);
            if (orkAsteroid != null && applyDamage(obstacle, damage)) {
                obstacle.markForRemoval();
                notifyDestroyed(DestructionEvent.Kind.ENEMY, orkAsteroid, cause, source);
            }
        }
    }

    private void splitAsteroid(Asteroid asteroid, DestructionEvent.Cause cause, SpaceObject source) {
        if (asteroid.getType() == Asteroid.LARGE) {
            addAsteroid(new Asteroid(asteroid.getX(), asteroid.getY(), Asteroid.MEDIUM));
            addAsteroid(new Asteroid(asteroid.getX(), asteroid.getY(), Asteroid.MEDIUM));
        } else if (asteroid.getType() == Asteroid.MEDIUM) {
            addAsteroid(new Asteroid(asteroid.getX(), asteroid.getY(), Asteroid.SMALL));
            addAsteroid(new Asteroid(asteroid.getX(), asteroid.getY(), Asteroid.SMALL));
        }
        notifyDestroyed(DestructionEvent.Kind.ASTEROID_STAGE, asteroid, cause, source);
    }

    private void damageEnemy(GameObject enemy, int damage, DestructionEvent.Cause cause,
                             SpaceObject source) {
        if (enemy.isEnemyShip()) {
            EnemyShip enemyShip = enemy.as(EnemyShip.class);
            if (enemyShip != null && applyDamage(enemy, damage)) {
                enemy.markForRemoval();
                notifyDestroyed(DestructionEvent.Kind.ENEMY, enemyShip, cause, source);
            }
        }
    }

    private boolean applyDamage(GameObject target, int damage) {
        if (!(target.getObject() instanceof Damageable damageable)) return false;
        boolean wasAlive = !damageable.shouldRemove();
        damageable.takeDamage(damage);
        return wasAlive && damageable.shouldRemove();
    }

    private int projectileDamage(GameObject projectile) {
        AutoRocket rocket = projectile.as(AutoRocket.class);
        if (rocket != null) return Math.max(1, Math.round(rocket.getDamage()));
        Bullet bullet = projectile.as(Bullet.class);
        return bullet == null ? 1 : Math.max(1, Math.round(bullet.getDamage()));
    }

    private Array<GameObject> collectSplashTargets(float x, float y, GameObject directTarget) {
        Array<GameObject> targets = new Array<>();
        collectSplashTargets(obstacles, targets, x, y, directTarget);
        collectSplashTargets(enemies, targets, x, y, directTarget);
        return targets;
    }

    private void collectSplashTargets(Array<GameObject> source, Array<GameObject> targets,
                                      float x, float y, GameObject directTarget) {
        float radiusSquared = AutoRocket.EXPLOSION_RADIUS * AutoRocket.EXPLOSION_RADIUS;
        int initialSize = source.size;
        for (int i = 0; i < initialSize; i++) {
            GameObject candidate = source.get(i);
            if (candidate == directTarget || candidate.shouldRemove()) continue;
            if (distanceSquared(candidate.getObject(), x, y) <= radiusSquared) targets.add(candidate);
        }
    }

    private void applyRocketExplosion(float x, float y, Array<GameObject> targets,
                                      SpaceObject source) {
        if (rocketExplosionListener != null) {
            rocketExplosionListener.onRocketExplosion(x, y, AutoRocket.EXPLOSION_RADIUS);
        }
        for (GameObject target : targets) {
            if (target.shouldRemove()) continue;
            damageTarget(target, AutoRocket.SPLASH_DAMAGE,
                    DestructionEvent.Cause.ROCKET_SPLASH, source);
        }
    }

    private void notifyDestroyed(DestructionEvent.Kind kind, SpaceObject target,
                                 DestructionEvent.Cause cause, SpaceObject source) {
        DestructionEvent event = new DestructionEvent(
                kind, target, cause, source, target.getX(), target.getY());
        for (DestructionListener listener : destructionListeners) {
            listener.onDestroyed(event);
        }
    }

    private void handlePlayerObstacleCollision(GameObject obstacle) {
        if (player.isHit() || player.isInvulnerable()) return;
        player.hit();
        if (playerObstacleCollisionListener != null) {
            playerObstacleCollisionListener.onPlayerObstacleCollision(player.getX(), player.getY());
        }
        Asteroid asteroid = obstacle.as(Asteroid.class);
        if (asteroid != null) {
            splitAsteroid(asteroid, DestructionEvent.Cause.PLAYER_COLLISION, player);
        }
    }

    private void handlePlayerEnemyCollision() {
        player.hit();
    }

    // === УДАЛЕНИЕ ОБЪЕКТОВ ===

    private void removeObject(GameObject obj) {
        releaseObject(obj);

        allObjects.removeValue(obj, true);

        // Удаляем из быстрых списков
        projectiles.removeValue(obj, true);
        obstacles.removeValue(obj, true);
        enemies.removeValue(obj, true);
        particles.removeValue(obj, true);

        activeObjects--;
        GameLogger.debug("Removed object, active: " + activeObjects);
    }

    private void releaseObject(GameObject obj) {
        if (obj.getObject() instanceof AutoRocket rocket) {
            com.gdx.utils.ObjectPools.freeAutoRocket(rocket);
        }
        if (obj.getObject() instanceof EnemyShip enemyShip) {
            enemyShip.dispose();
        }
    }

    // === ОЧИСТКА ===

    /**
     * Очистить все объекты
     */
    public void clear() {
        for (GameObject obj : allObjects) releaseObject(obj);
        for (RocketParticle particle : rocketParticles) {
            com.gdx.utils.ParticlePool.freeRocketParticle(particle);
        }
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
                count += es.getSmokeParticles().size;
            }
        }
        if (player != null) {
            count += player.getFlameParticles().size;
            count += player.getShieldParticles().size;
            count += player.getSparkParticles().size;
            count += player.getExhaustTrailSampleCount();
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
