package com.gdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.gdx.managers.GameInputProcessor;
import com.gdx.managers.GameKeys;
import com.gdx.managers.ImprovedGameStateManager;
import com.gdx.managers.AndroidInputManager;
import com.gdx.utils.TimeManager;
import com.gdx.utils.GameConfig;
import com.gdx.utils.DisplayManager;
import com.gdx.utils.GameLogger;
import com.gdx.utils.GameSettings;
import com.gdx.utils.ParticlePool;
import com.gdx.utils.PerformanceMetrics;
import com.gdx.managers.MenuStateHandler;
import com.gdx.managers.RealPlayStateHandler;
import com.gdx.managers.PausedStateHandler;
import com.gdx.managers.SettingsStateHandler;
import com.gdx.managers.UpgradeSelectionStateHandler;

public class MyGdxGame extends ApplicationAdapter {
	SpriteBatch batch;
	Texture img;

	public static int WIDTH ;
	public static int HEIGHT;

	public static OrthographicCamera camera;

	public ImprovedGameStateManager gameStateManager;
	public AndroidInputManager androidInputManager;
	
	// Настройки FPS
	public static int targetFPS = 60;  // Целевой FPS
	public static final int[] FPS_PRESETS = {30, 60, 120, 240, 0}; // 0 = без ограничений
	public static int currentFPSIndex = 1; // Начинаем с 60 FPS
	
	// Рекорд
	public static int highScore = 0;
	
	// Режим отладки для оптимизации логирования
	public static final boolean DEBUG_MODE = GameSettings.isDebugMode();
	
	// Переменные для полноэкранного режима
	private static boolean isFullscreen = false;
	private static int windowedWidth = 1280;
	private static int windowedHeight = 720;

	/**
	 * метод инициализации
	 */
	@Override
	public void create () {  //метод инициализации по сути
		// Принудительно отключаем V-Sync для максимальной производительности
		Gdx.graphics.setVSync(false);
		
		// Включаем встроенные инструменты профилирования LibGDX
		Gdx.app.setLogLevel(com.badlogic.gdx.Application.LOG_DEBUG);
		
		// Инициализируем GameLogger с настройками из GameSettings
		GameLogger.setLogLevel(GameSettings.getLogLevel());
		GameLogger.setDebugMode(GameSettings.isDebugMode());
		
		// Диагностика системы
		GameLogger.info("=== SYSTEM DIAGNOSTICS ===");
		GameLogger.info("Java version: " + System.getProperty("java.version"));
		GameLogger.info("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
		GameLogger.info("OpenGL: " + Gdx.gl.glGetString(com.badlogic.gdx.graphics.GL20.GL_VERSION));
		GameLogger.info("GPU: " + Gdx.gl.glGetString(com.badlogic.gdx.graphics.GL20.GL_RENDERER));
		GameLogger.info("Display refresh rate: " + Gdx.graphics.getDisplayMode().refreshRate + "Hz");
		GameLogger.info("VSync: DISABLED (forced)");
		GameLogger.info("Log Level: " + GameLogger.getCurrentLogLevelName());
		GameLogger.info("Debug Mode: " + (GameSettings.isDebugMode() ? "ON" : "OFF"));
		GameLogger.info("==========================");
		
		WIDTH = Gdx.graphics.getWidth();
		HEIGHT = Gdx.graphics.getHeight();

		camera = new OrthographicCamera(WIDTH, HEIGHT);
		camera.translate(WIDTH / 2, HEIGHT / 2);
		camera.update();

		// Инициализируем Android управление
		androidInputManager = new AndroidInputManager();
		
		// Устанавливаем обработчик ввода (Android или Desktop)
		if (androidInputManager.isAndroid()) {
			Gdx.input.setInputProcessor(androidInputManager);
		} else {
			Gdx.input.setInputProcessor(new GameInputProcessor());
		}

		gameStateManager = new ImprovedGameStateManager();
		
		// Инициализируем новую систему состояний
		initializeStateSystem();
		
		// Вернули обычную игру для профилирования
		// gameStateManager.push(new com.gdx.gamestates.FPSTestState(gameStateManager));

		batch = new SpriteBatch();
		// img = new Texture("badlogic.jpg"); // Убираем логотип
	}

	/**
	 * тут происходить вся игра
	 * нужно все делить все на разные составляющие чтобы тут небыло хаоса
	 */	
	@Override
	public void render () {
		ScreenUtils.clear(0, 0, 0, 1); //black

		// Получаем deltaTime через TimeManager для независимости от FPS
		float deltaTime = TimeManager.getDeltaTime();
		
		// Обновляем аккумулятор времени
		TimeManager.updateAccumulator(deltaTime);
		
		// Обновляем игровую логику с фиксированным временным шагом
		int ticksThisFrame = 0;
		while (TimeManager.shouldUpdate()) {
			gameStateManager.update(TimeManager.getFixedTimestep());
			ticksThisFrame++;
		}
		
		// Обработка ввода
		gameStateManager.handleInput();
		
		// Отрисовка происходит каждый кадр (не зависит от временного шага)
		gameStateManager.render();
		TimeManager.incrementRenderCount();

		// Честные метрики: реально отрисованный кадр и реально выполненные тики
		// игровой логики (только в состоянии PLAYING — во время паузы логика не идёт).
		PerformanceMetrics.onRenderFrame(gameStateManager.isPlaying() ? ticksThisFrame : 0);

		// Обновляем Android управление
		if (androidInputManager != null) {
			androidInputManager.update();
		}
		
		GameKeys.update();
		
		// Обработка переключения полноэкранного режима
		if (GameKeys.isDown(GameKeys.TOGGLE_FULLSCREEN)) {
			GameLogger.input("F11 pressed - toggling fullscreen");
			toggleFullscreen();
			// Сбрасываем состояние клавиши, чтобы избежать повторного срабатывания
			GameKeys.setKey(GameKeys.TOGGLE_FULLSCREEN, false);
		}
		
		// Мониторинг FPS и фреймтайма с детальной статистикой
		if (Gdx.graphics.getFrameId() % 120 == 0) { // Каждые 2 секунды
			float fps = 1.0f / deltaTime;
			float frameTime = deltaTime * 1000; // В миллисекундах
			if (fps > 0 && fps < 300) {
				String stats = TimeManager.getStats();
				if (!stats.isEmpty()) {
					GameLogger.performance(String.format("RENDER FPS: %.1f | Render Frame Time: %.2fms | %s", fps, frameTime, stats));
				} else {
					GameLogger.performance(String.format("RENDER FPS: %.1f | Render Frame Time: %.2fms", fps, frameTime));
				}
			}
		}
	}

	@Override
	public void resize (int width, int height) {
		WIDTH = width;
		HEIGHT = height;
		
		// Обновляем камеру при изменении размера окна
		if (camera != null) {
			camera.viewportWidth = width;
			camera.viewportHeight = height;
			camera.update();
		}
		
		// Обновляем Android управление при изменении размера экрана
		if (androidInputManager != null) {
			androidInputManager.resize(width, height);
		}
	}

	@Override
	public void pause() {

	}

	@Override
	public void resume() {

	}

	/**
	 * Инициализация системы состояний
	 */
	private void initializeStateSystem() {
		GameLogger.info("=== ИНИЦИАЛИЗАЦИЯ НОВОЙ СИСТЕМЫ СОСТОЯНИЙ ===");
		
		// Регистрируем обработчики для всех состояний
		MenuStateHandler menuHandler = new MenuStateHandler();
		menuHandler.setStateManager(gameStateManager);
		gameStateManager.registerStateHandler(ImprovedGameStateManager.GameState.MENU, menuHandler);
		
		RealPlayStateHandler playHandler = new RealPlayStateHandler();
		playHandler.setStateManager(gameStateManager);
		gameStateManager.registerStateHandler(ImprovedGameStateManager.GameState.PLAYING, playHandler);
		
		PausedStateHandler pauseHandler = new PausedStateHandler();
		pauseHandler.setStateManager(gameStateManager);
		gameStateManager.registerStateHandler(ImprovedGameStateManager.GameState.PAUSED, pauseHandler);
		
		SettingsStateHandler settingsHandler = new SettingsStateHandler();
		settingsHandler.setStateManager(gameStateManager);
		gameStateManager.registerStateHandler(ImprovedGameStateManager.GameState.SETTINGS, settingsHandler);
		gameStateManager.registerStateHandler(ImprovedGameStateManager.GameState.UPGRADE_SELECTION, new UpgradeSelectionStateHandler());
		
		// Добавляем слушателя для логирования изменений состояния
		gameStateManager.addStateChangeListener((from, to) -> {
			String fromDesc = from != null ? from.getDescription() : "null";
			String toDesc = to != null ? to.getDescription() : "null";
			GameLogger.info("🔄 ПЕРЕХОД: " + fromDesc + " → " + toDesc);
		});
		
		// Устанавливаем начальное состояние - меню
		gameStateManager.setState(ImprovedGameStateManager.GameState.MENU);
		
		// Принудительно вызываем onEnter для начального состояния
		GameLogger.info("Принудительно вызываем onEnter для начального состояния...");
		
		GameLogger.info("Система состояний инициализирована");
	}
	
	/**
	 * заключительый метод
	 */
	@Override
	public void dispose () {
		batch.dispose();
		// img.dispose(); // Убираем освобождение ресурсов логотипа
		
		// Очищаем пулы частиц
		ParticlePool.clearAllPools();
		
		// Очищаем систему состояний
		if (gameStateManager != null) {
			gameStateManager.dispose();
		}
	}
	
	// Методы для управления FPS
	public static void increaseFPS() {
		if (currentFPSIndex < FPS_PRESETS.length - 1) {
			currentFPSIndex++;
			targetFPS = FPS_PRESETS[currentFPSIndex];
			updateFPSSettings();
			System.out.println("FPS limit set to: " + (targetFPS == 0 ? "UNLIMITED" : targetFPS));
		}
	}
	
	public static void decreaseFPS() {
		if (currentFPSIndex > 0) {
			currentFPSIndex--;
			targetFPS = FPS_PRESETS[currentFPSIndex];
			updateFPSSettings();
			System.out.println("FPS limit set to: " + (targetFPS == 0 ? "UNLIMITED" : targetFPS));
		}
	}
	
	private static void updateFPSSettings() {
		if (Gdx.app != null) {
			if (targetFPS == 0) {
				Gdx.graphics.setForegroundFPS(0); // Без ограничений
			} else {
				Gdx.graphics.setForegroundFPS(targetFPS);
			}
		}
	}
	
	// Метод для переключения полноэкранного режима
	private static void toggleFullscreen() {
		DisplayManager.toggleFullscreen();
	}
}
