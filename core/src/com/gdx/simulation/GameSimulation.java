package com.gdx.simulation;

import com.badlogic.gdx.math.MathUtils;
import com.gdx.entities.Asteroid;
import com.gdx.entities.AutoRocket;
import com.gdx.entities.Enemy;
import com.gdx.entities.GameObject;
import com.gdx.entities.Player;
import com.gdx.scenarios.ScenarioRunner;
import com.gdx.session.GameSession;
import com.gdx.utils.ObjectPools;

public final class GameSimulation {

    @FunctionalInterface
    public interface PositionEffect {
        void create(float x, float y);
    }

    public record BeginTickResult(
            long playerNanos,
            long objectsNanos,
            boolean playerReset
    ) {}

    private static final float ENEMY_CACHE_DURATION = 0.1f;
    private static final float AUTO_ROCKET_INTERVAL = 1f;

    private final GameSession session;
    private final ScenarioRunner scenarioRunner;
    private final PositionEffect shipExplosion;
    private Enemy cachedNearestEnemy;
    private float enemyCacheTimer;
    private boolean autoRocketLeftSide = true;
    private boolean restoredFromSave;

    public GameSimulation(GameSession session, ScenarioRunner scenarioRunner,
                          PositionEffect shipExplosion) {
        this.session = session;
        this.scenarioRunner = scenarioRunner;
        this.shipExplosion = shipExplosion;
    }

    public BeginTickResult beginTick(float delta, PlayerControls controls, boolean scenarioActive) {
        session.updateTime(delta);
        applyControls(controls);
        if (!scenarioActive) session.getWorld().update(delta, restoredFromSave);

        long started = System.nanoTime();
        if (scenarioRunner != null) scenarioRunner.beforeUpdate(delta);
        Player player = session.getPlayer();
        player.update(delta);
        if (player.isDead()) {
            player.reset();
            return new BeginTickResult(System.nanoTime() - started, 0L, true);
        }
        long playerNanos = System.nanoTime() - started;

        started = System.nanoTime();
        session.getObjects().update(delta);
        long objectsNanos = System.nanoTime() - started;

        return new BeginTickResult(playerNanos, objectsNanos, false);
    }

    public long finishTick(float delta, boolean scenarioActive) {
        updateTargeting(delta, scenarioActive);
        long started = System.nanoTime();
        checkPlayerObstacleCollisions();
        session.getObjects().checkCollisions();
        if (scenarioRunner != null) scenarioRunner.afterUpdate(delta);
        return System.nanoTime() - started;
    }

    private void applyControls(PlayerControls controls) {
        Player player = session.getPlayer();
        player.setLeft(controls.left());
        player.setRight(controls.right());
        player.setUp(controls.thrust());
        if (controls.fire()) player.shoot();
    }

    private void updateTargeting(float delta, boolean scenarioActive) {
        enemyCacheTimer += delta;
        if (enemyCacheTimer >= ENEMY_CACHE_DURATION) {
            Player player = session.getPlayer();
            cachedNearestEnemy = session.getObjects().findNearestEnemy(
                    player.getX(), player.getY(), AutoRocket.DETECTION_RANGE);
            enemyCacheTimer = 0f;
        }
        if (scenarioActive) return;

        session.addAutoRocketTime(delta);
        if (session.getAutoRocketTimer() >= AUTO_ROCKET_INTERVAL) {
            createAutoRocket();
            session.resetAutoRocketTimer();
        }
    }

    private void createAutoRocket() {
        if (cachedNearestEnemy == null) return;
        Player player = session.getPlayer();
        float dx = cachedNearestEnemy.getX() - player.getX();
        float dy = cachedNearestEnemy.getY() - player.getY();
        if ((float) Math.sqrt(dx * dx + dy * dy) > AutoRocket.DETECTION_RANGE) return;

        float angle = player.getRadians()
                + (autoRocketLeftSide ? MathUtils.HALF_PI : -MathUtils.HALF_PI);
        session.getObjects().addAutoRocket(ObjectPools.obtainAutoRocket(
                player.getX(), player.getY(), angle, cachedNearestEnemy));
        autoRocketLeftSide = !autoRocketLeftSide;
    }

    private void checkPlayerObstacleCollisions() {
        Player player = session.getPlayer();
        if (player.isHit() || player.isInvulnerable()) return;
        for (GameObject object : session.getObjects().getObstacles()) {
            if (!object.intersects(player)) continue;
            player.hit();
            shipExplosion.create(player.getX(), player.getY());
            Asteroid asteroid = object.as(Asteroid.class);
            if (asteroid != null) session.getObjects().splitAsteroid(asteroid);
            break;
        }
    }

    public void setRestoredFromSave(boolean restoredFromSave) {
        this.restoredFromSave = restoredFromSave;
    }
}
