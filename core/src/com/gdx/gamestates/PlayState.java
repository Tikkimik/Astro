package com.gdx.gamestates;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.gdx.entities.Asteroid;
import com.gdx.entities.AutoRocket;
import com.gdx.entities.Background;
import com.gdx.entities.Bullet;
import com.gdx.entities.Enemy;
import com.gdx.entities.OrkAsteroid;
import com.gdx.entities.OrkBullet;
import com.gdx.entities.Particle;
import com.gdx.entities.Player;
import com.gdx.game.MyGdxGame;
import com.gdx.managers.Camera;
import com.gdx.managers.GameKeys;
import com.gdx.managers.GameStateManager;
import com.gdx.managers.AndroidInputManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

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
    
    // Кэш для ближайшего врага
    private Enemy cachedNearestEnemy = null;
    private float enemyCacheTimer = 0f;
    private final float ENEMY_CACHE_DURATION = 0.1f; // Обновляем каждые 0.1 секунды
    
    // FPS счетчик
    private FPSLogger fpsLogger;
    private boolean showFPS = false;
    private BitmapFont font;
    private SpriteBatch spriteBatch;
    private int currentFPS = 0;
    private float fpsTimer = 0f;
    
    // Профилирование производительности
    private long lastUpdateTime = 0;
    private long cameraTime = 0, backgroundTime = 0, playerTime = 0;
    private long bulletsTime = 0, asteroidsTime = 0, particlesTime = 0;
    private long rocketsTime = 0, orkTime = 0, collisionsTime = 0;
    private long drawTime = 0;
    
    // Счетчик кадров для FPS
    private int frameCount = 0;
    
    // Android управление
    private AndroidInputManager androidInputManager;

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
        
        // Инициализируем FPS счетчик
        fpsLogger = new FPSLogger();
        font = new BitmapFont();
        spriteBatch = new SpriteBatch();
        
        // Инициализируем Android управление
        androidInputManager = new AndroidInputManager();
    }

    private void createParticles(float x, float y) {
        for(int i = 0; i < 6; i++) {
            particles.add(new Particle(x, y));
        }
    }
    
    // Найти ближайшего врага к игроку (оптимизированная версия)
    private Enemy findNearestEnemy() {
        Enemy nearest = null;
        float minDistanceSquared = Float.MAX_VALUE;
        
        // Проверяем обычные астероиды
        for (Asteroid asteroid : asteroids) {
            float dx = asteroid.getX() - player.getX();
            float dy = asteroid.getY() - player.getY();
            float distanceSquared = dx * dx + dy * dy;
            
            if (distanceSquared < minDistanceSquared) {
                minDistanceSquared = distanceSquared;
                nearest = asteroid;
            }
        }
        
        // Проверяем орк-астероиды
        for (OrkAsteroid orkAsteroid : orkAsteroids) {
            float dx = orkAsteroid.getX() - player.getX();
            float dy = orkAsteroid.getY() - player.getY();
            float distanceSquared = dx * dx + dy * dy;
            
            if (distanceSquared < minDistanceSquared) {
                minDistanceSquared = distanceSquared;
                nearest = orkAsteroid;
            }
        }
        
        return nearest;
    }
    
    // Создать автоматическую ракету
    private void createAutoRocket() {
        if (cachedNearestEnemy != null) {
            autoRockets.add(new AutoRocket(player.getX(), player.getY(), player.getRadians(), cachedNearestEnemy));
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
        
        //auto rocket-enemy collision
        for (int i = 0; i < autoRockets.size(); i++) {
            AutoRocket rocket = autoRockets.get(i);
            
            // Проверяем обычные астероиды
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
            
            // Проверяем орк-астероиды
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

        // Защита от нулевого или отрицательного deltaTime
        if (dt <= 0) {
            System.out.println("WARNING: Invalid deltaTime: " + dt + ", using 0.016f (60 FPS)");
            dt = 0.016f; // Принудительно устанавливаем 60 FPS
        }

        // Начинаем профилирование
        long startTime = System.nanoTime();
        
        //update camera to follow player
        long cameraStart = System.nanoTime();
        camera.followTarget(player.getX(), player.getY());
        camera.update(dt);
        cameraTime = System.nanoTime() - cameraStart;

        //update background
        long backgroundStart = System.nanoTime();
        background.update(dt);
        backgroundTime = System.nanoTime() - backgroundStart;

        //get user input
        handleInput();

        //next level
        if(asteroids.size() == 0 && orkAsteroids.size() == 0) {
            level++;
            spawnAsteroids();
            spawnOrkAsteroids();
        }

        //update player
        long playerStart = System.nanoTime();
        player.update(dt);
        if (player.isDead()) {
            player.reset();
            return;
        }
        playerTime = System.nanoTime() - playerStart;

        //update player bullets
        long bulletsStart = System.nanoTime();
        for (int i = 0; i < bullets.size(); i++) {
            bullets.get(i).update(dt);
            if (bullets.get(i).shouldRemove()) {
                bullets.remove(i);
                i--;
            }
        }
        bulletsTime = System.nanoTime() - bulletsStart;

        //update asteroids
        long asteroidsStart = System.nanoTime();
        for (int i = 0; i < asteroids.size(); i++) {
            asteroids.get(i).update(dt);
            if (asteroids.get(i).shouldRemove()) {
                asteroids.remove(i);
                i--;
            }
        }
        asteroidsTime = System.nanoTime() - asteroidsStart;

        //update particles
        long particlesStart = System.nanoTime();
        for(int i = 0; i < particles.size(); i++) {
            particles.get(i).update(dt);
            if(particles.get(i).shouldRemove()) {
                particles.remove(i);
                i--;
            }
        }
        particlesTime = System.nanoTime() - particlesStart;
        
        //update auto rockets
        long rocketsStart = System.nanoTime();
        for(int i = 0; i < autoRockets.size(); i++) {
            autoRockets.get(i).update(dt);
            if(autoRockets.get(i).shouldRemove()) {
                autoRockets.remove(i);
                i--;
            }
        }
        rocketsTime = System.nanoTime() - rocketsStart;
        
        //update ork asteroids and bullets
        long orkStart = System.nanoTime();
        for(int i = 0; i < orkAsteroids.size(); i++) {
            orkAsteroids.get(i).update(dt);
            if(orkAsteroids.get(i).shouldRemove()) {
                orkAsteroids.remove(i);
                i--;
            }
        }
        
        for(int i = 0; i < orkBullets.size(); i++) {
            orkBullets.get(i).update(dt);
            if(orkBullets.get(i).shouldRemove()) {
                orkBullets.remove(i);
                i--;
            }
        }
        orkTime = System.nanoTime() - orkStart;
        
        // Обновляем кэш ближайшего врага
        enemyCacheTimer += dt;
        if (enemyCacheTimer >= ENEMY_CACHE_DURATION) {
            cachedNearestEnemy = findNearestEnemy();
            enemyCacheTimer = 0f;
        }
        
        //create auto rocket every second
        autoRocketTimer += dt;
        if(autoRocketTimer >= AUTO_ROCKET_INTERVAL) {
            createAutoRocket();
            autoRocketTimer = 0f;
        }

        //check collisions
        long collisionsStart = System.nanoTime();
        checkCollisions();
        collisionsTime = System.nanoTime() - collisionsStart;
        
        // Обновляем FPS счетчик - подсчет кадров
        if (showFPS) {
            frameCount++;
            fpsTimer += dt;
            if (fpsTimer >= 1.0f) {
                currentFPS = frameCount;
                frameCount = 0;
                fpsTimer = 0f;
            }
        }
        
        // Логируем низкий FPS для диагностики с профилированием
        if (currentFPS < 100 && showFPS) {
            long totalTime = cameraTime + backgroundTime + playerTime + bulletsTime + 
                           asteroidsTime + particlesTime + rocketsTime + orkTime + collisionsTime;
            
            System.out.println("=== PERFORMANCE PROFILE ===");
            System.out.println("FPS: " + currentFPS + " | Objects: A=" + asteroids.size() + 
                             " OA=" + orkAsteroids.size() + " B=" + bullets.size() + 
                             " AR=" + autoRockets.size() + " P=" + particles.size());
            System.out.println("Times (μs): Camera=" + (cameraTime/1000) + 
                             " Background=" + (backgroundTime/1000) + 
                             " Player=" + (playerTime/1000) + 
                             " Bullets=" + (bulletsTime/1000));
            System.out.println("          Asteroids=" + (asteroidsTime/1000) + 
                             " Particles=" + (particlesTime/1000) + 
                             " Rockets=" + (rocketsTime/1000) + 
                             " Orks=" + (orkTime/1000));
            System.out.println("          Collisions=" + (collisionsTime/1000) + 
                             " Total=" + (totalTime/1000) + " μs");
            System.out.println("===========================");
        }
    }

    @Override
    public void draw() {
//        System.out.println("PLAY STATE DRAWING");
        long drawStart = System.nanoTime();

        //draw background first
        background.draw(shapeRenderer);

        // Оптимизированная отрисовка - разделяем Line и Filled
        
        // Отрисовка линий (корабль, астероиды, частицы)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        
        //draw player
        player.draw(shapeRenderer, camera);

        //draw asteroids
        for (int i = 0; i < asteroids.size(); i++) {
            asteroids.get(i).draw(shapeRenderer, camera);
        }

        //draw particles
        for(int i = 0; i < particles.size(); i++) {
            particles.get(i).draw(shapeRenderer, camera);
        }
        
        //draw ork asteroids (контуры)
        for(int i = 0; i < orkAsteroids.size(); i++) {
            orkAsteroids.get(i).draw(shapeRenderer, camera);
        }
        
        shapeRenderer.end();
        
        // Отрисовка заполненных объектов (пули, ракеты)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        //draw bullets
        for (Bullet bullet : bullets) {
            bullet.draw(shapeRenderer, camera);
        }
        
        //draw auto rockets
        for(int i = 0; i < autoRockets.size(); i++) {
            autoRockets.get(i).draw(shapeRenderer, camera);
        }
        
        //draw ork bullets
        for(int i = 0; i < orkBullets.size(); i++) {
            orkBullets.get(i).draw(shapeRenderer, camera);
        }
        
        //draw ork asteroids (заполненные части)
        for(int i = 0; i < orkAsteroids.size(); i++) {
            orkAsteroids.get(i).drawFilled(shapeRenderer, camera);
        }
        
        shapeRenderer.end();
        
        // Отображаем FPS счетчик если включен
        if (showFPS) {
            spriteBatch.begin();
            font.setColor(1, 1, 0, 1); // Желтый цвет
            font.draw(spriteBatch, "FPS: " + currentFPS, 10, MyGdxGame.HEIGHT - 10);
            
            // Показываем текущий лимит FPS
            String fpsLimit = MyGdxGame.targetFPS == 0 ? "UNLIMITED" : String.valueOf(MyGdxGame.targetFPS);
            font.draw(spriteBatch, "Limit: " + fpsLimit + " (` ↑ 1 ↓)", 10, MyGdxGame.HEIGHT - 35);
            spriteBatch.end();
        }
        
        // Рендерим Android элементы управления поверх всего
        if (androidInputManager != null && androidInputManager.isAndroid()) {
            androidInputManager.render(shapeRenderer);
        }
        
        // Записываем время отрисовки
        drawTime = System.nanoTime() - drawStart;
    }

    @Override
    public void handleInput() {
        player.setLeft(GameKeys.isDown(GameKeys.LEFT));
        player.setRight(GameKeys.isDown(GameKeys.RIGHT));
        player.setUp(GameKeys.isDown(GameKeys.UP));
        if (GameKeys.isDown(GameKeys.SPACE)) {
            player.shoot();
        }
        
        // Управление зумом - используем isDown для непрерывного действия
        if (GameKeys.isDown(GameKeys.ZOOM_IN)) {
            camera.zoomIn();
        }
        if (GameKeys.isDown(GameKeys.ZOOM_OUT)) {
            camera.zoomOut();
        }
        if (GameKeys.isPressed(GameKeys.ENTER)) {
            camera.resetZoom();
        }
        
        // Переключение FPS счетчика
        if (GameKeys.isPressed(GameKeys.TOGGLE_FPS)) {
            showFPS = !showFPS;
        }
        
        // Управление лимитом FPS
        if (GameKeys.isPressed(GameKeys.FPS_UP)) {
            MyGdxGame.increaseFPS();
        }
        if (GameKeys.isPressed(GameKeys.FPS_DOWN)) {
            MyGdxGame.decreaseFPS();
        }
    }

    @Override
    public void dispose() {
        if (font != null) {
            font.dispose();
        }
        if (spriteBatch != null) {
            spriteBatch.dispose();
        }
    }
}