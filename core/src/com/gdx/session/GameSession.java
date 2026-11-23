package com.gdx.session;

import com.gdx.entities.Player;
import com.gdx.managers.GameObjectManager;
import com.gdx.managers.ProgressionManager;
import com.gdx.managers.WorldManager;

public final class GameSession {

    private final Player player;
    private final GameObjectManager objects;
    private final ProgressionManager progression;
    private final WorldManager world;

    private int level = 1;
    private int totalAsteroids;
    private int numAsteroidsLeft;
    private int score;
    private int highScore;
    private float autoRocketTimer;
    private float elapsedTime;

    public GameSession(Player player, GameObjectManager objects, WorldManager world,
                       ProgressionManager progression, int highScore) {
        this.player = player;
        this.objects = objects;
        this.world = world;
        this.progression = progression;
        this.highScore = highScore;
    }

    public void updateTime(float fixedDelta) {
        elapsedTime += fixedDelta;
    }

    public void addScore(int points) {
        score += points;
        highScore = Math.max(highScore, score);
        progression.addExperience(points);
    }

    public GameSessionSnapshot createSnapshot() {
        return new GameSessionSnapshot(
                objects.createSnapshot(), progression.createSnapshot(), level,
                totalAsteroids, numAsteroidsLeft, score, highScore,
                autoRocketTimer, elapsedTime);
    }

    public void restore(GameSessionSnapshot snapshot) {
        objects.restore(snapshot.objects());
        progression.restore(snapshot.progression());
        level = snapshot.level();
        totalAsteroids = snapshot.totalAsteroids();
        numAsteroidsLeft = snapshot.numAsteroidsLeft();
        score = snapshot.score();
        highScore = snapshot.highScore();
        autoRocketTimer = snapshot.autoRocketTimer();
        elapsedTime = snapshot.elapsedTime();
        player.setGameObjectManager(objects);
        world.updateReferences(objects, player);
    }

    public Player getPlayer() { return player; }
    public GameObjectManager getObjects() { return objects; }
    public ProgressionManager getProgression() { return progression; }
    public WorldManager getWorld() { return world; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getTotalAsteroids() { return totalAsteroids; }
    public void setTotalAsteroids(int totalAsteroids) { this.totalAsteroids = totalAsteroids; }
    public int getNumAsteroidsLeft() { return numAsteroidsLeft; }
    public void setNumAsteroidsLeft(int numAsteroidsLeft) { this.numAsteroidsLeft = numAsteroidsLeft; }
    public void decrementAsteroidsLeft() {
        if (numAsteroidsLeft > 0) numAsteroidsLeft--;
    }
    public int getScore() { return score; }
    public int getHighScore() { return highScore; }
    public float getAutoRocketTimer() { return autoRocketTimer; }
    public void addAutoRocketTime(float delta) { autoRocketTimer += delta; }
    public void resetAutoRocketTimer() { autoRocketTimer = 0f; }
    public float getElapsedTime() { return elapsedTime; }
}
