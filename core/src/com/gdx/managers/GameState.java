package com.gdx.managers;

import com.gdx.entities.Player;
import com.gdx.entities.Asteroid;
import com.gdx.entities.Enemy;
import com.gdx.entities.AutoRocket;
import com.gdx.entities.Bullet;
import com.gdx.entities.OrkAsteroid;
import com.gdx.entities.OrkBullet;
import com.gdx.entities.Particle;
import com.badlogic.gdx.utils.Array;

public class GameState {
    // Игровые объекты
    private Player player;
    private Array<Asteroid> asteroids;
    private Array<Enemy> enemies;
    private Array<AutoRocket> autoRockets;
    private Array<Bullet> bullets;
    private Array<OrkAsteroid> orkAsteroids;
    private Array<OrkBullet> orkBullets;
    private Array<Particle> particles;
    
    // Игровые параметры
    private int level;
    private int totalAsteroids;
    private int numAsteroidsLeft;
    private int score;
    private int highScore;
    private float autoRocketTimer;
    
    // Конструктор
    public GameState() {
        asteroids = new Array<>();
        enemies = new Array<>();
        autoRockets = new Array<>();
        bullets = new Array<>();
        orkAsteroids = new Array<>();
        orkBullets = new Array<>();
        particles = new Array<>();
    }
    
    // Геттеры и сеттеры
    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }
    
    public Array<Asteroid> getAsteroids() { return asteroids; }
    public void setAsteroids(Array<Asteroid> asteroids) { this.asteroids = asteroids; }
    
    public Array<Enemy> getEnemies() { return enemies; }
    public void setEnemies(Array<Enemy> enemies) { this.enemies = enemies; }
    
    public Array<AutoRocket> getAutoRockets() { return autoRockets; }
    public void setAutoRockets(Array<AutoRocket> autoRockets) { this.autoRockets = autoRockets; }
    
    public Array<Bullet> getBullets() { return bullets; }
    public void setBullets(Array<Bullet> bullets) { this.bullets = bullets; }
    
    public Array<OrkAsteroid> getOrkAsteroids() { return orkAsteroids; }
    public void setOrkAsteroids(Array<OrkAsteroid> orkAsteroids) { this.orkAsteroids = orkAsteroids; }
    
    public Array<OrkBullet> getOrkBullets() { return orkBullets; }
    public void setOrkBullets(Array<OrkBullet> orkBullets) { this.orkBullets = orkBullets; }
    
    public Array<Particle> getParticles() { return particles; }
    public void setParticles(Array<Particle> particles) { this.particles = particles; }
    
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    
    public int getTotalAsteroids() { return totalAsteroids; }
    public void setTotalAsteroids(int totalAsteroids) { this.totalAsteroids = totalAsteroids; }
    
    public int getNumAsteroidsLeft() { return numAsteroidsLeft; }
    public void setNumAsteroidsLeft(int numAsteroidsLeft) { this.numAsteroidsLeft = numAsteroidsLeft; }
    
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    
    public int getHighScore() { return highScore; }
    public void setHighScore(int highScore) { this.highScore = highScore; }
    
    public float getAutoRocketTimer() { return autoRocketTimer; }
    public void setAutoRocketTimer(float autoRocketTimer) { this.autoRocketTimer = autoRocketTimer; }
}
