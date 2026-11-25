package com.gdx.simulation;

import com.badlogic.gdx.math.MathUtils;
import com.gdx.entities.AutoRocket;
import com.gdx.entities.Bullet;
import com.gdx.entities.Player;
import com.gdx.entities.Targetable;
import com.gdx.scenarios.ScenarioRunner;
import com.gdx.session.GameSession;
import com.gdx.utils.ObjectPools;

public final class GameSimulation {

    public record BeginTickResult(
            long playerNanos,
            long objectsNanos,
            boolean playerReset
    ) {}

    private static final float TARGET_CACHE_DURATION = 0.1f;
    private static final float BASE_PLAYER_FIRE_INTERVAL = 0.25f;
    private static final int MAX_PLAYER_BULLETS = 4;

    private final GameSession session;
    private final ScenarioRunner scenarioRunner;
    private Targetable cachedNearestTarget;
    private float targetCacheTimer;
    private boolean autoRocketLeftSide = true;
    private boolean restoredFromSave;
    private float playerFireTimer = BASE_PLAYER_FIRE_INTERVAL;

    public GameSimulation(GameSession session, ScenarioRunner scenarioRunner) {
        this.session = session;
        this.scenarioRunner = scenarioRunner;
    }

    public BeginTickResult beginTick(float delta, PlayerControls controls, boolean scenarioActive) {
        session.updateTime(delta);
        session.getProgression().syncConfiguredBaseStats();
        playerFireTimer += delta;
        applyControls(controls);
        if (!scenarioActive) {
            session.getWorld().update(delta, restoredFromSave);
            restoredFromSave = false;
        }

        long started = System.nanoTime();
        if (scenarioRunner != null) scenarioRunner.beforeUpdate(delta);
        Player player = session.getPlayer();
        player.setSpeed(session.getProgression().getPlayerSpeed());
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
        session.getObjects().checkCollisions();
        if (scenarioRunner != null) scenarioRunner.afterUpdate(delta);
        return System.nanoTime() - started;
    }

    private void applyControls(PlayerControls controls) {
        Player player = session.getPlayer();
        player.setLeft(controls.left());
        player.setRight(controls.right());
        player.setUp(controls.thrust());
        if (controls.fire()) firePlayerBullet(player);
    }

    private void firePlayerBullet(Player player) {
        if (session.getObjects().getProjectileCount() >= MAX_PLAYER_BULLETS) return;
        float interval = BASE_PLAYER_FIRE_INTERVAL / session.getProgression().getFireRate();
        if (playerFireTimer < interval) return;
        session.getObjects().addBullet(new Bullet(
                player.getX(), player.getY(), player.getRadians(),
                session.getProgression().getBulletSpeed(),
                session.getProgression().getBulletDamage()));
        playerFireTimer = 0f;
    }

    private void updateTargeting(float delta, boolean scenarioActive) {
        targetCacheTimer += delta;
        if (targetCacheTimer >= TARGET_CACHE_DURATION) {
            Player player = session.getPlayer();
            cachedNearestTarget = session.getObjects().findNearestTarget(
                    player.getX(), player.getY(), AutoRocket.DETECTION_RANGE);
            targetCacheTimer = 0f;
        }
        if (scenarioActive) return;

        session.addAutoRocketTime(delta);
        if (session.getAutoRocketTimer() >= session.getProgression().getAutoRocketCooldown()) {
            createAutoRocket();
            session.resetAutoRocketTimer();
        }
    }

    private void createAutoRocket() {
        if (cachedNearestTarget == null) return;
        Player player = session.getPlayer();
        float dx = cachedNearestTarget.getX() - player.getX();
        float dy = cachedNearestTarget.getY() - player.getY();
        if ((float) Math.sqrt(dx * dx + dy * dy) > AutoRocket.DETECTION_RANGE) return;

        float angle = player.getRadians()
                + (autoRocketLeftSide ? MathUtils.HALF_PI : -MathUtils.HALF_PI);
        AutoRocket rocket = ObjectPools.obtainAutoRocket(
                player.getX(), player.getY(), angle, cachedNearestTarget);
        rocket.setDamage(session.getProgression().getAutoRocketDamage());
        session.getObjects().addAutoRocket(rocket);
        autoRocketLeftSide = !autoRocketLeftSide;
    }

    public void setRestoredFromSave(boolean restoredFromSave) {
        this.restoredFromSave = restoredFromSave;
    }
}
