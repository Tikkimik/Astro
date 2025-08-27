package com.gdx.managers;

import com.gdx.entities.Player;
import com.gdx.entities.Asteroid;
import com.gdx.entities.Enemy;
import com.gdx.entities.AutoRocket;
import com.gdx.entities.Bullet;
import com.gdx.entities.OrkAsteroid;
import com.gdx.entities.OrkBullet;
import com.gdx.entities.Particle;
import java.util.ArrayList;

public class GameState {
    // Игровые объекты
    private Player player;
    private ArrayList<Asteroid> asteroids;
    private ArrayList<Enemy> enemies;
    private ArrayList<AutoRocket> autoRockets;
    private ArrayList<Bullet> bullets;
    private ArrayList<OrkAsteroid> orkAsteroids;
    private ArrayList<OrkBullet> orkBullets;
    private ArrayList<Particle> particles;
    
    // Игровые параметры
    private int level;
    private int totalAsteroids;
    private int numAsteroidsLeft;
    private int score;
    private int highScore;
    private float autoRocketTimer;
    
    // Конструктор
    public GameState() {
        asteroids = new ArrayList<>();
        enemies = new ArrayList<>();
        autoRockets = new ArrayList<>();
        bullets = new ArrayList<>();
        orkAsteroids = new ArrayList<>();
        orkBullets = new ArrayList<>();
        particles = new ArrayList<>();
    }
    
    // Геттеры и сеттеры
    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }
    
    public ArrayList<Asteroid> getAsteroids() { return asteroids; }
    public void setAsteroids(ArrayList<Asteroid> asteroids) { this.asteroids = asteroids; }
    
    public ArrayList<Enemy> getEnemies() { return enemies; }
    public void setEnemies(ArrayList<Enemy> enemies) { this.enemies = enemies; }
    
    public ArrayList<AutoRocket> getAutoRockets() { return autoRockets; }
    public void setAutoRockets(ArrayList<AutoRocket> autoRockets) { this.autoRockets = autoRockets; }
    
    public ArrayList<Bullet> getBullets() { return bullets; }
    public void setBullets(ArrayList<Bullet> bullets) { this.bullets = bullets; }
    
    public ArrayList<OrkAsteroid> getOrkAsteroids() { return orkAsteroids; }
    public void setOrkAsteroids(ArrayList<OrkAsteroid> orkAsteroids) { this.orkAsteroids = orkAsteroids; }
    
    public ArrayList<OrkBullet> getOrkBullets() { return orkBullets; }
    public void setOrkBullets(ArrayList<OrkBullet> orkBullets) { this.orkBullets = orkBullets; }
    
    public ArrayList<Particle> getParticles() { return particles; }
    public void setParticles(ArrayList<Particle> particles) { this.particles = particles; }
    
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
