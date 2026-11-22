package com.gdx.scenarios;

import com.badlogic.gdx.math.MathUtils;
import com.gdx.entities.Asteroid;
import com.gdx.entities.AutoRocket;
import com.gdx.entities.Bullet;
import com.gdx.entities.Enemy;
import com.gdx.entities.EnemyShip;
import com.gdx.entities.GameObject;
import com.gdx.entities.OrkBullet;
import com.gdx.entities.Player;
import com.gdx.entities.SpaceObject;
import com.gdx.gamestates.PlayState;
import com.gdx.managers.GameObjectManager;
import com.gdx.utils.ObjectPools;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ScenarioContext {

    private final PlayState playState;
    private final GameObjectManager objects;
    private final Map<String, SpaceObject> ids = new LinkedHashMap<>();

    ScenarioContext(PlayState playState) {
        this.playState = playState;
        this.objects = playState.getGameObjectManager();
        ids.put("player", playState.getPlayer());
    }

    public Player player() { return playState.getPlayer(); }
    public GameObjectManager objects() { return objects; }
    public PlayState playState() { return playState; }

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

    public void expectExperienceAtLeast(int expected) {
        int actual = playState.getProgressionManager().getCurrentExperience();
        if (actual < expected) {
            throw new AssertionError("experience: expected at least=" + expected + ", actual=" + actual);
        }
    }
}
