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
import com.gdx.entities.ExplosionParticle;
import com.gdx.game.MyGdxGame;
import com.gdx.managers.Camera;
import com.gdx.managers.GameKeys;
import com.gdx.managers.GameStateManager;
import com.gdx.managers.AndroidInputManager;
import com.gdx.managers.WorldManager;
import com.gdx.managers.ProgressionManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.ArrayList;
import java.util.List;

public class PlayState extends GameState {

    private ShapeRenderer shapeRenderer;

    private Camera camera;
    private Background background;
    private Player player;
    private ArrayList<Bullet> bullets;
    private ArrayList<Asteroid> asteroids;
    private ArrayList<Enemy> enemies;
    private ArrayList<AutoRocket> autoRockets;
    private ArrayList<OrkAsteroid> orkAsteroids;
    private ArrayList<OrkBullet> orkBullets;

    private ArrayList<Particle> particles;

    private int level;
    private int totalAsteroids;
    private int numAsteroidsLeft;
    private int score;
    private int highScore;
    
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
    
    // Управление бесконечным миром
    private WorldManager worldManager;
    
    // Система прокачки
    private ProgressionManager progressionManager;
    private boolean showUpgradeScreen = false;
    
    // Анимация полоски опыта
    private float experienceBarTimer = 0f;
    private float lastExperienceProgress = 0f;
    private float animatedProgress = 0f;
    private boolean isLevelingUp = false;
    private float levelUpTimer = 0f;
    
    // Флаг для предотвращения повторной инициализации
    private boolean isInitialized = false;
    private boolean isRestoredFromSave = false;
    private boolean autoRocketLeftSide = true; // Флаг для чередования сторон авторакет

    public PlayState(GameStateManager gameStateManager) {
        super(gameStateManager);
    }
    
    public PlayState(GameStateManager gameStateManager, ProgressionManager progressionManager) {
        super(gameStateManager);
        this.progressionManager = progressionManager;
    }

    @Override
    public void init() {
        System.out.println("=== ИНИЦИАЛИЗАЦИЯ PLAYSTATE ===");
        System.out.println("isInitialized: " + isInitialized);
        // Проверяем, была ли уже инициализация
        System.out.println("Проверяем isInitialized: " + isInitialized);
        if (isInitialized) {
            System.out.println("Пропускаем инициализацию - уже инициализировано");
            return;
        }
        
        // Инициализируем только базовые компоненты, которые не зависят от состояния игры
        shapeRenderer = new ShapeRenderer();
        camera = new Camera();
        background = new Background(camera);
        
        // Инициализируем списки объектов (только если они еще не инициализированы)
        if (bullets == null) bullets = new ArrayList<Bullet>();
        if (asteroids == null) asteroids = new ArrayList<>();
        if (enemies == null) enemies = new ArrayList<Enemy>();
        if (autoRockets == null) autoRockets = new ArrayList<AutoRocket>();
        if (orkAsteroids == null) orkAsteroids = new ArrayList<OrkAsteroid>();
        if (orkBullets == null) orkBullets = new ArrayList<OrkBullet>();
        if (particles == null) particles = new ArrayList<Particle>();

        // Инициализируем игровые параметры по умолчанию (только если они еще не инициализированы)
        if (level == 0) level = 1;
        if (score == 0 && asteroids == null) score = 0; // Сбрасываем счет только при новой игре
        if (highScore == 0) highScore = MyGdxGame.highScore; // Загружаем глобальный рекорд

        // Создаем игрока только если он еще не создан (не восстановлен из состояния)
        if (player == null) {
            System.out.println("Создаем нового игрока в центре экрана");
            player = new Player(bullets);
        } else {
            System.out.println("Используем восстановленного игрока в позиции: (" + player.getX() + ", " + player.getY() + ")");
        }

        // Инициализируем WorldManager для бесконечного мира (после создания игрока)
        worldManager = new WorldManager(asteroids, orkAsteroids, orkBullets, player);
        
        // Инициализируем систему прокачки только если её еще нет
        if (progressionManager == null) {
            progressionManager = new ProgressionManager();
            progressionManager.setGameStateManager(gameStateManager);
        }
        
        // Инициализируем FPS счетчик
        fpsLogger = new FPSLogger();
        font = new BitmapFont();
        spriteBatch = new SpriteBatch();
        
        // Инициализируем Android управление
        androidInputManager = new AndroidInputManager();
        
        // Инициализируем списки объектов (только если они еще не инициализированы)
        if (bullets == null) bullets = new ArrayList<Bullet>();
        if (asteroids == null) asteroids = new ArrayList<>();
        if (enemies == null) enemies = new ArrayList<Enemy>();
        if (autoRockets == null) autoRockets = new ArrayList<AutoRocket>();
        if (orkAsteroids == null) orkAsteroids = new ArrayList<OrkAsteroid>();
        if (orkBullets == null) orkBullets = new ArrayList<OrkBullet>();
        if (particles == null) particles = new ArrayList<Particle>();
        
        // Отмечаем, что инициализация завершена
        isInitialized = true;
    }

    private void createParticles(float x, float y) {
        for(int i = 0; i < 6; i++) {
            particles.add(new Particle(x, y));
        }
    }
    
    private void createExplosion(float x, float y, int particleCount) {
        for(int i = 0; i < particleCount; i++) {
            particles.add(new ExplosionParticle(x, y));
        }
    }
    
    private void createShipExplosion(float x, float y) {
        // Создаем много частиц для эффектного взрыва корабля
        for(int i = 0; i < 15; i++) {
            particles.add(new ExplosionParticle(x, y));
        }
        // Добавляем несколько обычных частиц
        for(int i = 0; i < 8; i++) {
            particles.add(new Particle(x, y));
        }
    }
    
    private void addScore(int points) {
        score += points;
        if (score > highScore) {
            highScore = score;
            MyGdxGame.highScore = highScore; // Обновляем глобальный рекорд
        }
        
        // Добавляем опыт за очки
        progressionManager.addExperience(points);
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
            // Определяем сторону для запуска ракеты
            float rocketAngle;
            if (autoRocketLeftSide) {
                // Левая сторона (90 градусов от носа корабля)
                rocketAngle = player.getRadians() + MathUtils.HALF_PI;
            } else {
                // Правая сторона (-90 градусов от носа корабля)
                rocketAngle = player.getRadians() - MathUtils.HALF_PI;
            }
            
            autoRockets.add(new AutoRocket(player.getX(), player.getY(), rocketAngle, cachedNearestEnemy));
            
            // Переключаем сторону для следующей ракеты
            autoRocketLeftSide = !autoRocketLeftSide;
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
        if(!player.isHit() && !player.isInvulnerable()) {
            for (int i = 0; i < asteroids.size(); i++) {
                Asteroid asteroid = asteroids.get(i);

                if (asteroid.intersects(player)) {
                    player.hit();
                    createShipExplosion(player.getX(), player.getY());
                    asteroids.remove(i);
                    i--;
                    splitAsteroids(asteroid);
                    break;
                }
            }
            
            // player-ork asteroids
            for (int i = 0; i < orkAsteroids.size(); i++) {
                OrkAsteroid orkAsteroid = orkAsteroids.get(i);

                if (orkAsteroid.intersects(player)) {
                    player.hit();
                    createShipExplosion(player.getX(), player.getY());
                    // Орк-астероиды не разбиваются при столкновении с игроком
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
                    addScore(10); // 10 очков за обычный астероид
                    createExplosion(a.getX(), a.getY(), 8);
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
                    addScore(10); // 10 очков за обычный астероид
                    createExplosion(a.getX(), a.getY(), 6);
                    splitAsteroids(a);
                    break;
                }
            }
            
            // Проверяем орк-астероиды
            for (int j = 0; j < orkAsteroids.size(); j++) {
                if (j >= 0 && j < orkAsteroids.size()) {
                    OrkAsteroid oa = orkAsteroids.get(j);
                    if (rocket.intersects(oa)) {
                        if (i >= 0 && i < autoRockets.size()) {
                            autoRockets.remove(i);
                            i--;
                        }
                        if (j >= 0 && j < orkAsteroids.size()) {
                            orkAsteroids.remove(j);
                            j--;
                        }
                        addScore(25); // 25 очков за орк-астероид
                        createExplosion(oa.getX(), oa.getY(), 12);
                        break;
                    }
                }
            }
        }
        
        //bullet-ork asteroid collision
        for (int i = 0; i < bullets.size(); i++) {
            if (i >= 0 && i < bullets.size()) {
                Bullet b = bullets.get(i);
                for (int j = 0; j < orkAsteroids.size(); j++) {
                    if (j >= 0 && j < orkAsteroids.size()) {
                        OrkAsteroid oa = orkAsteroids.get(j);
                        if (oa.intersects(b)) {
                            if (i >= 0 && i < bullets.size()) {
                                bullets.remove(i);
                                i--;
                            }
                            if (j >= 0 && j < orkAsteroids.size()) {
                                orkAsteroids.remove(j);
                                j--;
                            }
                            addScore(25); // 25 очков за орк-астероид
                            createExplosion(oa.getX(), oa.getY(), 12);
                            break;
                        }
                    }
                }
            }
        }
        

        
        //ork bullet-player collision
        if(!player.isHit() && !player.isInvulnerable()) {
            for (int i = 0; i < orkBullets.size(); i++) {
                if (i >= 0 && i < orkBullets.size()) {
                    OrkBullet ob = orkBullets.get(i);
                    if (ob.intersects(player)) {
                        if (i >= 0 && i < orkBullets.size()) {
                            orkBullets.remove(i);
                            i--;
                        }
                        player.hit();
                        break;
                    }
                }
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
        
        // Применяем улучшения к игровым объектам (если есть новые)
        progressionManager.applyUpgradesToGameObjects(player, autoRockets, bullets);

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

        // Обновляем бесконечный мир
        worldManager.update(dt, isRestoredFromSave);

        //next level (теперь не нужен, так как мир бесконечный)
        // if(asteroids.size() == 0 && orkAsteroids.size() == 0) {
        //     level++;
        //     spawnAsteroids();
        //     spawnOrkAsteroids();
        // }

        //update player
        long playerStart = System.nanoTime();
        if (player != null) {
            player.update(dt);
            if (player.isDead()) {
                System.out.println("Player умер, сбрасываем...");
                player.reset();
                return;
            }
        } else {
            System.out.println("ОШИБКА: Player == null в update!");
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
        
        // Анимация полоски опыта
        experienceBarTimer += dt;
        float currentProgress = progressionManager.getExperienceProgress();
        
        // Проверяем, произошло ли повышение уровня
        if (currentProgress < lastExperienceProgress) {
            isLevelingUp = true;
            levelUpTimer = 0f;
        }
        
        // Анимация повышения уровня
        if (isLevelingUp) {
            levelUpTimer += dt;
            if (levelUpTimer >= 1.0f) {
                isLevelingUp = false;
                animatedProgress = 0f;
            }
        }
        
        // Плавная анимация прогресса
        if (!isLevelingUp) {
            float targetProgress = currentProgress;
            float diff = targetProgress - animatedProgress;
            if (Math.abs(diff) > 0.001f) {
                animatedProgress += diff * dt * 5f; // Скорость анимации
            }
        }
        
        lastExperienceProgress = currentProgress;
        
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
            System.out.println("World: " + worldManager.getWorldInfo());
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

        // Отрисовка заполненных объектов (фон, пули, ракеты)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        //draw background first
        background.draw(shapeRenderer);
        
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
        
        // Отрисовка линий (корабль, астероиды, частицы)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        
        //draw player

            player.draw(shapeRenderer, camera, spriteBatch);
        

        //draw asteroids
        for (int i = 0; i < asteroids.size(); i++) {
            asteroids.get(i).draw(shapeRenderer, camera);
        }

        shapeRenderer.end();
        
        // Отрисовка заполненных объектов (частицы взрывов)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        //draw particles
        for(int i = 0; i < particles.size(); i++) {
            particles.get(i).draw(shapeRenderer, camera);
        }
        
        shapeRenderer.end();
        
        // Отрисовка линий (обычные частицы)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        
        //draw ork asteroids (контуры)
        for(int i = 0; i < orkAsteroids.size(); i++) {
            orkAsteroids.get(i).draw(shapeRenderer, camera);
        }
        
        shapeRenderer.end();
        
        // Отображаем игровую информацию
        spriteBatch.begin();
        
        // Очки и уровень
        font.setColor(1, 1, 1, 1); // Белый цвет
        font.draw(spriteBatch, "Score: " + score, 10, MyGdxGame.HEIGHT - 10);
        font.draw(spriteBatch, "High: " + highScore, 10, MyGdxGame.HEIGHT - 35);
        
        // Система прокачки
        font.setColor(0, 1, 1, 1); // Голубой цвет для уровня
        font.draw(spriteBatch, "Level: " + progressionManager.getCurrentLevel(), 10, MyGdxGame.HEIGHT - 60);
        
        // Прогресс опыта
        font.setColor(1, 1, 0, 1); // Желтый цвет для опыта
        String expText = "XP: " + progressionManager.getCurrentExperience() + "/" + progressionManager.getExperienceToNextLevel();
        font.draw(spriteBatch, expText, 10, MyGdxGame.HEIGHT - 85);
        
        spriteBatch.end();
        
        // Крутая анимированная полоска прогресса опыта
        float progressBarWidth = 200f;
        float progressBarHeight = 12f;
        float progressBarX = 10f;
        float progressBarY = MyGdxGame.HEIGHT - 95f;
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        // Тень полоски
        shapeRenderer.setColor(0f, 0f, 0f, 0.5f);
        shapeRenderer.rect(progressBarX + 2, progressBarY - 2, progressBarWidth, progressBarHeight);
        
        // Фон полоски с градиентом
        shapeRenderer.setColor(0.2f, 0.2f, 0.3f, 1f);
        shapeRenderer.rect(progressBarX, progressBarY, progressBarWidth, progressBarHeight);
        
        // Внутренний фон с эффектом глубины
        shapeRenderer.setColor(0.1f, 0.1f, 0.2f, 1f);
        shapeRenderer.rect(progressBarX + 1, progressBarY + 1, progressBarWidth - 2, progressBarHeight - 2);
        
        // Анимированное заполнение полоски
        if (isLevelingUp) {
            // Эффект повышения уровня - пульсирующий градиент
            float pulseIntensity = 0.5f + 0.5f * MathUtils.sin(levelUpTimer * 10f);
            shapeRenderer.setColor(1f, 1f, 0f, pulseIntensity);
            shapeRenderer.rect(progressBarX + 2, progressBarY + 2, progressBarWidth - 4, progressBarHeight - 4);
            
            // Дополнительный эффект свечения
            shapeRenderer.setColor(1f, 0.8f, 0f, pulseIntensity * 0.5f);
            shapeRenderer.rect(progressBarX + 2, progressBarY + 2, progressBarWidth - 4, progressBarHeight - 4);
        } else {
            // Обычное заполнение с анимацией
            float fillWidth = (progressBarWidth - 4) * animatedProgress;
            
            // Градиентное заполнение
            for (int i = 0; i < fillWidth; i += 2) {
                float segmentProgress = i / fillWidth;
                float r = 1f;
                float g = 0.5f + 0.5f * segmentProgress;
                float b = 0f;
                float alpha = 0.8f + 0.2f * MathUtils.sin(experienceBarTimer * 3f + i * 0.1f);
                
                shapeRenderer.setColor(r, g, b, alpha);
                shapeRenderer.rect(progressBarX + 2 + i, progressBarY + 2, 2, progressBarHeight - 4);
            }
            
            // Эффект свечения на конце полоски
            if (animatedProgress > 0) {
                float glowIntensity = 0.3f + 0.2f * MathUtils.sin(experienceBarTimer * 5f);
                shapeRenderer.setColor(1f, 1f, 0f, glowIntensity);
                shapeRenderer.rect(progressBarX + 2 + fillWidth - 4, progressBarY + 2, 4, progressBarHeight - 4);
            }
        }
        
        // Рамка полоски
        shapeRenderer.setColor(0.6f, 0.6f, 0.8f, 1f);
        shapeRenderer.rect(progressBarX, progressBarY, progressBarWidth, 2);
        shapeRenderer.rect(progressBarX, progressBarY + progressBarHeight - 2, progressBarWidth, 2);
        shapeRenderer.rect(progressBarX, progressBarY, 2, progressBarHeight);
        shapeRenderer.rect(progressBarX + progressBarWidth - 2, progressBarY, 2, progressBarHeight);
        
        // Дополнительные эффекты
        if (animatedProgress > 0.9f) {
            // Эффект готовности к повышению уровня
            float readyPulse = 0.2f + 0.3f * MathUtils.sin(experienceBarTimer * 8f);
            shapeRenderer.setColor(1f, 0f, 0f, readyPulse);
            shapeRenderer.rect(progressBarX + 2, progressBarY + 2, progressBarWidth - 4, 2);
        }
        
        shapeRenderer.end();
        
        // FPS счетчик если включен
        if (showFPS) {
            font.setColor(1, 1, 0, 1); // Желтый цвет
            font.draw(spriteBatch, "FPS: " + currentFPS, 10, MyGdxGame.HEIGHT - 110);
            
                    // Показываем текущий лимит FPS
        String fpsLimit = MyGdxGame.targetFPS == 0 ? "UNLIMITED" : String.valueOf(MyGdxGame.targetFPS);
        font.draw(spriteBatch, "Limit: " + fpsLimit + " (` ↑ 1 ↓)", 10, MyGdxGame.HEIGHT - 135);
        
        // Показываем выбранные улучшения (справа)
        font.setColor(0, 1, 0, 1); // Зеленый цвет для улучшений
        font.getData().setScale(0.8f);
        font.draw(spriteBatch, "Upgrades:", MyGdxGame.WIDTH - 150, MyGdxGame.HEIGHT - 10);
        
        List<ProgressionManager.Upgrade> selectedUpgrades = progressionManager.getSelectedUpgrades();
        for (int i = 0; i < Math.min(selectedUpgrades.size(), 5); i++) {
            ProgressionManager.Upgrade upgrade = selectedUpgrades.get(i);
            font.draw(spriteBatch, "• " + upgrade.getName(), MyGdxGame.WIDTH - 150, MyGdxGame.HEIGHT - 30 - i * 20);
        }
        
        // Сбрасываем размер шрифта
        font.getData().setScale(1f);
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
        // Отладка клавиш
        if (GameKeys.isPressed(GameKeys.ESCAPE)) {
            System.out.println("ESCAPE нажата!");
        }
        
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
        
        // Возврат в меню с сохранением состояния
        if (GameKeys.isPressed(GameKeys.ESCAPE)) {
            System.out.println("=== ПАУЗА: Сохраняем состояние игры ===");
            System.out.println("Очки: " + score + ", Астероиды: " + asteroids.size());
            if (player != null) {
                System.out.println("Позиция игрока: (" + player.getX() + ", " + player.getY() + ")");
            }
            if (!asteroids.isEmpty()) {
                System.out.println("Позиция первого астероида: (" + asteroids.get(0).getX() + ", " + asteroids.get(0).getY() + ")");
            }
            gameStateManager.setState(GameStateManager.MENU, saveGameState());
        }
        
        // Тестовая кнопка для получения опыта (для тестирования)
        if (GameKeys.isPressed(GameKeys.TEST)) {
            progressionManager.addExperience(50);
        }
    }
    
    // Геттеры для применения улучшений
    public Player getPlayer() {
        return player;
    }
    
    public ArrayList<AutoRocket> getAutoRockets() {
        return autoRockets;
    }
    
    // Методы для сохранения и восстановления состояния
    public com.gdx.managers.GameState saveGameState() {
        System.out.println("=== СОХРАНЕНИЕ СОСТОЯНИЯ ===");
        System.out.println("До сохранения - Очки: " + score + ", Астероиды: " + asteroids.size());
        if (player != null) {
            System.out.println("Player позиция: (" + player.getX() + ", " + player.getY() + ")");
        }
        
        com.gdx.managers.GameState gameState = new com.gdx.managers.GameState();
        gameState.setPlayer(player);
        gameState.setAsteroids(asteroids);
        gameState.setEnemies(enemies);
        gameState.setAutoRockets(autoRockets);
        gameState.setBullets(bullets);
        gameState.setOrkAsteroids(orkAsteroids);
        gameState.setOrkBullets(orkBullets);
        gameState.setParticles(particles);
        gameState.setLevel(level);
        gameState.setTotalAsteroids(totalAsteroids);
        gameState.setNumAsteroidsLeft(numAsteroidsLeft);
        gameState.setScore(score);
        gameState.setHighScore(highScore);
        gameState.setAutoRocketTimer(autoRocketTimer);
        
        System.out.println("Состояние создано - Очки: " + gameState.getScore() + ", Астероиды: " + gameState.getAsteroids().size());
        return gameState;
    }
    
    public void restoreGameState(com.gdx.managers.GameState gameState) {
        if (gameState != null) {
            System.out.println("=== ВОССТАНОВЛЕНИЕ СОСТОЯНИЯ ===");
            
            // Восстанавливаем игровые объекты
            player = gameState.getPlayer();
            asteroids = gameState.getAsteroids();
            enemies = gameState.getEnemies();
            autoRockets = gameState.getAutoRockets();
            bullets = gameState.getBullets();
            orkAsteroids = gameState.getOrkAsteroids();
            orkBullets = gameState.getOrkBullets();
            particles = gameState.getParticles();
            
            // Восстанавливаем игровые параметры
            level = gameState.getLevel();
            totalAsteroids = gameState.getTotalAsteroids();
            numAsteroidsLeft = gameState.getNumAsteroidsLeft();
            score = gameState.getScore();
            highScore = gameState.getHighScore();
            autoRocketTimer = gameState.getAutoRocketTimer();
            
            System.out.println("После восстановления:");
            if (player != null) {
                System.out.println("Позиция игрока: (" + player.getX() + ", " + player.getY() + ")");
                System.out.println("Player объект: " + player.hashCode());
                System.out.println("Player жив: " + !player.isDead());
                System.out.println("Player невидимый: " + player.isInvulnerable());
            } else {
                System.out.println("ОШИБКА: Player == null после восстановления!");
            }
            System.out.println("Астероиды: " + asteroids.size());
            System.out.println("Пули: " + bullets.size());
            System.out.println("Орк-астероиды: " + orkAsteroids.size());
            System.out.println("Орк-пули: " + orkBullets.size());
            if (!asteroids.isEmpty()) {
                System.out.println("Позиция первого астероида: (" + asteroids.get(0).getX() + ", " + asteroids.get(0).getY() + ")");
            }
            
            // Обновляем ссылки в объектах
            if (player != null) {
                // Обновляем ссылку на bullets в Player
                try {
                    java.lang.reflect.Field bulletsField = player.getClass().getDeclaredField("bullets");
                    bulletsField.setAccessible(true);
                    bulletsField.set(player, bullets);
                } catch (Exception e) {
                    // Игнорируем ошибку
                }
            }
            
            // Обновляем WorldManager
            if (worldManager != null) {
                worldManager.updateReferences(asteroids, orkAsteroids, orkBullets, player);
            }
            
            // Проверяем и пересоздаем текстуру корабля, если нужно
            if (player != null) {
                player.recreateTextureIfNeeded();
            }
            
            // Отмечаем, что состояние восстановлено из сохранения
            isRestoredFromSave = true;
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
        if (player != null) {
            player.dispose();
        }
    }
}