package com.gdx.gamestates;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.gdx.entities.Asteroid;
import com.gdx.entities.AutoRocket;
import com.gdx.entities.Background;
import com.gdx.entities.Bullet;
import com.gdx.entities.OrkAsteroid;
import com.gdx.entities.OrkBullet;
import com.gdx.entities.Particle;
import com.gdx.entities.Player;
import com.gdx.game.MyGdxGame;
import com.gdx.managers.Camera;
import com.gdx.managers.GameKeys;
import com.gdx.managers.GameStateManager;

import java.util.ArrayList;

public class PlayState extends GameState {

    private ShapeRenderer shapeRenderer;

    private Camera camera;
    private Background background;
    private Player player;
    private ArrayList<Bullet> bullets;
    private ArrayList<Asteroid> asteroids;
    private ArrayList<AutoRocket> autoRockets;
    private ArrayList<OrkAsteroid> orkAsteroids;
    private ArrayList<OrkBullet> orkBullets;

    private ArrayList<Particle> particles;

    private int level;
    private int totalAsteroids;
    private int numAsteroidsLeft;
    
    // Таймер для автоматических ракет
    private float autoRocketTimer = 0f;
    private final float AUTO_ROCKET_INTERVAL = 1f; // Интервал в секундах

    public PlayState(GameStateManager gameStateManager) {
        super(gameStateManager);
    }

    @Override
    public void init() {
        shapeRenderer = new ShapeRenderer();
        camera = new Camera();
        background = new Background(camera);
        bullets = new ArrayList<Bullet>();
        player = new Player(bullets);
        asteroids = new ArrayList<>();
        autoRockets = new ArrayList<AutoRocket>();
        orkAsteroids = new ArrayList<OrkAsteroid>();
        orkBullets = new ArrayList<OrkBullet>();
        particles = new ArrayList<Particle>();

        level = 1;

        spawnAsteroids();
        spawnOrkAsteroids();
    }

    private void createParticles(float x, float y) {
        for(int i = 0; i < 6; i++) {
            particles.add(new Particle(x, y));
        }
    }
    
    // Найти ближайший астероид к игроку
    private Asteroid findNearestAsteroid() {
        if (asteroids.isEmpty()) return null;
        
        Asteroid nearest = null;
        float minDistance = Float.MAX_VALUE;
        
        for (Asteroid asteroid : asteroids) {
            float dx = asteroid.getX() - player.getX();
            float dy = asteroid.getY() - player.getY();
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            
            if (distance < minDistance) {
                minDistance = distance;
                nearest = asteroid;
            }
        }
        
        return nearest;
    }
    
    // Создать автоматическую ракету
    private void createAutoRocket() {
        Asteroid target = findNearestAsteroid();
        if (target != null) {
            autoRockets.add(new AutoRocket(player.getX(), player.getY(), player.getRadians(), target));
        }
    }

    private void spawnAsteroids() {
        asteroids.clear();

        int numToSpawn = 4 + level - 1;

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
    
    private void spawnOrkAsteroids() {
        orkAsteroids.clear();
        
        // Создаем 1-2 орк-астероида в зависимости от уровня
        int numToSpawn = Math.min(1 + level / 3, 3);
        
        for (int i = 0; i < numToSpawn; i++) {
            float x = MathUtils.random(MyGdxGame.WIDTH);
            float y = MathUtils.random(MyGdxGame.HEIGHT);
            float dx = x - player.getX();
            float dy = y - player.getY();
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            while (dist < 150) {
                x = MathUtils.random(MyGdxGame.WIDTH);
                y = MathUtils.random(MyGdxGame.HEIGHT);
                dx = x - player.getX();
                dy = y - player.getY();
                dist = (float) Math.sqrt(dx * dx + dy * dy);
            }

            orkAsteroids.add(new OrkAsteroid(x, y, OrkAsteroid.MEDIUM, player, orkBullets));
        }
    }

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

    private void checkCollisions() {

        //player-asteroids
        if(!player.isHit()) {
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
        
        //auto rocket-asteroid collision
        for (int i = 0; i < autoRockets.size(); i++) {
            AutoRocket rocket = autoRockets.get(i);
            for (int j = 0; j < asteroids.size(); j++) {
                Asteroid a = asteroids.get(j);
                if (rocket.intersects(a)) {
                    autoRockets.remove(i);
                    i--;
                    asteroids.remove(j);
                    j--;
                    splitAsteroids(a);
                    break;
                }
            }
        }
        
        //bullet-ork asteroid collision
        for (int i = 0; i < bullets.size(); i++) {
            Bullet b = bullets.get(i);
            for (int j = 0; j < orkAsteroids.size(); j++) {
                OrkAsteroid oa = orkAsteroids.get(j);
                if (oa.intersects(b)) {
                    bullets.remove(i);
                    i--;
                    orkAsteroids.remove(j);
                    j--;
                    createParticles(oa.getX(), oa.getY());
                    break;
                }
            }
        }
        
        //auto rocket-ork asteroid collision
        for (int i = 0; i < autoRockets.size(); i++) {
            AutoRocket rocket = autoRockets.get(i);
            for (int j = 0; j < orkAsteroids.size(); j++) {
                OrkAsteroid oa = orkAsteroids.get(j);
                if (rocket.intersects(oa)) {
                    autoRockets.remove(i);
                    i--;
                    orkAsteroids.remove(j);
                    j--;
                    createParticles(oa.getX(), oa.getY());
                    break;
                }
            }
        }
        
        //ork bullet-player collision
        for (int i = 0; i < orkBullets.size(); i++) {
            OrkBullet ob = orkBullets.get(i);
            if (ob.intersects(player)) {
                orkBullets.remove(i);
                i--;
                player.hit();
                break;
            }
        }
    }

    @Override
    public void update(float dt) {
//        System.out.println("PLAY STATE UPDATING");

        //update camera to follow player
        camera.followTarget(player.getX(), player.getY());
        camera.update(dt);

        //update background
        background.update(dt);

        //get user input
        handleInput();

        //next level
        if(asteroids.size() == 0 && orkAsteroids.size() == 0) {
            level++;
            spawnAsteroids();
            spawnOrkAsteroids();
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
        for(int i = 0; i < particles.size(); i++) {
            particles.get(i).update(dt);
            if(particles.get(i).shouldRemove()) {
                particles.remove(i);
                i--;
            }
        }
        
        //update auto rockets
        for(int i = 0; i < autoRockets.size(); i++) {
            autoRockets.get(i).update(dt);
            if(autoRockets.get(i).shouldRemove()) {
                autoRockets.remove(i);
                i--;
            }
        }
        
        //update ork asteroids
        for(int i = 0; i < orkAsteroids.size(); i++) {
            orkAsteroids.get(i).update(dt);
            if(orkAsteroids.get(i).shouldRemove()) {
                orkAsteroids.remove(i);
                i--;
            }
        }
        
        //update ork bullets
        for(int i = 0; i < orkBullets.size(); i++) {
            orkBullets.get(i).update(dt);
            if(orkBullets.get(i).shouldRemove()) {
                orkBullets.remove(i);
                i--;
            }
        }
        
        //create auto rocket every second
        autoRocketTimer += dt;
        if(autoRocketTimer >= AUTO_ROCKET_INTERVAL) {
            createAutoRocket();
            autoRocketTimer = 0f;
        }

        //check collisions
        checkCollisions();
    }

    @Override
    public void draw() {
//        System.out.println("PLAY STATE DRAWING");

        //draw background first
        background.draw(shapeRenderer);

        //draw player
        player.draw(shapeRenderer, camera);

        //draw bullets
        for (Bullet bullet : bullets) {
            bullet.draw(shapeRenderer, camera);
        }

        //draw asteroids
        for (int i = 0; i < asteroids.size(); i++) {
            asteroids.get(i).draw(shapeRenderer, camera);
        }

        //draw particles
        for(int i = 0; i < particles.size(); i++) {
            particles.get(i).draw(shapeRenderer, camera);
        }
        
        //draw auto rockets
        for(int i = 0; i < autoRockets.size(); i++) {
            autoRockets.get(i).draw(shapeRenderer, camera);
        }
        
        //draw ork asteroids
        for(int i = 0; i < orkAsteroids.size(); i++) {
            orkAsteroids.get(i).draw(shapeRenderer, camera);
        }
        
        //draw ork bullets
        for(int i = 0; i < orkBullets.size(); i++) {
            orkBullets.get(i).draw(shapeRenderer, camera);
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
        
        // Управление зумом
        if (GameKeys.isPressed(GameKeys.ZOOM_IN)) {
            camera.zoomIn();
        }
        if (GameKeys.isPressed(GameKeys.ZOOM_OUT)) {
            camera.zoomOut();
        }
        if (GameKeys.isPressed(GameKeys.ENTER)) {
            camera.resetZoom();
        }
    }

    @Override
    public void dispose() {

    }
}