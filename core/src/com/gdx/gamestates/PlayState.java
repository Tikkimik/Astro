package com.gdx.gamestates;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.gdx.entities.Asteroid;
import com.gdx.entities.AutoRocket;
import com.gdx.entities.Bullet;
import com.gdx.entities.OrkAsteroid;
import com.gdx.entities.OrkBullet;
import com.gdx.entities.Particle;
import com.gdx.entities.Player;
import com.gdx.entities.EnemyShip;
import com.gdx.entities.ExplosionParticle;
import com.gdx.entities.RocketExplosionEffect;
import com.gdx.game.MyGdxGame;
import com.gdx.managers.Camera;
import com.gdx.managers.GameKeys;
import com.gdx.managers.AndroidInputManager;
import com.gdx.managers.WorldManager;
import com.gdx.managers.ProgressionManager;
import com.gdx.managers.GameObjectManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.gdx.combat.CombatRewardRules;
import com.gdx.combat.DestructionEvent;
import com.gdx.utils.ObjectPools;
import com.gdx.utils.PerformanceMetrics;
import com.gdx.utils.PerformanceMonitor;
import com.gdx.utils.TimeManager;
import com.gdx.utils.GameSettings;
import com.gdx.scenarios.ScenarioRunner;
import com.gdx.scenarios.ScenarioRuntime;
import com.gdx.session.GameSession;
import com.gdx.session.GameSessionSnapshot;
import com.gdx.session.GameSaveData;
import com.gdx.presentation.GameHudRenderer;
import com.gdx.presentation.GameHudView;
import com.gdx.presentation.GamePresentation;
import com.gdx.presentation.GameDebugOverlay;
import com.gdx.presentation.GameDebugView;
import com.gdx.presentation.GameWorldRenderer;
import com.gdx.simulation.GameSimulation;
import com.gdx.simulation.PlayerControls;

public class PlayState implements Disposable {

    private ShapeRenderer shapeRenderer;

    private GamePresentation presentation;
    private Player player;
    private GameObjectManager gameObjectManager;

    private GameSession session;

    private GameSimulation simulation;

    // FPS счетчик
    private boolean showFPS = false;
    private BitmapFont font;
    private FreeTypeFontGenerator fontGenerator;
    private SpriteBatch spriteBatch;

    private GameHudRenderer hudRenderer;
    private GameDebugOverlay debugOverlay;
    private GameWorldRenderer worldRenderer;

    // Профилирование производительности
    private long cameraTime = 0, backgroundTime = 0, playerTime = 0;
    private long objectsTime = 0, particlesTime = 0, collisionsTime = 0;

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
    private boolean restoredFromSave;

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
            gameObjectManager.addDestructionListener(event -> {
                if (event.kind() == DestructionEvent.Kind.ASTEROID_STAGE) {
                    createParticles(event.x(), event.y());
                    session.decrementAsteroidsLeft();
                } else {
                    createOptimizedExplosion(event.x(), event.y(), 8);
                }
            });
            gameObjectManager.addDestructionListener(event -> {
                int reward = CombatRewardRules.pointsFor(event);
                if (reward > 0) addScore(reward);
            });
            gameObjectManager.setRocketExplosionListener((x, y, radius) -> {
                presentation.rocketExplosionEffects().add(new RocketExplosionEffect(x, y, radius));
                createOptimizedExplosion(x, y, 12);
            });
            gameObjectManager.setPlayerObstacleCollisionListener(this::createShipExplosion);
        }

        // Создаем игрока только если он еще не создан (не восстановлен из состояния)
        if (player == null) {
            System.out.println("Создаем нового игрока в центре экрана");
            player = new Player();
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

        ScenarioRunner scenarioRunner = ScenarioRuntime.attach(this);
        simulation = new GameSimulation(session, scenarioRunner);
        simulation.setRestoredFromSave(restoredFromSave);

        createUiFont();
        spriteBatch = new SpriteBatch();
        hudRenderer = new GameHudRenderer(font);
        debugOverlay = new GameDebugOverlay(font);
        worldRenderer = new GameWorldRenderer(presentation, player, gameObjectManager);

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
            presentation.particles().add(ObjectPools.obtainParticle(x, y));
            com.gdx.utils.ParticleBudget.add(1);
        }
    }

    private void createExplosion(float x, float y, int particleCount) {
        if (!com.gdx.utils.ParticleBudget.isInSpawnBounds(x, y)) return;
        for(int i = 0; i < particleCount && com.gdx.utils.ParticleBudget.canSpawn(x, y); i++) {
            presentation.particles().add(ObjectPools.obtainExplosionParticle(x, y));
            com.gdx.utils.ParticleBudget.add(1);
        }
    }

    private void createShipExplosion(float x, float y) {
        // Создаем много частиц для эффектного взрыва корабля
        for(int i = 0; i < 15 && com.gdx.utils.ParticleBudget.canSpawn(x, y); i++) {
            presentation.particles().add(ObjectPools.obtainExplosionParticle(x, y));
            com.gdx.utils.ParticleBudget.add(1);
        }
        // Добавляем несколько обычных частиц
        for(int i = 0; i < 8 && com.gdx.utils.ParticleBudget.canSpawn(x, y); i++) {
            presentation.particles().add(ObjectPools.obtainParticle(x, y));
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

    public void update(float dt) {
//        System.out.println("PLAY STATE UPDATING");

        // Начинаем мониторинг производительности
        PerformanceMonitor.startFrame();

        // Защита от нулевого или отрицательного deltaTime
        if (dt <= 0) {
            System.out.println("WARNING: Invalid deltaTime: " + dt + ", using 0.016f (60 FPS)");
            dt = 0.016f; // Принудительно устанавливаем 60 FPS
        }

        boolean logicScenario = ScenarioRuntime.isActive() && ScenarioRuntime.isLogicMode();
        if (!logicScenario) {
            GamePresentation.ViewTimings presentationTimings = presentation.updateView(dt, player);
            cameraTime = presentationTimings.cameraNanos();
            backgroundTime = presentationTimings.backgroundNanos();
        }

        GameSimulation.BeginTickResult simulationResult = simulation.beginTick(dt,
                new PlayerControls(inputLeft, inputRight, inputUp, inputFire),
                ScenarioRuntime.isActive());
        playerTime = simulationResult.playerNanos();
        objectsTime = simulationResult.objectsNanos();
        if (simulationResult.playerReset()) {
            System.out.println("Player умер, сбрасываем...");
            return;
        }

        if (!logicScenario) {
            particlesTime = presentation.updateEffects(dt);
            hudRenderer.update(dt, createHudView());
        }
        collisionsTime = simulation.finishTick(dt, ScenarioRuntime.isActive());

        // Завершаем мониторинг производительности
        PerformanceMonitor.endFrame();
    }

    public void draw() {
        if (ScenarioRuntime.isActive() && ScenarioRuntime.isLogicMode()) {
            throw new IllegalStateException("Logic scenario must not enter presentation render");
        }
        worldRenderer.draw(shapeRenderer, spriteBatch);

        hudRenderer.draw(spriteBatch, shapeRenderer, createHudView());

        // Детальная F3-табличка с полезной информацией (клавиша F)
        // Начинаем ниже HUD-блока с безопасным отступом
        if (showFPS) {
            debugOverlay.draw(spriteBatch, createDebugView(), hudRenderer.getBottomY() - 16f);
        }

        // Рендерим Android элементы управления поверх всего
        if (androidInputManager != null && androidInputManager.isAndroid()) {
            androidInputManager.render(shapeRenderer);
        }
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

    public void setDebugOverlayVisible(boolean visible) {
        showFPS = visible;
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

            restoredFromSave = true;
            if (simulation != null) simulation.setRestoredFromSave(true);
        }
    }

    public GameSaveData createPersistentSave() {
        return session.createSaveData();
    }

    public void restorePersistentSave(GameSaveData data) {
        session.restore(data);
        MyGdxGame.highScore = session.getHighScore();
        restoredFromSave = false;
        if (simulation != null) simulation.setRestoredFromSave(false);
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

    private GameDebugView createDebugView() {
        long totalTickNanos = cameraTime + backgroundTime + playerTime + objectsTime
                + particlesTime + collisionsTime;
        Camera camera = presentation.camera();
        var renderStats = worldRenderer.stats();
        Runtime runtime = Runtime.getRuntime();
        return new GameDebugView(
                new GameDebugView.Performance(
                        Math.round(PerformanceMetrics.getRenderFps()),
                        Math.round(PerformanceMetrics.getSimulationUps()),
                        PerformanceMetrics.getTicksPerFrame(),
                        MyGdxGame.targetFPS == 0 ? "Unlimited" : String.valueOf(MyGdxGame.targetFPS),
                        GameSettings.getTargetLogicFPS(), TimeManager.getFixedStep() * 1000f,
                        totalTickNanos, collisionsTime, cameraTime),
                new GameDebugView.PlayerState(player.getX(), player.getY(), player.getDx(),
                        player.getDy(), player.getRadians()),
                new GameDebugView.CameraState(camera.getCurrentZoom(), camera.getCameraX(), camera.getCameraY()),
                new GameDebugView.WorldState(
                        gameObjectManager.getActiveObjects(), gameObjectManager.getTotalObjects(),
                        gameObjectManager.getProjectileCount(), gameObjectManager.getObstacleCount(),
                        gameObjectManager.getEnemyCount(), worldManager.getSectorCount(),
                        worldManager.getGridCellCount()),
                new GameDebugView.CullingState(
                        culling(renderStats.objectsActive, renderStats.objectsVisible,
                                renderStats.objectsDrawn, renderStats.objectsCulled),
                        culling(renderStats.projectilesActive, renderStats.projectilesVisible,
                                renderStats.projectilesDrawn, renderStats.projectilesCulled),
                        culling(renderStats.obstaclesActive, renderStats.obstaclesVisible,
                                renderStats.obstaclesDrawn, renderStats.obstaclesCulled),
                        culling(renderStats.enemiesActive, renderStats.enemiesVisible,
                                renderStats.enemiesDrawn, renderStats.enemiesCulled),
                        culling(renderStats.particlesActive, renderStats.particlesVisible,
                                renderStats.particlesDrawn, renderStats.particlesCulled)),
                new GameDebugView.ProgressionState(
                        progressionManager.getCurrentLevel(), progressionManager.getCurrentExperience(),
                        progressionManager.getExperienceToNextLevel(), session.getScore(), session.getHighScore()),
                new GameDebugView.SystemState(
                        (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024,
                        runtime.maxMemory() / 1024 / 1024,
                        MyGdxGame.WIDTH, MyGdxGame.HEIGHT,
                        Gdx.graphics.getGLVersion().getDebugVersionString()));
    }

    private GameDebugView.Culling culling(int active, int visible, int drawn, int culled) {
        return new GameDebugView.Culling(active, visible, drawn, culled);
    }

    @Override
    public void dispose() {
        if (presentation != null) presentation.dispose();
        if (gameObjectManager != null) {
            gameObjectManager.dispose();
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
