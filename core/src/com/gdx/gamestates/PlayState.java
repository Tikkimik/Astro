package com.gdx.gamestates;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.gdx.entities.Asteroid;
import com.gdx.entities.AutoRocket;
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
import com.gdx.entities.RocketExplosionEffect;
import com.gdx.entities.ShieldParticle;
import com.gdx.entities.SparkParticle;
import com.gdx.game.MyGdxGame;
import com.gdx.managers.Camera;
import com.gdx.managers.GameKeys;
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
import com.badlogic.gdx.utils.Disposable;
import java.util.ArrayList;
import java.util.List;
import com.gdx.utils.ObjectPools;
import com.gdx.utils.PerformanceMetrics;
import com.gdx.utils.PerformanceMonitor;
import com.gdx.utils.TimeManager;
import com.gdx.utils.GameSettings;
import com.gdx.utils.RenderStats;
import com.gdx.scenarios.ScenarioRunner;
import com.gdx.scenarios.ScenarioRuntime;
import com.gdx.session.GameSession;
import com.gdx.session.GameSessionSnapshot;
import com.gdx.presentation.GameHudRenderer;
import com.gdx.presentation.GameHudView;
import com.gdx.presentation.GamePresentation;

public class PlayState implements Disposable {

    private ShapeRenderer shapeRenderer;

    private GamePresentation presentation;
    private Player player;
    private GameObjectManager gameObjectManager;

    private GameSession session;

    // Счётчики рендера (culling) и копия границ камеры для ParticleBudget
    private final RenderStats renderStats = new RenderStats();

    // Таймер для автоматических ракет
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

    private GameHudRenderer hudRenderer;

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

    // Снимок непрерывного управления фиксируется один раз на render frame и
    // одинаково применяется ко всем fixed ticks, выполненным в этом кадре.
    private boolean inputLeft;
    private boolean inputRight;
    private boolean inputUp;
    private boolean inputFire;

    // Управление бесконечным миром
    private WorldManager worldManager;

    // Система прокачки
    private ProgressionManager progressionManager;

    // Флаг для предотвращения повторной инициализации
    private boolean isInitialized = false;
    private boolean isRestoredFromSave = false;
    private boolean autoRocketLeftSide = true; // Флаг для чередования сторон авторакет
    private ScenarioRunner scenarioRunner;

    public PlayState() {
    }

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
        presentation = new GamePresentation();

        // Инициализируем GameObjectManager для управления всеми объектами
        if (gameObjectManager == null) {
            gameObjectManager = new GameObjectManager();
            gameObjectManager.setAsteroidDestroyedListener((asteroid, x, y) -> {
                createParticles(x, y);
                session.decrementAsteroidsLeft();
                if (asteroid.getType() == Asteroid.SMALL) addScore(5);
            });
            gameObjectManager.setEnemyDestroyedListener((enemy, x, y) -> {
                createOptimizedExplosion(x, y, 8);
                addScore(enemy instanceof OrkAsteroid ? 10 : 25);
            });
            gameObjectManager.setRocketExplosionListener((x, y, radius) -> {
                presentation.rocketExplosionEffects().add(new RocketExplosionEffect(x, y, radius));
                createOptimizedExplosion(x, y, 12);
            });
        }

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
        }

        session = new GameSession(player, gameObjectManager, worldManager,
                progressionManager, MyGdxGame.highScore);

        scenarioRunner = ScenarioRuntime.attach(this);

        // Инициализируем FPS счетчик
        fpsLogger = new FPSLogger();
        createUiFont();
        spriteBatch = new SpriteBatch();
        hudRenderer = new GameHudRenderer(font);

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
            presentation.particles().add(new Particle(x, y));
            com.gdx.utils.ParticleBudget.add(1);
        }
    }

    private void createExplosion(float x, float y, int particleCount) {
        if (!com.gdx.utils.ParticleBudget.isInSpawnBounds(x, y)) return;
        for(int i = 0; i < particleCount && com.gdx.utils.ParticleBudget.canSpawn(x, y); i++) {
            presentation.particles().add(new ExplosionParticle(x, y));
            com.gdx.utils.ParticleBudget.add(1);
        }
    }

    private void createShipExplosion(float x, float y) {
        // Создаем много частиц для эффектного взрыва корабля
        for(int i = 0; i < 15 && com.gdx.utils.ParticleBudget.canSpawn(x, y); i++) {
            presentation.particles().add(new ExplosionParticle(x, y));
            com.gdx.utils.ParticleBudget.add(1);
        }
        // Добавляем несколько обычных частиц
        for(int i = 0; i < 8 && com.gdx.utils.ParticleBudget.canSpawn(x, y); i++) {
            presentation.particles().add(new Particle(x, y));
            com.gdx.utils.ParticleBudget.add(1);
        }
    }

    // Методы для создания оптимизированных частиц
    private void createOptimizedExplosion(float x, float y, int particleCount) {
        if (!com.gdx.utils.ParticleBudget.isInSpawnBounds(x, y)) return;
        for(int i = 0; i < particleCount && com.gdx.utils.ParticleBudget.canSpawn(x, y); i++) {
            presentation.optimizedParticles().add(ObjectPools.obtainOptimizedParticle(x, y, "explosion"));
            com.gdx.utils.ParticleBudget.add(1);
        }
    }

    private void createOptimizedFlameParticles(float x, float y, int particleCount) {
        if (!com.gdx.utils.ParticleBudget.isInSpawnBounds(x, y)) return;
        for(int i = 0; i < particleCount && com.gdx.utils.ParticleBudget.canSpawn(x, y); i++) {
            presentation.optimizedParticles().add(ObjectPools.obtainOptimizedParticle(x, y, "flame"));
            com.gdx.utils.ParticleBudget.add(1);
        }
    }

    private void createOptimizedSmokeParticles(float x, float y, int particleCount) {
        if (!com.gdx.utils.ParticleBudget.isInSpawnBounds(x, y)) return;
        for(int i = 0; i < particleCount && com.gdx.utils.ParticleBudget.canSpawn(x, y); i++) {
            presentation.optimizedParticles().add(ObjectPools.obtainOptimizedParticle(x, y, "smoke"));
            com.gdx.utils.ParticleBudget.add(1);
        }
    }

    private void addScore(int points) {
        session.addScore(points);
        MyGdxGame.highScore = session.getHighScore();
    }

    // Найти ближайшего врага к игроку (оптимизированная версия)
    private Enemy findNearestEnemy() {
        return gameObjectManager.findNearestEnemy(
                player.getX(), player.getY(), AutoRocket.DETECTION_RANGE);
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

        int numToSpawn = 4 + session.getLevel() - 1;

        session.setTotalAsteroids(numToSpawn * 7);
        session.setNumAsteroidsLeft(session.getTotalAsteroids());

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
        int numToSpawn = Math.min(1 + session.getLevel() / 3, 3);

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

    public void update(float dt) {
//        System.out.println("PLAY STATE UPDATING");

        // Начинаем мониторинг производительности
        PerformanceMonitor.startFrame();

        // Защита от нулевого или отрицательного deltaTime
        if (dt <= 0) {
            System.out.println("WARNING: Invalid deltaTime: " + dt + ", using 0.016f (60 FPS)");
            dt = 0.016f; // Принудительно устанавливаем 60 FPS
        }

        session.updateTime(dt);

        // Применяем улучшения к игровым объектам (если есть новые)
        // progressionManager.applyUpgradesToGameObjects(player, autoRockets, bullets);

        // Начинаем профилирование
        long startTime = System.nanoTime();

        GamePresentation.ViewTimings presentationTimings = presentation.updateView(dt, player);
        cameraTime = presentationTimings.cameraNanos();
        backgroundTime = presentationTimings.backgroundNanos();

        applyPlayerInput();

        // Обновляем бесконечный мир
        if (!ScenarioRuntime.isActive()) {
            worldManager.update(dt, isRestoredFromSave);
        }

        //next level (теперь не нужен, так как мир бесконечный)
        // if(asteroids.size == 0 && orkAsteroids.size == 0) {
        //     level++;
        //     spawnAsteroids();
        //     spawnOrkAsteroids();
        // }

        //update player
        long playerStart = System.nanoTime();
        if (scenarioRunner != null) scenarioRunner.beforeUpdate(dt);
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

        particlesTime = presentation.updateEffects(dt);

                //update auto rockets
        long rocketsStart = System.nanoTime();
        // GameObjectManager уже обновил все объекты
        rocketsTime = System.nanoTime() - rocketsStart;

        //update ork asteroids and bullets
        long orkStart = System.nanoTime();
        // GameObjectManager уже обновил все объекты

        hudRenderer.update(dt, createHudView());

        // GameObjectManager уже обновил все объекты
        orkTime = System.nanoTime() - orkStart;

        // Обновляем кэш ближайшего врага
        enemyCacheTimer += dt;
        if (enemyCacheTimer >= ENEMY_CACHE_DURATION) {
            cachedNearestEnemy = findNearestEnemy();
            enemyCacheTimer = 0f;
        }

        //create auto rocket every second
        if (!ScenarioRuntime.isActive()) {
            session.addAutoRocketTime(dt);
            if(session.getAutoRocketTimer() >= AUTO_ROCKET_INTERVAL) {
                createAutoRocket();
                session.resetAutoRocketTimer();
            }
        }

        //check collisions
        long collisionsStart = System.nanoTime();
        checkCollisions();
        gameObjectManager.checkCollisions();
        if (scenarioRunner != null) scenarioRunner.afterUpdate(dt);
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
                             " P=" + presentation.particles().size);
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

    public void draw() {
//        System.out.println("PLAY STATE DRAWING");
        long drawStart = System.nanoTime();
        Camera camera = presentation.camera();

        // Сброс счётчиков рендера на начало кадра
        renderStats.beginFrame();

        // Отрисовка заполненных объектов (фон)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

//draw background first
        presentation.background().draw(shapeRenderer, camera);

        // Отрисовка всех игровых объектов через GameObjectManager (с culling:
        // снаряды, препятствия, враги и следы ракет за камерой в renderer не идут)
        gameObjectManager.draw(shapeRenderer, camera, spriteBatch, renderStats);

        for (RocketExplosionEffect effect : presentation.rocketExplosionEffects()) {
            effect.drawFlash(shapeRenderer, camera);
        }

        shapeRenderer.end();

        // Отрисовка линий (корабль). Игрок рисуется здесь один раз за кадр —
        // раньше он попадал в рендер и из allObjects менеджера, и этим вызовом,
        // из-за чего корабль, пламя и щит рисовались дважды.
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        float playerRadius = Math.max(1f, player.getWidth() / 2f);
        int flameCount = player.getFlameParticles().size;
        int shieldCount = player.getShieldParticles().size;
        int sparkCount = player.getSparkParticles().size;
        int exhaustTrailCount = player.getExhaustTrailSampleCount();
        boolean playerInView = camera.isInRenderBounds(player.getX(), player.getY(), playerRadius);
        renderStats.particlesActive += flameCount + shieldCount + sparkCount + exhaustTrailCount;
        if (playerInView) {
            player.draw(shapeRenderer, camera, spriteBatch);
            // Внутренние образцы непрерывного шлейфа рисуются Player одним
            // проходом; учитываем их в общей статистике частиц.
            renderStats.particlesVisible += exhaustTrailCount;
            renderStats.particlesDrawn += exhaustTrailCount;
        }
        // Пламя, щит и искры игрока считаем поштучно (с culling): внеэкранные
        // частицы не должны расходовать видимый бюджет и забирать слоты у эффектов
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
            for (SparkParticle sp : player.getSparkParticles()) {
                if (camera.isInRenderBounds(sp.getX(), sp.getY(), sp.getCullRadius())) {
                    renderStats.particlesVisible++;
                    renderStats.particlesDrawn++;
                } else {
                    renderStats.particlesCulled++;
                }
            }
        } else {
            renderStats.particlesCulled += flameCount + shieldCount + sparkCount + exhaustTrailCount;
        }

        for (RocketExplosionEffect effect : presentation.rocketExplosionEffects()) {
            effect.drawRing(shapeRenderer, camera);
        }

        shapeRenderer.end();

        // Отрисовка заполненных объектов (частицы взрывов) — с culling
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        //draw particles
        for(int i = 0; i < presentation.particles().size; i++) {
            Particle p = presentation.particles().get(i);
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
        if (presentation.optimizedParticles().size > 0) {
            spriteBatch.begin();
            //draw optimized particles
            for(int i = 0; i < presentation.optimizedParticles().size; i++) {
                OptimizedParticle p = presentation.optimizedParticles().get(i);
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

        // Следы ракет — мягкие аддитивные текстуры через SpriteBatch
        gameObjectManager.drawRocketParticles(spriteBatch, camera, renderStats);

        // Дым вражеских кораблей — мягкая текстура, обычное смешивание
        gameObjectManager.drawEnemySmoke(spriteBatch, camera, renderStats);

        // Итоговая категория «Objects»: все мировые объекты, включая игрока
        renderStats.objectsActive = gameObjectManager.getActiveObjects();
        renderStats.objectsVisible = renderStats.projectilesVisible + renderStats.obstaclesVisible
                + renderStats.enemiesVisible + (playerInView ? 1 : 0);
        renderStats.objectsDrawn = renderStats.projectilesDrawn + renderStats.obstaclesDrawn
                + renderStats.enemiesDrawn + (playerInView ? 1 : 0);
        renderStats.objectsCulled = renderStats.objectsActive - renderStats.objectsVisible;

        // Видимый счётчик частиц для ParticleBudget — по итогам реального render-кадра
        com.gdx.utils.ParticleBudget.setVisible(renderStats.particlesVisible);

        hudRenderer.draw(spriteBatch, shapeRenderer, createHudView());

        // Детальная F3-табличка с полезной информацией (клавиша F)
        // Начинаем ниже HUD-блока с безопасным отступом
        if (showFPS) {
            drawDebugTable(hudRenderer.getBottomY() - 16f);
        }


        // Рендерим Android элементы управления поверх всего
        if (androidInputManager != null && androidInputManager.isAndroid()) {
            androidInputManager.render(shapeRenderer);
        }

        // Записываем время отрисовки
        drawTime = System.nanoTime() - drawStart;
    }

    public void handleInput() {
        if (ScenarioRuntime.isActive()) {
            presentation.camera().setTargetZoom(2f);
        }

        // Отладка клавиш
        if (GameKeys.isPressed(GameKeys.ESCAPE)) {
            System.out.println("ESCAPE нажата!");
        }

        inputLeft = GameKeys.isDown(GameKeys.LEFT);
        inputRight = GameKeys.isDown(GameKeys.RIGHT);
        inputUp = GameKeys.isDown(GameKeys.UP);
        inputFire = GameKeys.isDown(GameKeys.SPACE);

        // Управление зумом - используем isDown для непрерывного действия
        if (GameKeys.isDown(GameKeys.ZOOM_IN)) {
            presentation.camera().zoomIn();
        }
        if (GameKeys.isDown(GameKeys.ZOOM_OUT)) {
            presentation.camera().zoomOut();
        }
        if (GameKeys.isPressed(GameKeys.ENTER)) {
            presentation.camera().resetZoom();
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

        // Тестовая кнопка для получения опыта (для тестирования)
        if (GameKeys.isPressed(GameKeys.TEST)) {
            progressionManager.addExperience(50);
        }
    }

    private void applyPlayerInput() {
        player.setLeft(inputLeft);
        player.setRight(inputRight);
        player.setUp(inputUp);
        if (inputFire) {
            player.shoot();
        }
    }

    // Геттеры для применения улучшений
    public Player getPlayer() {
        return session.getPlayer();
    }

    public GameObjectManager getGameObjectManager() {
        return session.getObjects();
    }

    public ProgressionManager getProgressionManager() {
        return session.getProgression();
    }

    public int getScore() {
        return session.getScore();
    }

    public GameSession getSession() {
        return session;
    }

    public Array<AutoRocket> getAutoRockets() {
        // Возвращаем пустой массив, так как теперь используем GameObjectManager
        return new Array<AutoRocket>();
    }

    // Методы для сохранения и восстановления состояния
    public GameSessionSnapshot saveGameState() {
        System.out.println("=== СОХРАНЕНИЕ СОСТОЯНИЯ ===");
        System.out.println("До сохранения - Очки: " + session.getScore() + ", Объекты: " + gameObjectManager.getStats());
        if (player != null) {
            System.out.println("Player позиция: (" + player.getX() + ", " + player.getY() + ")");
        }

        GameSessionSnapshot snapshot = session.createSnapshot();
        System.out.println("Состояние создано - Очки: " + snapshot.score() + ", Объекты: " + gameObjectManager.getStats());
        return snapshot;
    }

    public void restoreGameState(GameSessionSnapshot snapshot) {
        if (snapshot != null) {
            System.out.println("=== ВОССТАНОВЛЕНИЕ СОСТОЯНИЯ ===");
            session.restore(snapshot);
            MyGdxGame.highScore = session.getHighScore();

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

    private GameHudView createHudView() {
        return new GameHudView(
                session.getScore(), session.getHighScore(),
                progressionManager.getCurrentLevel(), progressionManager.getCurrentExperience(),
                progressionManager.getExperienceToNextLevel(), progressionManager.getExperienceProgress(),
                MyGdxGame.WIDTH, MyGdxGame.HEIGHT);
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
        Camera camera = presentation.camera();
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
        list.add(new DebugEntry("Score: " + session.getScore() + "   High: " + session.getHighScore(), 1f, 1f, 1f, false));

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
        if (presentation != null) presentation.dispose();
        if (gameObjectManager != null) {
            gameObjectManager.clear();
        }
        if (font != null) {
            font.dispose();
            font = null;
        }
        if (fontGenerator != null) {
            fontGenerator.dispose();
            fontGenerator = null;
        }
        if (spriteBatch != null) {
            spriteBatch.dispose();
            spriteBatch = null;
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
            shapeRenderer = null;
        }
        if (player != null) {
            player.dispose();
            player = null;
        }
        isInitialized = false;
    }
}
