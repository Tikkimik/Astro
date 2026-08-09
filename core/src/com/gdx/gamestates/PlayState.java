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
import com.gdx.entities.EnemyShip;
import com.gdx.entities.ExplosionParticle;
import com.gdx.entities.FlameParticle;
import com.gdx.entities.OptimizedParticle;
import com.gdx.entities.ShieldParticle;
import com.gdx.game.MyGdxGame;
import com.gdx.managers.Camera;
import com.gdx.managers.GameKeys;
import com.gdx.managers.GameStateManager;
import com.gdx.managers.AndroidInputManager;
import com.gdx.managers.WorldManager;
import com.gdx.managers.ProgressionManager;
import com.gdx.managers.GameObjectManager;
import com.gdx.entities.GameObject;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

import com.badlogic.gdx.utils.Array;
import java.util.ArrayList;
import java.util.List;
import com.gdx.utils.ObjectPools;
import com.gdx.utils.PerformanceMetrics;
import com.gdx.utils.PerformanceMonitor;
import com.gdx.utils.TimeManager;
import com.gdx.utils.GameSettings;
import com.gdx.utils.RenderStats;
import com.gdx.utils.CameraBounds;

public class PlayState extends GameState {

    private ShapeRenderer shapeRenderer;

    private Camera camera;
    private Background background;
    private Player player;
    private GameObjectManager gameObjectManager;
    
    // Оставляем только частицы, так как они обрабатываются по-другому
    private Array<Particle> particles;
    private Array<OptimizedParticle> optimizedParticles;

    private int level;
    private int totalAsteroids;
    private int numAsteroidsLeft;
    private int score;
    private int highScore;

    // Счётчики рендера (culling) и копия границ камеры для ParticleBudget
    private final RenderStats renderStats = new RenderStats();
    private final CameraBounds cameraBounds = new CameraBounds();
    
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
    private FreeTypeFontGenerator fontGenerator;
    private SpriteBatch spriteBatch;
    private int currentFPS = 0;
    private float fpsTimer = 0f;

    // Нижняя граница текстового HUD-блока (используется для отступа Debug menu)
    private float hudBottomY = 0f;
    
    // Система паузы
    private boolean isPaused = false;
    private float pauseTime = 0f;
    private float totalPauseTime = 0f;
    private boolean pauseOverlayVisible = true;
    private float pauseOverlayTimer = 0f;
    
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
        
        // Инициализируем GameObjectManager для управления всеми объектами
        if (gameObjectManager == null) {
            gameObjectManager = new GameObjectManager();
            gameObjectManager.setAsteroidDestroyedListener((asteroid, x, y) -> {
                createParticles(x, y);
                numAsteroidsLeft--;
            });
        }
        
        // Инициализируем только частицы (они обрабатываются по-другому)
        if (particles == null) particles = new Array<Particle>();
        if (optimizedParticles == null) optimizedParticles = new Array<OptimizedParticle>();
        com.gdx.utils.ParticleBudget.register(particles);
        com.gdx.utils.ParticleBudget.register(optimizedParticles);

        // Инициализируем игровые параметры по умолчанию (только если они еще не инициализированы)
        if (level == 0) level = 1;
        if (score == 0) score = 0; // Сбрасываем счет только при новой игре
        if (highScore == 0) highScore = MyGdxGame.highScore; // Загружаем глобальный рекорд

        // Создаем игрока только если он еще не создан (не восстановлен из состояния)
        if (player == null) {
            System.out.println("Создаем нового игрока в центре экрана");
                    player = new Player(null); // Передаем null, так как теперь используем GameObjectManager
        player.setGameObjectManager(gameObjectManager);
        gameObjectManager.setPlayer(player);
        } else {
            System.out.println("Используем восстановленного игрока в позиции: (" + player.getX() + ", " + player.getY() + ")");
            gameObjectManager.setPlayer(player);
        }

        // Инициализируем WorldManager для бесконечного мира (после создания игрока)
        // Передаем ссылки на GameObjectManager вместо отдельных списков
        worldManager = new WorldManager(gameObjectManager, player);
        
        // Начальные объекты создаются в WorldManager
        
        // Инициализируем систему прокачки только если её еще нет
        if (progressionManager == null) {
            progressionManager = new ProgressionManager();
            progressionManager.setGameStateManager(gameStateManager);
        }
        
        // Инициализируем FPS счетчик
        fpsLogger = new FPSLogger();
        createUiFont();
        spriteBatch = new SpriteBatch();
        
        // Инициализируем Android управление
        androidInputManager = new AndroidInputManager();
        
        // Отмечаем, что инициализация завершена
        isInitialized = true;
    }
    
    private void spawnInitialObjects() {
        System.out.println("Создаем начальные объекты для тестирования...");
        
        // Создаем несколько астероидов
        for (int i = 0; i < 5; i++) {
            float x = MathUtils.random(100, MyGdxGame.WIDTH - 100);
            float y = MathUtils.random(100, MyGdxGame.HEIGHT - 100);
            gameObjectManager.addAsteroid(new Asteroid(x, y, Asteroid.LARGE));
        }
        
        // Создаем несколько орк-астероидов
        for (int i = 0; i < 3; i++) {
            float x = MathUtils.random(100, MyGdxGame.WIDTH - 100);
            float y = MathUtils.random(100, MyGdxGame.HEIGHT - 100);
            gameObjectManager.addOrkAsteroid(new OrkAsteroid(x, y, OrkAsteroid.MEDIUM, player, null));
        }
        
        // Создаем несколько вражеских кораблей
        for (int i = 0; i < 2; i++) {
            float x = MathUtils.random(100, MyGdxGame.WIDTH - 100);
            float y = MathUtils.random(100, MyGdxGame.HEIGHT - 100);
            gameObjectManager.addEnemyShip(new EnemyShip(x, y, player, null));
        }
        
        System.out.println("Начальные объекты созданы: " + gameObjectManager.getStats());
    }

    private void createParticles(float x, float y) {
        // Взрыв за пределами расширенной области спавна не создаём: эффект не будет виден
        if (!com.gdx.utils.ParticleBudget.isInSpawnBounds(x, y)) return;
        for(int i = 0; i < 6 && com.gdx.utils.ParticleBudget.canSpawn(x, y); i++) {
            particles.add(new Particle(x, y));
            com.gdx.utils.ParticleBudget.add(1);
        }
    }
    
    private void createExplosion(float x, float y, int particleCount) {
        if (!com.gdx.utils.ParticleBudget.isInSpawnBounds(x, y)) return;
        for(int i = 0; i < particleCount && com.gdx.utils.ParticleBudget.canSpawn(x, y); i++) {
            particles.add(new ExplosionParticle(x, y));
            com.gdx.utils.ParticleBudget.add(1);
        }
    }
    
    private void createShipExplosion(float x, float y) {
        // Создаем много частиц для эффектного взрыва корабля
        for(int i = 0; i < 15 && com.gdx.utils.ParticleBudget.canSpawn(x, y); i++) {
            particles.add(new ExplosionParticle(x, y));
            com.gdx.utils.ParticleBudget.add(1);
        }
        // Добавляем несколько обычных частиц
        for(int i = 0; i < 8 && com.gdx.utils.ParticleBudget.canSpawn(x, y); i++) {
            particles.add(new Particle(x, y));
            com.gdx.utils.ParticleBudget.add(1);
        }
    }
    
    // Методы для создания оптимизированных частиц
    private void createOptimizedExplosion(float x, float y, int particleCount) {
        if (!com.gdx.utils.ParticleBudget.isInSpawnBounds(x, y)) return;
        for(int i = 0; i < particleCount && com.gdx.utils.ParticleBudget.canSpawn(x, y); i++) {
            optimizedParticles.add(ObjectPools.obtainOptimizedParticle(x, y, "explosion"));
            com.gdx.utils.ParticleBudget.add(1);
        }
    }
    
    private void createOptimizedFlameParticles(float x, float y, int particleCount) {
        if (!com.gdx.utils.ParticleBudget.isInSpawnBounds(x, y)) return;
        for(int i = 0; i < particleCount && com.gdx.utils.ParticleBudget.canSpawn(x, y); i++) {
            optimizedParticles.add(ObjectPools.obtainOptimizedParticle(x, y, "flame"));
            com.gdx.utils.ParticleBudget.add(1);
        }
    }
    
    private void createOptimizedSmokeParticles(float x, float y, int particleCount) {
        if (!com.gdx.utils.ParticleBudget.isInSpawnBounds(x, y)) return;
        for(int i = 0; i < particleCount && com.gdx.utils.ParticleBudget.canSpawn(x, y); i++) {
            optimizedParticles.add(ObjectPools.obtainOptimizedParticle(x, y, "smoke"));
            com.gdx.utils.ParticleBudget.add(1);
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
        
        // Проверяем все препятствия через GameObjectManager
        for (GameObject obj : gameObjectManager.getObstacles()) {
            float dx = obj.getX() - player.getX();
            float dy = obj.getY() - player.getY();
            float distanceSquared = dx * dx + dy * dy;
            
            if (distanceSquared < minDistanceSquared) {
                minDistanceSquared = distanceSquared;
                if (obj.isAsteroid()) {
                    nearest = obj.as(Asteroid.class);
                } else if (obj.isOrkAsteroid()) {
                    nearest = obj.as(OrkAsteroid.class);
                }
            }
        }
        
        return nearest;
    }
    
    // Создать автоматическую ракету
    private void createAutoRocket() {
        if (cachedNearestEnemy != null) {
            float dx = cachedNearestEnemy.getX() - player.getX();
            float dy = cachedNearestEnemy.getY() - player.getY();
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist > AutoRocket.DETECTION_RANGE) return;
            
            float rocketAngle;
            if (autoRocketLeftSide) {
                rocketAngle = player.getRadians() + MathUtils.HALF_PI;
            } else {
                rocketAngle = player.getRadians() - MathUtils.HALF_PI;
            }
            
            gameObjectManager.addAutoRocket(com.gdx.utils.ObjectPools.obtainAutoRocket(player.getX(), player.getY(), rocketAngle, cachedNearestEnemy));
            
            autoRocketLeftSide = !autoRocketLeftSide;
        }
    }

    private void spawnAsteroids() {
        // Очищаем старые астероиды через GameObjectManager
        // GameObjectManager сам управляет очисткой

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

            gameObjectManager.addAsteroid(new Asteroid(x, y, Asteroid.LARGE));
        }
    }
    
    private void spawnOrkAsteroids() {
        // GameObjectManager сам управляет очисткой
        
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

            gameObjectManager.addOrkAsteroid(new OrkAsteroid(x, y, OrkAsteroid.MEDIUM, player, null));
        }
    }

    private void checkCollisions() {
        if(!player.isHit() && !player.isInvulnerable()) {
            for (GameObject obj : gameObjectManager.getObstacles()) {
                if (obj.intersects(player)) {
                    player.hit();
                    createShipExplosion(player.getX(), player.getY());
                    
                    if (obj.isAsteroid()) {
                        Asteroid asteroid = obj.as(Asteroid.class);
                        if (asteroid != null) {
                            gameObjectManager.splitAsteroid(asteroid);
                        }
                    }
                    break;
                }
            }
        }
    }

    @Override
    public void update(float dt) {
//        System.out.println("PLAY STATE UPDATING");

        // Начинаем мониторинг производительности
        PerformanceMonitor.startFrame();

        // Защита от нулевого или отрицательного deltaTime
        if (dt <= 0) {
            System.out.println("WARNING: Invalid deltaTime: " + dt + ", using 0.016f (60 FPS)");
            dt = 0.016f; // Принудительно устанавливаем 60 FPS
        }
        
        // СТАРАЯ СИСТЕМА ПАУЗЫ - ЗАКОММЕНТИРОВАНА
        // Теперь пауза управляется через новую систему состояний
        /*
        if (isPaused) {
            pauseTime += dt;
            totalPauseTime += dt;
            // Обрабатываем только ввод для паузы, но пропускаем всю игровую логику
            handlePauseInput();
            return;
        }
        */
        
        // Применяем улучшения к игровым объектам (если есть новые)
        // progressionManager.applyUpgradesToGameObjects(player, autoRockets, bullets);

        // Начинаем профилирование
        long startTime = System.nanoTime();
        
        //update camera to follow player
        long cameraStart = System.nanoTime();
        camera.followTarget(player.getX(), player.getY());
        camera.update(dt);
        cameraTime = System.nanoTime() - cameraStart;

        // Копия границ камеры для ParticleBudget (решения о спавне частиц)
        cameraBounds.set(camera.getRenderLeft(), camera.getRenderRight(), camera.getRenderBottom(), camera.getRenderTop(),
                camera.getSpawnLeft(), camera.getSpawnRight(), camera.getSpawnBottom(), camera.getSpawnTop());
        com.gdx.utils.ParticleBudget.updateBounds(cameraBounds);

        //update background
        long backgroundStart = System.nanoTime();
        background.update(dt);
        backgroundTime = System.nanoTime() - backgroundStart;

        //get user input
        handleInput();

        // Обновляем бесконечный мир
        worldManager.update(dt, isRestoredFromSave);

        //next level (теперь не нужен, так как мир бесконечный)
        // if(asteroids.size == 0 && orkAsteroids.size == 0) {
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
        // Обновляем все объекты через GameObjectManager
        gameObjectManager.update(dt);
        bulletsTime = System.nanoTime() - bulletsStart;

        //update asteroids
        long asteroidsStart = System.nanoTime();
        // GameObjectManager уже обновил все объекты
        asteroidsTime = System.nanoTime() - asteroidsStart;

        //update particles
        long particlesStart = System.nanoTime();
        for(int i = 0; i < particles.size; i++) {
            particles.get(i).update(dt);
            if(particles.get(i).shouldRemove()) {
                particles.removeIndex(i);
                com.gdx.utils.ParticleBudget.release(1);
                i--;
            }
        }
        
        //update optimized particles
        for(int i = 0; i < optimizedParticles.size; i++) {
            optimizedParticles.get(i).update(dt);
            if(optimizedParticles.get(i).shouldRemove()) {
                ObjectPools.freeOptimizedParticle(optimizedParticles.get(i));
                optimizedParticles.removeIndex(i);
                com.gdx.utils.ParticleBudget.release(1);
                i--;
            }
        }
        particlesTime = System.nanoTime() - particlesStart;
        
                //update auto rockets
        long rocketsStart = System.nanoTime();
        // GameObjectManager уже обновил все объекты
        rocketsTime = System.nanoTime() - rocketsStart;
        
        //update ork asteroids and bullets
        long orkStart = System.nanoTime();
        // GameObjectManager уже обновил все объекты
        
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
        
        // GameObjectManager уже обновил все объекты
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
        gameObjectManager.checkCollisions();
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
        
        // Завершаем мониторинг производительности
        PerformanceMonitor.endFrame();
        
        // Логируем низкий FPS для диагностики с профилированием
        if (currentFPS < 100 && showFPS) {
            long totalTime = cameraTime + backgroundTime + playerTime + bulletsTime + 
                           asteroidsTime + particlesTime + rocketsTime + orkTime + collisionsTime;
            
            System.out.println("=== PERFORMANCE PROFILE ===");
            System.out.println("FPS: " + currentFPS + " | Objects: " + gameObjectManager.getStats() + 
                             " P=" + particles.size);
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

        // Сброс счётчиков рендера на начало кадра
        renderStats.beginFrame();

        // Отрисовка заполненных объектов (фон)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
//draw background first
        background.draw(shapeRenderer, camera);
        
        // Отрисовка всех игровых объектов через GameObjectManager (с culling:
        // снаряды, препятствия, враги и следы ракет за камерой в renderer не идут)
        gameObjectManager.draw(shapeRenderer, camera, spriteBatch, renderStats);
        
        shapeRenderer.end();
        
        // Отрисовка линий (корабль). Игрок рисуется здесь один раз за кадр —
        // раньше он попадал в рендер и из allObjects менеджера, и этим вызовом,
        // из-за чего корабль, пламя и щит рисовались дважды.
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        
        float playerRadius = Math.max(1f, player.getWidth() / 2f);
        int flameCount = player.getFlameParticles().size;
        int shieldCount = player.getShieldParticles().size;
        boolean playerInView = camera.isInRenderBounds(player.getX(), player.getY(), playerRadius);
        renderStats.particlesActive += flameCount + shieldCount;
        if (playerInView) {
            player.draw(shapeRenderer, camera, spriteBatch);
        }
        // Пламя и щит игрока считаем поштучно (с culling): внеэкранные частицы
        // не должны расходовать видимый бюджет и забирать слоты у эффектов на экране
        if (playerInView) {
            for (FlameParticle fp : player.getFlameParticles()) {
                if (camera.isInRenderBounds(fp.getX(), fp.getY(), fp.getCullRadius())) {
                    renderStats.particlesVisible++;
                    renderStats.particlesDrawn++;
                } else {
                    renderStats.particlesCulled++;
                }
            }
            for (ShieldParticle sp : player.getShieldParticles()) {
                if (camera.isInRenderBounds(sp.getX(), sp.getY(), sp.getCullRadius())) {
                    renderStats.particlesVisible++;
                    renderStats.particlesDrawn++;
                } else {
                    renderStats.particlesCulled++;
                }
            }
        } else {
            renderStats.particlesCulled += flameCount + shieldCount;
        }

        shapeRenderer.end();
        
        // Отрисовка заполненных объектов (частицы взрывов) — с culling
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        //draw particles
        for(int i = 0; i < particles.size; i++) {
            Particle p = particles.get(i);
            renderStats.particlesActive++;
            if (camera.isInRenderBounds(p.getX(), p.getY(), p.getCullRadius())) {
                renderStats.particlesVisible++;
                p.draw(shapeRenderer, camera);
                renderStats.particlesDrawn++;
            } else {
                renderStats.particlesCulled++;
            }
        }
        
        shapeRenderer.end();
        
        // Оптимизированные частицы рисуются через SpriteBatch (текстуры).
        // begin()/end() вынесены сюда: раньше batch.draw вызывался без begin().
        if (optimizedParticles.size > 0) {
            spriteBatch.begin();
            //draw optimized particles
            for(int i = 0; i < optimizedParticles.size; i++) {
                OptimizedParticle p = optimizedParticles.get(i);
                renderStats.particlesActive++;
                if (camera.isInRenderBounds(p.getX(), p.getY(), p.getCullRadius())) {
                    renderStats.particlesVisible++;
                    p.draw(spriteBatch, camera);
                    renderStats.particlesDrawn++;
                } else {
                    renderStats.particlesCulled++;
                }
            }
            spriteBatch.end();
        }
        
        // Итоговая категория «Objects»: все мировые объекты, включая игрока
        renderStats.objectsActive = gameObjectManager.getActiveObjects();
        renderStats.objectsVisible = renderStats.projectilesVisible + renderStats.obstaclesVisible
                + renderStats.enemiesVisible + (playerInView ? 1 : 0);
        renderStats.objectsDrawn = renderStats.projectilesDrawn + renderStats.obstaclesDrawn
                + renderStats.enemiesDrawn + (playerInView ? 1 : 0);
        renderStats.objectsCulled = renderStats.objectsActive - renderStats.objectsVisible;

        // Видимый счётчик частиц для ParticleBudget — по итогам реального render-кадра
        com.gdx.utils.ParticleBudget.setVisible(renderStats.particlesVisible);

        // Отображаем игровую информацию (экранные координаты, не зависят от камеры мира)
        spriteBatch.begin();
        drawHudText();
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
        
        // Детальная F3-табличка с полезной информацией (клавиша F)
        // Начинаем ниже HUD-блока с безопасным отступом
        if (showFPS) {
            drawDebugTable(hudBottomY - 16f);
        }
        
        // СТАРАЯ СИСТЕМА ПАУЗЫ - ЗАКОММЕНТИРОВАНА
        // Теперь пауза управляется через новую систему состояний
        /*
        if (isPaused) {
            // Анимированный полупрозрачный фон с пульсацией
            float pulseAlpha = 0.6f + 0.1f * (float)Math.sin(pauseTime * 2f);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0, 0, 0, pulseAlpha);
            shapeRenderer.rect(0, 0, MyGdxGame.WIDTH, MyGdxGame.HEIGHT);
            shapeRenderer.end();
            
            // Рамка вокруг экрана
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(1, 1, 1, 0.3f);
            shapeRenderer.rect(10, 10, MyGdxGame.WIDTH - 20, MyGdxGame.HEIGHT - 20);
            shapeRenderer.end();
            
            // Основной текст паузы с анимацией
            font.setColor(1, 1, 1, 1);
            font.getData().setScale(3f);
            String pauseText = "⏸ ПАУЗА ⏸";
            float textWidth = font.draw(spriteBatch, pauseText, 0, 0).width;
            float textY = MyGdxGame.HEIGHT / 2 + 80;
            font.draw(spriteBatch, pauseText, 
                     (MyGdxGame.WIDTH - textWidth) / 2, 
                     textY);
            
            // Подсказка с миганием
            font.getData().setScale(1.2f);
            String hintText = "Нажмите ESC для продолжения";
            textWidth = font.draw(spriteBatch, hintText, 0, 0).width;
            float blinkAlpha = 0.5f + 0.5f * (float)Math.abs(Math.sin(pauseTime * 3f));
            font.setColor(1, 1, 1, blinkAlpha);
            font.draw(spriteBatch, hintText, 
                     (MyGdxGame.WIDTH - textWidth) / 2, 
                     MyGdxGame.HEIGHT / 2 - 10);
            
            // Время в паузе
            font.setColor(0.9f, 0.9f, 0.9f, 1);
            String timeText = String.format("⏱ Время в паузе: %.1f сек", pauseTime);
            textWidth = font.draw(spriteBatch, timeText, 0, 0).width;
            font.draw(spriteBatch, timeText, 
                     (MyGdxGame.WIDTH - textWidth) / 2, 
                     MyGdxGame.HEIGHT / 2 - 60);
            
            // Общее время в паузе
            font.setColor(0.7f, 0.7f, 0.7f, 1);
            String totalTimeText = String.format("📊 Общее время: %.1f сек", totalPauseTime);
            textWidth = font.draw(spriteBatch, totalTimeText, 0, 0).width;
            font.draw(spriteBatch, totalTimeText, 
                     (MyGdxGame.WIDTH - textWidth) / 2, 
                     MyGdxGame.WIDTH - textWidth) / 2, 
                     MyGdxGame.HEIGHT / 2 - 90);
            
            // Дополнительные подсказки
            font.getData().setScale(0.9f);
            font.setColor(0.6f, 0.6f, 0.6f, 1);
            String extraHint = "💡 Игра продолжает отрисовываться";
            textWidth = font.draw(spriteBatch, extraHint, 0, 0).width;
            font.draw(spriteBatch, extraHint, 
                     (MyGdxGame.WIDTH - textWidth) / 2, 
                     MyGdxGame.HEIGHT / 2 - 120);
            
            // Подсказка о выходе в меню
            String menuHint = "🎮 Нажмите TAB для выхода в меню";
            textWidth = font.draw(spriteBatch, menuHint, 0, 0).width;
            font.draw(spriteBatch, menuHint, 
                     (MyGdxGame.WIDTH - textWidth) / 2, 
                     MyGdxGame.HEIGHT / 2 - 150);
            
            // Сбрасываем размер шрифта
            font.getData().setScale(1f);
        }
        */
        
        // Рендерим Android элементы управления поверх всего
        if (androidInputManager != null && androidInputManager.isAndroid()) {
            androidInputManager.render(shapeRenderer);
        }
        
        // Записываем время отрисовки
        drawTime = System.nanoTime() - drawStart;
    }

    // СТАРАЯ СИСТЕМА ПАУЗЫ - ЗАКОММЕНТИРОВАНА
    // Теперь пауза управляется через новую систему состояний
    /*
    // Метод для обработки ввода в паузе
    private void handlePauseInput() {
        // Выход в меню из паузы (TAB для Menu)
        if (GameKeys.isPressed(GameKeys.TAB)) {
            System.out.println("=== ВЫХОД В МЕНЮ ИЗ ПАУЗЫ ===");
            gameStateManager.setState(GameStateManager.MENU, saveGameState());
        }
        
        // Переключение паузы (ESC)
        if (GameKeys.isPressed(GameKeys.ESCAPE)) {
            isPaused = false;
            System.out.println("=== ПАУЗА ОТКЛЮЧЕНА ===");
            System.out.println("Общее время в паузе: " + totalPauseTime + " сек");
        }
    }
    */
    
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
        
        // СТАРАЯ СИСТЕМА ПАУЗЫ - ЗАКОММЕНТИРОВАНА
        // Теперь пауза управляется через новую систему состояний
        /*
        if (GameKeys.isPressed(GameKeys.ESCAPE)) {
            isPaused = !isPaused;
            if (isPaused) {
                System.out.println("=== ПАУЗА АКТИВИРОВАНА ===");
                pauseTime = 0f;
                pauseOverlayTimer = 0f;
            } else {
                System.out.println("=== ПАУЗА ОТКЛЮЧЕНА ===");
                System.out.println("Общее время в паузе: " + totalPauseTime + " сек");
            }
        }
        */
        
        // Возврат в меню с сохранением состояния (только если не в паузе)
        // if (GameKeys.isPressed(GameKeys.ESCAPE) && !isPaused) {
        //     System.out.println("=== ВОЗВРАТ В МЕНЮ: Сохраняем состояние игры ===");
        //     System.out.println("Очки: " + score + ", Астероиды: " + asteroids.size);
        //     if (player != null) {
        //         System.out.println("Позиция игрока: (" + player.getX() + ", " + player.getY() + ")");
        //     }
        //     if (!asteroids.isEmpty()) {
        //         System.out.println("Позиция первого астероида: (" + asteroids.get(0).getX() + ", " + asteroids.get(0).getY() + ")");
        //     }
        //     gameStateManager.setState(GameStateManager.MENU, saveGameState());
        // }
        
        // Тестовая кнопка для получения опыта (для тестирования)
        if (GameKeys.isPressed(GameKeys.TEST)) {
            progressionManager.addExperience(50);
        }
    }
    
    // Геттеры для применения улучшений
    public Player getPlayer() {
        return player;
    }
    
    public Array<AutoRocket> getAutoRockets() {
        // Возвращаем пустой массив, так как теперь используем GameObjectManager
        return new Array<AutoRocket>();
    }
    
    // Методы для сохранения и восстановления состояния
    public com.gdx.managers.GameState saveGameState() {
        System.out.println("=== СОХРАНЕНИЕ СОСТОЯНИЯ ===");
        System.out.println("До сохранения - Очки: " + score + ", Объекты: " + gameObjectManager.getStats());
        if (player != null) {
            System.out.println("Player позиция: (" + player.getX() + ", " + player.getY() + ")");
        }
        
        com.gdx.managers.GameState gameState = new com.gdx.managers.GameState();
        gameState.setPlayer(player);
        // GameObjectManager сам управляет всеми объектами
        gameState.setParticles(particles);
        gameState.setLevel(level);
        gameState.setTotalAsteroids(totalAsteroids);
        gameState.setNumAsteroidsLeft(numAsteroidsLeft);
        gameState.setScore(score);
        gameState.setHighScore(highScore);
        gameState.setAutoRocketTimer(autoRocketTimer);
        
        System.out.println("Состояние создано - Очки: " + gameState.getScore() + ", Объекты: " + gameObjectManager.getStats());
        return gameState;
    }
    
    public void restoreGameState(com.gdx.managers.GameState gameState) {
        if (gameState != null) {
            System.out.println("=== ВОССТАНОВЛЕНИЕ СОСТОЯНИЯ ===");
            
            // Восстанавливаем игровые объекты
            player = gameState.getPlayer();
            // GameObjectManager сам управляет всеми объектами
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
            System.out.println("Объекты: " + gameObjectManager.getStats());
            
            // Обновляем ссылки в объектах
            if (player != null) {
                // GameObjectManager сам управляет всеми объектами
            }
            
            // Обновляем WorldManager
            if (worldManager != null) {
                worldManager.updateReferences(gameObjectManager, player);
            }
            
            // Проверяем и пересоздаем текстуру корабля, если нужно
            if (player != null) {
                player.recreateTextureIfNeeded();
            }
            
            // Отмечаем, что состояние восстановлено из сохранения
            isRestoredFromSave = true;
        }
    }

    // Генерируем шрифт с поддержкой кириллицы через gdx-freetype.
    // Стандартный BitmapFont содержит только ASCII, поэтому русский текст
    // превращался в квадраты. DejaVu Sans — свободный шрифт с кириллицей.
    private void createUiFont() {
        fontGenerator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/DejaVuSans.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();

        // Размер близкий к стандартному BitmapFont (15px), чтобы текст не выглядел иначе
        param.size = 16;
        param.characters = buildFontCharacters();
        // Линейная фильтрация — чёткий текст без размытия
        param.minFilter = Texture.TextureFilter.Linear;
        param.magFilter = Texture.TextureFilter.Linear;

        font = fontGenerator.generateFont(param);
    }

    // Набор глифов: ASCII + кириллица (включая дополнение) + спецсимволы.
    // Диапазоны формируются программно, чтобы ничего не пропустить.
    private String buildFontCharacters() {
        StringBuilder sb = new StringBuilder();

        // Печатные ASCII-символы (код 32–126)
        for (int i = 32; i <= 126; i++) {
            sb.append((char) i);
        }

        // Кириллица U+0400–U+04FF (включая Ё = U+0401 и ё = U+0451)
        for (int i = 0x0400; i <= 0x04FF; i++) {
            sb.append((char) i);
        }

        // Кириллическое дополнение U+0500–U+052F
        for (int i = 0x0500; i <= 0x052F; i++) {
            sb.append((char) i);
        }

        // Спецсимволы, реально используемые в интерфейсе: тире, градус и пр.
        sb.append("—–…°`•»«±→↓↑×·“”");

        // Служебные символы: перевод строки и табуляция
        sb.append('\n').append('\t');

        return sb.toString();
    }

    // Основной HUD (Score/High/Level/XP) рисуется курсором сверху вниз,
    // позиции строк зависят только от метрик шрифта и не пересекаются.
    private void drawHudText() {
        float x = 10f;
        float lineH = font.getLineHeight(); // высота строки для масштаба 1.0
        float spacing = 4f; // отступ между строками HUD
        float y = MyGdxGame.HEIGHT - 8f; // базовая линия первой строки

        font.setColor(1, 1, 1, 1); // Белый цвет
        font.draw(spriteBatch, "Score: " + score, x, y); y -= lineH + spacing;

        font.setColor(1, 1, 1, 1);
        font.draw(spriteBatch, "High: " + highScore, x, y); y -= lineH + spacing;

        font.setColor(0, 1, 1, 1); // Голубой цвет для уровня
        font.draw(spriteBatch, "Level: " + progressionManager.getCurrentLevel(), x, y); y -= lineH + spacing;

        font.setColor(1, 1, 0, 1); // Желтый цвет для опыта
        String expText = "XP: " + progressionManager.getCurrentExperience() + "/" + progressionManager.getExperienceToNextLevel();
        font.draw(spriteBatch, expText, x, y);

        // Нижняя граница HUD-текста (низ последней строки)
        hudBottomY = y - lineH;
    }

    // Детальная F3-табличка (клавиша F) в стиле Minecraft.
    // Все строки раскладываются вертикальным курсором: высота строки берётся из
    // метрик шрифта, у заголовков секций — увеличенный отступ.
    // Текст рисуется прямо поверх игрового мира, без панели-подложки.
    private void drawDebugTable(float startY) {
        List<DebugEntry> lines = buildDebugEntries();

        font.getData().setScale(0.8f);
        float scale = font.getData().scaleY;
        float lineH = font.getLineHeight() * scale; // высота строки для текущего масштаба
        float spacing = 3f;  // межстрочный отступ
        float headerGap = 7f; // дополнительный отступ перед заголовком секции

        // Вычисляем шаги строк (высота из метрик шрифта + отступы)
        float[] advances = new float[lines.size()];
        for (int i = 0; i < lines.size(); i++) {
            DebugEntry e = lines.get(i);
            float adv = lineH + spacing;
            if (e.header && i > 0) {
                adv += headerGap;
            }
            advances[i] = adv;
        }

        // Текст поверх игрового мира
        spriteBatch.begin();
        float x = 10f;
        float y = startY;
        for (int i = 0; i < lines.size(); i++) {
            DebugEntry e = lines.get(i);
            font.setColor(e.r, e.g, e.b, 1f);
            font.draw(spriteBatch, e.text, x, y);
            y -= advances[i];
        }
        spriteBatch.end();

        font.getData().setScale(1f);
    }

    private List<DebugEntry> buildDebugEntries() {
        List<DebugEntry> list = new ArrayList<>();

        // === Производительность ===
        list.add(new DebugEntry("--- Debug menu (F чтобы скрыть) ---", 1f, 1f, 0f, true));

        // Фактические значения измеряются в общем окне реального времени
        // (PerformanceMetrics): кадры рендера и тики игровой логики считаются
        // отдельно и независимо, лимит и цель показываются как настройки, а не факт.
        int renderFps = Math.round(PerformanceMetrics.getRenderFps());
        int simUps = Math.round(PerformanceMetrics.getSimulationUps());
        String cap = MyGdxGame.targetFPS == 0 ? "Unlimited" : String.valueOf(MyGdxGame.targetFPS);
        String fixedDtMs = String.format("%.2f", TimeManager.getFixedStep() * 1000f);

        list.add(new DebugEntry("Render FPS: " + renderFps + " | Cap: " + cap, 1f, 1f, 1f, false));
        list.add(new DebugEntry("Simulation UPS: " + simUps + " | Target: " + GameSettings.getTargetLogicFPS()
                + " Hz | Fixed dt: " + fixedDtMs + " ms", 1f, 1f, 1f, false));
        list.add(new DebugEntry("Ticks/frame: " + PerformanceMetrics.getTicksPerFrame(), 1f, 1f, 1f, false));
        list.add(new DebugEntry("Render cap controls: ` — increase, 1 — decrease", 1f, 1f, 1f, false));

        // Времена одного последнего фиксированного тика (а не всего кадра):
        // каждое поле перезаписывается внутри PlayState.update при каждом тике.
        long totalTime = cameraTime + backgroundTime + playerTime + bulletsTime + asteroidsTime
                + particlesTime + rocketsTime + orkTime + collisionsTime;
        list.add(new DebugEntry("Simulation tick: " + (totalTime / 1000) + " us", 1f, 1f, 1f, false));
        list.add(new DebugEntry("Collisions: " + (collisionsTime / 1000) + " us | Camera: " + (cameraTime / 1000) + " us", 1f, 1f, 1f, false));

        // === Игрок ===
        list.add(new DebugEntry("--- Player ---", 0f, 1f, 1f, true));

        float speed = (float) Math.sqrt(player.getDx() * player.getDx() + player.getDy() * player.getDy());
        list.add(new DebugEntry(String.format("Pos: x=%.1f y=%.1f", player.getX(), player.getY()), 1f, 1f, 1f, false));
        list.add(new DebugEntry(String.format("Vel: dx=%.1f dy=%.1f  speed=%.1f", player.getDx(), player.getDy(), speed), 1f, 1f, 1f, false));

        // Направление взгляда (как в Minecraft)
        float deg = (player.getRadians() * MathUtils.radiansToDegrees);
        float norm = ((deg % 360) + 360) % 360;
        String[] dirs = {"E", "NE", "N", "NW", "W", "SW", "S", "SE"};
        String facing = dirs[(((int) norm + 22) / 45) % 8];
        list.add(new DebugEntry(String.format("Facing: %.1f deg (%s)", norm, facing), 1f, 1f, 1f, false));
        list.add(new DebugEntry("Zoom: " + camera.getCurrentZoom() + "  (camera: " + (int) camera.getCameraX() + "," + (int) camera.getCameraY() + ")", 1f, 1f, 1f, false));

        // === Мир / объекты ===
        // Все показатели читаются из живых коллекций каждый кадр.
        // activeObjects — объекты, находящиеся сейчас в менеджере;
        // totalObjects — кумулятивное число созданных за сессию (не убывает).
        list.add(new DebugEntry("--- World / Objects ---", 0f, 1f, 0f, true));
        list.add(new DebugEntry("Active objects: " + gameObjectManager.getActiveObjects()
                + " / Created total: " + gameObjectManager.getTotalObjects(), 1f, 1f, 1f, false));
        list.add(new DebugEntry("Projectiles: " + gameObjectManager.getProjectileCount(), 1f, 1f, 1f, false));
        list.add(new DebugEntry("Obstacles: " + gameObjectManager.getObstacleCount(), 1f, 1f, 1f, false));
        list.add(new DebugEntry("Enemies: " + gameObjectManager.getEnemyCount(), 1f, 1f, 1f, false));
        list.add(new DebugEntry("Sectors: " + worldManager.getSectorCount(), 1f, 1f, 1f, false));
        list.add(new DebugEntry("Grid cells: " + worldManager.getGridCellCount(), 1f, 1f, 1f, false));

        // === Отсечение (Rendering / Culling) ===
        // active — живые объекты/частицы в коллекциях; visible — пересекающие расширенную
        // область камеры; drawn — реально отправленные в renderer в этом кадре;
        // culled — активные, но пропущенные из-за нахождения вне камеры.
        list.add(new DebugEntry("--- Rendering / Culling ---", 1f, 1f, 0.5f, true));
        list.add(new DebugEntry(cullingLine("Objects", renderStats.objectsActive, renderStats.objectsVisible,
                renderStats.objectsDrawn, renderStats.objectsCulled), 1f, 1f, 1f, false));
        list.add(new DebugEntry(cullingLine("Projectiles", renderStats.projectilesActive, renderStats.projectilesVisible,
                renderStats.projectilesDrawn, renderStats.projectilesCulled), 1f, 1f, 1f, false));
        list.add(new DebugEntry(cullingLine("Obstacles", renderStats.obstaclesActive, renderStats.obstaclesVisible,
                renderStats.obstaclesDrawn, renderStats.obstaclesCulled), 1f, 1f, 1f, false));
        list.add(new DebugEntry(cullingLine("Enemies", renderStats.enemiesActive, renderStats.enemiesVisible,
                renderStats.enemiesDrawn, renderStats.enemiesCulled), 1f, 1f, 1f, false));
        list.add(new DebugEntry(cullingLine("Particles", renderStats.particlesActive, renderStats.particlesVisible,
                renderStats.particlesDrawn, renderStats.particlesCulled), 1f, 1f, 1f, false));

        // === Прокачка ===
        list.add(new DebugEntry("--- Progression ---", 1f, 0.5f, 0f, true));
        list.add(new DebugEntry("Level: " + progressionManager.getCurrentLevel(), 1f, 1f, 1f, false));
        list.add(new DebugEntry("XP: " + progressionManager.getCurrentExperience() + "/" + progressionManager.getExperienceToNextLevel(), 1f, 1f, 1f, false));
        list.add(new DebugEntry("Score: " + score + "   High: " + highScore, 1f, 1f, 1f, false));

        // === Система ===
        list.add(new DebugEntry("--- System ---", 0f, 1f, 0.5f, true));
        Runtime rt = Runtime.getRuntime();
        long usedMem = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        long maxMem = rt.maxMemory() / 1024 / 1024;
        list.add(new DebugEntry("Mem: " + usedMem + " MB / " + maxMem + " MB", 1f, 1f, 1f, false));
        list.add(new DebugEntry("HUD: " + MyGdxGame.WIDTH + "x" + MyGdxGame.HEIGHT, 1f, 1f, 1f, false));
        list.add(new DebugEntry("GL: " + Gdx.graphics.getGLVersion().getDebugVersionString(), 1f, 1f, 1f, false));

        return list;
    }

    // Компактная строка категории: "Name: active A | visible V | drawn D | culled C"
    private String cullingLine(String name, int active, int visible, int drawn, int culled) {
        return name + ": active " + active + " | visible " + visible + " | drawn " + drawn + " | culled " + culled;
    }

    // Строка отладочной таблицы: текст, цвет и флаг заголовка секции
    private static class DebugEntry {
        final String text;
        final float r, g, b;
        final boolean header;

        DebugEntry(String text, float r, float g, float b, boolean header) {
            this.text = text;
            this.r = r;
            this.g = g;
            this.b = b;
            this.header = header;
        }
    }

    @Override
    public void dispose() {
        if (font != null) {
            font.dispose();
        }
        if (fontGenerator != null) {
            fontGenerator.dispose();
        }
        if (spriteBatch != null) {
            spriteBatch.dispose();
        }
        if (player != null) {
            player.dispose();
        }
    }
}