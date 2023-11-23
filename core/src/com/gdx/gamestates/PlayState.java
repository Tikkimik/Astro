package com.gdx.gamestates;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.gdx.entities.*;
import com.gdx.game.MyGdxGame;
import com.gdx.managers.GameKeys;
import com.gdx.managers.GameStateManager;

import java.util.ArrayList;

public class PlayState extends GameState {

    private ShapeRenderer shapeRenderer;
    private Player player;
    private ArrayList<Bullet> bullets;
    private ArrayList<Asteroid> asteroids;
    private ArrayList<Particle> particles;
    private ArrayList<Target> targets;
    private int level;
    private int totalAsteroids;
    private int numAsteroidsLeft;

    public PlayState(GameStateManager gameStateManager) {
        super(gameStateManager);
    }

    @Override
    public void init() {
        shapeRenderer = new ShapeRenderer();
        bullets = new ArrayList<>();
        player = new Player(bullets);
        asteroids = new ArrayList<>();
        particles = new ArrayList<>();
        targets = new ArrayList<>();

        level = 1;

        spawnAsteroids();
    }

    private void createParticles(float x, float y) {
        int particlesCount = MathUtils.random(5, 10);

        for (int i = 0; i < particlesCount; i++) {
            particles.add(new Particle(x, y, MathUtils.random(1, 10)));
        }
    }

    private void spawnAsteroids() {
        asteroids.clear();

        int numToSpawn = 10 + level - 1;

        totalAsteroids = numToSpawn * 7;
        numAsteroidsLeft = totalAsteroids;

        for (int i = 0; i < numToSpawn; i++) {
            float x = MathUtils.random(MyGdxGame.WIDTH);
            float y = MathUtils.random(MyGdxGame.HEIGHT);
            float dx = x - player.getX();
            float dy = y - player.getY();
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            while (dist < 100) {
                x = MathUtils.random(MyGdxGame.WIDTH);
                y = MathUtils.random(MyGdxGame.HEIGHT);
                dx = x - player.getX();
                dy = y - player.getY();
                dist = (float) Math.sqrt(dx * dx + dy * dy);
            }

            asteroids.add(new Asteroid(x, y, Asteroid.LARGE));
        }
    }

    /**
     * Раскалывание астероидов
     *
     * @param asteroid
     */
    private void splitAsteroids(Asteroid asteroid) {
        createParticles(asteroid.getX(), asteroid.getY());

        numAsteroidsLeft--;

        if (asteroid.getType() == Asteroid.LARGE) {
            asteroids.add(new Asteroid(asteroid.getX(), asteroid.getY(), Asteroid.MEDIUM));
            asteroids.add(new Asteroid(asteroid.getX(), asteroid.getY(), Asteroid.MEDIUM));
        }

        if (asteroid.getType() == Asteroid.MEDIUM) {
            asteroids.add(new Asteroid(asteroid.getX(), asteroid.getY(), Asteroid.SMALL));
            asteroids.add(new Asteroid(asteroid.getX(), asteroid.getY(), Asteroid.SMALL));
        }
    }

    /**
     * Коллизии
     */
    private void checkCollisions() {

        //player-asteroids
        if (!player.isHit()) {
            for (int i = 0; i < asteroids.size(); i++) {
                Asteroid asteroid = asteroids.get(i);

                if (asteroid.intersects(player)) {
                    player.hit();
                    asteroids.remove(i);
                    i--;
                    splitAsteroids(asteroid);
                    break;
                }
            }
        }

        //bullet-asteroid collision
        for (int i = 0; i < bullets.size(); i++) {
            Bullet b = bullets.get(i);
            for (int j = 0; j < asteroids.size(); j++) {
                Asteroid a = asteroids.get(j);
                if (a.contains(b.getX(), b.getY())) {
                    bullets.remove(i);
                    i--;
                    asteroids.remove(j);
                    j--;
                    splitAsteroids(a);
                    break;
                }
            }
        }

//        //asteroids collisions
//        for (int i = 0; i < asteroids.size(); i++) {
//            Asteroid b = asteroids.get(i);
//
//            for (int j = 0; j < asteroids.size(); j++) {
//                Asteroid a = asteroids.get(j);
//
//                if (a != b) {
//                    if (a.contains(b.getX(), b.getY())) {
//                        i--;
//                        asteroids.remove(j);
////                        asteroids.remove(i);
////                        j--;
//                        splitAsteroids(a);
////                        splitAsteroids(b);
//                        break;
//                    }
//                }
//            }
//        }
    }
    /*
    public void autoaim() {
        targets.clear();

        if (asteroids.size() > 0) {
            Asteroid a = asteroids.get(0);
            double minimalDistancePlayerTarget = Math.sqrt(Math.pow((player.getX() - a.getX()), 2) + Math.pow((player.getY() - a.getY()), 2));

            for (Asteroid b : asteroids) {
                double distancePlayerAndTarget = Math.sqrt(Math.pow((player.getX() - b.getX()), 2) + Math.pow((player.getY() - b.getY()), 2));
                if (minimalDistancePlayerTarget > distancePlayerAndTarget) {
                    minimalDistancePlayerTarget = distancePlayerAndTarget;
                    a = b;
                }
            }

            player.shoot(MathUtils.atan2(a.getY() - player.getY(), a.getX() - player.getX()));
            targets.add(new Target(a));
        }
    }

     */
    public void autoaim() {
        targets.clear();

        if (asteroids.size() > 0) {
            Asteroid closestAsteroid = findClosestAsteroid();

            if (closestAsteroid != null) {
                float angleToAsteroid = calculateAngleToAsteroid(closestAsteroid);

                player.shoot(angleToAsteroid);
                targets.add(new Target(closestAsteroid));
            }
        }
    }

    private Asteroid findClosestAsteroid() {
        Asteroid closestAsteroid = null;
        double minimalDistancePlayerTarget = Double.MAX_VALUE;

        for (Asteroid asteroid : asteroids) {
            double distancePlayerAndTarget = Math.sqrt(Math.pow((player.getX() - asteroid.getX()), 2) + Math.pow((player.getY() - asteroid.getY()), 2));

            if (minimalDistancePlayerTarget > distancePlayerAndTarget) {
                minimalDistancePlayerTarget = distancePlayerAndTarget;
                closestAsteroid = asteroid;
            }
        }

        return closestAsteroid;
    }

//    private float calculateAngleToAsteroid(Asteroid asteroid) {
//        // Учет скорости и движения астероида для предсказания будущего положения
//        // Здесь предполагается, что asteroid.getSpeedX() и asteroid.getSpeedY() возвращают скорость астероида по осям X и Y соответственно
//        // И player.getProjectileSpeed() возвращает скорость снаряда игрока
//
//        double minimalDistancePlayerTarget = Math.sqrt(Math.pow((player.getX() - asteroid.getX()), 2) + Math.pow((player.getY() - asteroid.getY()), 2));
//
//        double timeToImpact = minimalDistancePlayerTarget / 1000;
//        float futureX = (float)(asteroid.getX() + asteroid.getSpeed() * timeToImpact);
//        float futureY = (float)(asteroid.getY() + asteroid.getSpeed() * timeToImpact);
//
//        return MathUtils.atan2(futureY - player.getY(), futureX - player.getX());
//    }

    private float calculateAngleToAsteroid(Asteroid asteroid) {
        // Рассчитываем будущее положение мишени с использованием текущих координат, направления движения и скорости
        double minimalDistancePlayerTarget = Math.sqrt(Math.pow((player.getX() - asteroid.getX()), 2) + Math.pow((player.getY() - asteroid.getY()), 2));
        double timeToImpact = minimalDistancePlayerTarget / 350;
        float futureX = (float)(asteroid.getX() + asteroid.getDx() * timeToImpact);
        float futureY = (float)(asteroid.getY() + asteroid.getDy() * timeToImpact);

        return MathUtils.atan2(futureY - player.getY(), futureX - player.getX());
    }


    @Override
    public void update(float dt) {

        //get user input
        handleInput();

        //next level
        if (asteroids.size() == 0) {
            level++;
            spawnAsteroids();
        }

        //update player
        player.update(dt);
        if (player.isDead()) {
            player.reset();
            return;
        }

        //update player bullets
        for (int i = 0; i < bullets.size(); i++) {
            bullets.get(i).update(dt);
            if (bullets.get(i).shouldRemove()) {
                bullets.remove(i);
                i--;
            }
        }

        //update asteroids
        for (int i = 0; i < asteroids.size(); i++) {
            asteroids.get(i).update(dt);
            if (asteroids.get(i).shouldRemove()) {
                asteroids.remove(i);
                i--;
            }
        }

        //update particles
        for (int i = 0; i < particles.size(); i++) {
            particles.get(i).update(dt);
            if (particles.get(i).shouldRemove()) {
                particles.remove(i);
                i--;
            }
        }

        //check collisions
        checkCollisions();

        autoaim();
    }

    @Override
    public void draw() {

        //draw player
        player.draw(shapeRenderer);

        //draw bullets
        for (Bullet bullet : bullets) {
            bullet.draw(shapeRenderer);
        }

        //draw asteroids
        for (Asteroid asteroid : asteroids) {
            asteroid.draw(shapeRenderer);
        }

        //draw particles
        for (Particle particle : particles) {
            particle.draw(shapeRenderer);
        }

        //draw target
        for (Target target : targets) {
            target.draw(shapeRenderer);
        }
    }

    @Override
    public void handleInput() {
        player.setLeft(GameKeys.isDown(GameKeys.LEFT));
        player.setRight(GameKeys.isDown(GameKeys.RIGHT));
        player.setUp(GameKeys.isDown(GameKeys.UP));
        if (GameKeys.isPressed(GameKeys.SPACE)) {
            player.shoot();
        }
    }

    @Override
    public void dispose() {

    }
}