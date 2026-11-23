package com.gdx.scenarios;

import com.badlogic.gdx.math.MathUtils;
import com.gdx.entities.Asteroid;
import com.gdx.entities.AutoRocket;
import com.gdx.entities.Bullet;
import com.gdx.entities.Enemy;
import com.gdx.entities.EnemyShip;
import com.gdx.entities.GameObject;
import com.gdx.entities.OrkBullet;
import com.gdx.entities.OrkAsteroid;
import com.gdx.entities.Player;
import com.gdx.entities.SpaceObject;
import com.gdx.gamestates.PlayState;
import com.gdx.managers.GameObjectManager;
import com.gdx.utils.ObjectPools;
import com.gdx.session.GameSessionSnapshot;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class ScenarioContext {

    private final PlayState playState;
    private final GameObjectManager objects;
    private final Map<String, SpaceObject> ids = new LinkedHashMap<>();
    private final Map<String, float[]> positionSnapshots = new LinkedHashMap<>();
    private final Map<String, GameSessionSnapshot> sessionSnapshots = new LinkedHashMap<>();
    private final Set<String> completedActions = new HashSet<>();

    ScenarioContext(PlayState playState) {
        this.playState = playState;
        this.objects = playState.getGameObjectManager();
        ids.put("player", playState.getPlayer());
    }

    public Player player() { return playState.getPlayer(); }
    public GameObjectManager objects() { return objects; }
    public PlayState playState() { return playState; }

    public void rememberSession(String id) {
        if (sessionSnapshots.putIfAbsent(id, playState.getSession().createSnapshot()) != null) {
            throw new IllegalArgumentException("Duplicate session snapshot id: " + id);
        }
    }

    public void restoreSession(String id) {
        GameSessionSnapshot snapshot = sessionSnapshots.get(id);
        if (snapshot == null) throw new AssertionError("Unknown session snapshot: " + id);
        playState.restoreGameState(snapshot);
    }

    public void addScore(int points) {
        playState.getSession().addScore(points);
    }

    public void advanceSessionTime(float delta) {
        playState.getSession().updateTime(delta);
    }

    public void expectSessionElapsed(float expected) {
        float actual = playState.getSession().getElapsedTime();
        if (Math.abs(actual - expected) > 0.0001f) {
            throw new AssertionError("session elapsed: expected=" + expected + ", actual=" + actual);
        }
    }

    public void controls(boolean thrust, boolean left, boolean right) {
        player().setUp(thrust);
        player().setLeft(left);
        player().setRight(right);
    }

    public void setPlayerAngle(float radians) {
        player().setRadians(radians);
    }

    public void disablePlayerShield() {
        player().cancelInvulnerability();
    }

    public void showDebugOverlay() {
        playState.setDebugOverlayVisible(true);
    }

    public Asteroid asteroid(String id, float x, float y, int type) {
        Asteroid asteroid = new Asteroid(x, y, type);
        objects.addAsteroid(asteroid);
        return register(id, asteroid);
    }

    public EnemyShip enemyShip(String id, float x, float y) {
        EnemyShip enemy = new EnemyShip(x, y, player(), null);
        objects.addEnemyShip(enemy);
        return register(id, enemy);
    }

    public OrkAsteroid orkAsteroid(String id, float x, float y, int type) {
        OrkAsteroid asteroid = new OrkAsteroid(x, y, type, player(), null);
        objects.addOrkAsteroid(asteroid);
        return register(id, asteroid);
    }

    public Bullet bullet(String id, float x, float y, float angle) {
        Bullet bullet = new Bullet(x, y, angle);
        objects.addBullet(bullet);
        return register(id, bullet);
    }

    public OrkBullet enemyBullet(String id, float x, float y, float angle) {
        OrkBullet bullet = new OrkBullet(x, y, angle);
        objects.addOrkBullet(bullet);
        return register(id, bullet);
    }

    public OrkBullet enemyBullet(String id, float x, float y, float angle, String ownerId) {
        OrkBullet bullet = new OrkBullet(x, y, angle, get(ownerId, Enemy.class));
        objects.addOrkBullet(bullet);
        return register(id, bullet);
    }

    public AutoRocket rocket(String id, float x, float y, float angle, String targetId) {
        Enemy target = get(targetId, Enemy.class);
        AutoRocket rocket = ObjectPools.obtainAutoRocket(x, y, angle, target);
        objects.addAutoRocket(rocket);
        return register(id, rocket);
    }

    public float angleTo(float fromX, float fromY, float toX, float toY) {
        return MathUtils.atan2(toY - fromY, toX - fromX);
    }

    public <T extends SpaceObject> T register(String id, T object) {
        if (ids.putIfAbsent(id, object) != null) {
            throw new IllegalArgumentException("Duplicate scenario object id: " + id);
        }
        return object;
    }

    public <T extends SpaceObject> T get(String id, Class<T> type) {
        SpaceObject object = ids.get(id);
        if (object == null) throw new AssertionError("Unknown scenario object: " + id);
        if (!type.isInstance(object)) {
            throw new AssertionError("Object " + id + " is " + object.getClass().getSimpleName()
                    + ", expected " + type.getSimpleName());
        }
        return type.cast(object);
    }

    public boolean isManaged(String id) {
        SpaceObject target = get(id, SpaceObject.class);
        for (GameObject object : objects.getAllObjects()) {
            if (object.getObject() == target) return true;
        }
        return false;
    }

    public void expectManaged(String id, boolean expected) {
        boolean actual = isManaged(id);
        if (actual != expected) {
            throw new AssertionError(id + " managed: expected=" + expected + ", actual=" + actual);
        }
    }

    public void expectManagedCount(Class<? extends SpaceObject> type, int expected) {
        int actual = 0;
        for (GameObject object : objects.getAllObjects()) {
            if (object.isActive() && !object.shouldRemove() && type.isInstance(object.getObject())) actual++;
        }
        if (actual != expected) {
            throw new AssertionError(type.getSimpleName() + " count: expected=" + expected
                    + ", actual=" + actual);
        }
    }

    public void expectAsteroidCount(int type, int expected) {
        int actual = 0;
        for (GameObject object : objects.getObstacles()) {
            Asteroid asteroid = object.as(Asteroid.class);
            if (object.isActive() && !object.shouldRemove()
                    && asteroid != null && asteroid.getType() == type) actual++;
        }
        if (actual != expected) {
            throw new AssertionError("asteroid type " + type + " count: expected=" + expected
                    + ", actual=" + actual);
        }
    }

    public void expectHealth(String id, int expected) {
        int actual = get(id, Enemy.class).getHealth();
        if (actual != expected) {
            throw new AssertionError(id + " health: expected=" + expected + ", actual=" + actual);
        }
    }

    public void expectPlayerHit(boolean expected) {
        boolean actual = player().isHit();
        if (actual != expected) {
            throw new AssertionError("player hit: expected=" + expected + ", actual=" + actual);
        }
    }

    public void expectScoreAtLeast(int expected) {
        int actual = playState.getScore();
        if (actual < expected) {
            throw new AssertionError("score: expected at least=" + expected + ", actual=" + actual);
        }
    }

    public void expectScore(int expected) {
        int actual = playState.getScore();
        if (actual != expected) {
            throw new AssertionError("score: expected=" + expected + ", actual=" + actual);
        }
    }

    public void expectExperienceAtLeast(int expected) {
        int actual = playState.getProgressionManager().getCurrentExperience();
        if (actual < expected) {
            throw new AssertionError("experience: expected at least=" + expected + ", actual=" + actual);
        }
    }

    public void expectExperience(int expected) {
        int actual = playState.getProgressionManager().getCurrentExperience();
        if (actual != expected) {
            throw new AssertionError("experience: expected=" + expected + ", actual=" + actual);
        }
    }

    public void expectNearestEnemy(String expectedId, float maxDistance) {
        Enemy expected = get(expectedId, Enemy.class);
        Enemy actual = objects.findNearestEnemy(player().getX(), player().getY(), maxDistance);
        if (actual != expected) {
            String actualName = actual == null ? "none" : actual.getEnemyType();
            throw new AssertionError("nearest enemy: expected=" + expectedId + ", actual=" + actualName);
        }
    }

    public void expectRocketTarget(String rocketId, String expectedTargetId) {
        Enemy actual = get(rocketId, AutoRocket.class).getTarget();
        Enemy expected = get(expectedTargetId, Enemy.class);
        if (actual != expected) {
            String actualName = actual == null ? "none" : actual.getEnemyType();
            throw new AssertionError(rocketId + " target: expected=" + expectedTargetId
                    + ", actual=" + actualName);
        }
    }

    public void expectRocketHasNoTarget(String rocketId) {
        Enemy actual = get(rocketId, AutoRocket.class).getTarget();
        if (actual != null) {
            throw new AssertionError(rocketId + " target: expected=none, actual="
                    + actual.getEnemyType());
        }
    }

    public void rememberPosition(String snapshotId, String objectId) {
        SpaceObject object = get(objectId, SpaceObject.class);
        if (positionSnapshots.putIfAbsent(snapshotId,
                new float[]{object.getX(), object.getY()}) != null) {
            throw new IllegalArgumentException("Duplicate position snapshot: " + snapshotId);
        }
    }

    public void expectMovedAtLeast(String objectId, String snapshotId, float expectedDistance) {
        SpaceObject object = get(objectId, SpaceObject.class);
        float[] start = positionSnapshots.get(snapshotId);
        if (start == null) throw new AssertionError("Unknown position snapshot: " + snapshotId);
        float dx = object.getX() - start[0];
        float dy = object.getY() - start[1];
        float actualDistance = (float) Math.sqrt(dx * dx + dy * dy);
        if (actualDistance < expectedDistance) {
            throw new AssertionError(objectId + " distance: expected at least=" + expectedDistance
                    + ", actual=" + actualDistance);
        }
    }

    public void runOnce(String actionId, float elapsed, float time, Runnable action) {
        if (elapsed >= time && completedActions.add(actionId)) action.run();
    }

    public void shootAllAsteroidsOfType(int type) {
        for (GameObject object : objects.getObstacles().toArray(GameObject.class)) {
            Asteroid asteroid = object.as(Asteroid.class);
            if (object.isActive() && !object.shouldRemove()
                    && asteroid != null && asteroid.getType() == type) {
                objects.addBullet(new Bullet(asteroid.getX() - 4f, asteroid.getY(), 0f));
            }
        }
    }

    public void destroyEnemy(String id) {
        Enemy enemy = get(id, Enemy.class);
        enemy.takeDamage(enemy.getHealth());
    }

}
