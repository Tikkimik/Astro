package com.gdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.gdx.managers.GameInputProcessor;
import com.gdx.managers.GameKeys;
import com.gdx.managers.GameStateManager;
import com.gdx.managers.AndroidInputManager;
import com.gdx.utils.TimeManager;
import com.gdx.utils.GameConfig;
import com.gdx.utils.DisplayManager;
import com.gdx.utils.GameLogger;
import com.gdx.utils.GameSettings;

public class MyGdxGame extends ApplicationAdapter {
	SpriteBatch batch;
	Texture img;

	public static int WIDTH ;
	public static int HEIGHT;

	public static OrthographicCamera camera;

	public GameStateManager gameStateManager;
	public AndroidInputManager androidInputManager;
	
	// Настройки FPS
	public static int targetFPS = 60;  // Целевой FPS
	public static final int[] FPS_PRESETS = {30, 60, 120, 240, 0}; // 0 = без ограничений
	public static int currentFPSIndex = 1; // Начинаем с 60 FPS
	
	// Фиксированный временной шаг для независимости от FPS (теперь управляется TimeManager)
	public static final float FIXED_TIMESTEP = TimeManager.FIXED_TIMESTEP;
	public static final float MAX_ACCUMULATOR = TimeManager.MAX_ACCUMULATOR;
	
	// Рекорд
	public static int highScore = 0;
	
	// Переменные для полноэкранного режима
	private static boolean isFullscreen = false;
	private static int windowedWidth = 1280;
	private static int windowedHeight = 720;

	/**
	 * метод инициализации
	 */
	@Override
	public void create () {  //метод инициализации по сути
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

		gameStateManager = new GameStateManager();
		
		// Вернули обычную игру для профилирования
		// gameStateManager.push(new com.gdx.gamestates.FPSTestState(gameStateManager));

		batch = new SpriteBatch();
		// img = new Texture("badlogic.jpg"); // Убираем логотип
	}

	/**
	 * тут происходить вся игра
	 * нужно все делить все на разные составляющие чтобы тут небыло хаоса
	 */
	// Аккумулятор времени теперь управляется TimeManager
	
	@Override
	public void render () {
		ScreenUtils.clear(0, 0, 0, 1); //black

		// Получаем deltaTime через TimeManager
		float deltaTime = TimeManager.getDeltaTime();
		
		// Обновляем аккумулятор времени
		TimeManager.updateAccumulator(deltaTime);
		
		// Обновляем игровую логику с фиксированным временным шагом
		while (TimeManager.shouldUpdate()) {
			gameStateManager.update(TimeManager.getFixedTimestep());
		}
		
		// Обработка ввода
		gameStateManager.handleInput();
		
		// Отрисовка происходит каждый кадр (не зависит от временного шага)
		gameStateManager.draw();
		TimeManager.incrementRenderCount();

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
		
		// Мониторинг FPS с настраиваемым интервалом
		if (Gdx.graphics.getFrameId() % GameConfig.STATS_INTERVAL == 0) {
			String stats = TimeManager.getStats();
			if (!stats.isEmpty()) {
				GameLogger.performance(stats);
			}
			TimeManager.resetStats();
		}

		// batch.begin();
		// batch.draw(img, 0, 0); // Убираем отрисовку логотипа
		// batch.end();
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
	 * заключительый метод
	 */
	@Override
	public void dispose () {
		batch.dispose();
		// img.dispose(); // Убираем освобождение ресурсов логотипа
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
