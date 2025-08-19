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

public class MyGdxGame extends ApplicationAdapter {
	SpriteBatch batch;
	Texture img;

	public static int WIDTH ;
	public static int HEIGHT;

	public static OrthographicCamera camera;

	public GameStateManager gameStateManager;
	
	// Настройки FPS
	public static int targetFPS = 60;  // Целевой FPS
	public static final int[] FPS_PRESETS = {30, 60, 120, 240, 0}; // 0 = без ограничений
	public static int currentFPSIndex = 1; // Начинаем с 60 FPS

	/**
	 * метод инициализации
	 */
	@Override
	public void create () {  //метод инициализации по сути
		// Диагностика системы
		System.out.println("=== SYSTEM DIAGNOSTICS ===");
		System.out.println("Java version: " + System.getProperty("java.version"));
		System.out.println("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
		System.out.println("OpenGL: " + Gdx.gl.glGetString(com.badlogic.gdx.graphics.GL20.GL_VERSION));
		System.out.println("GPU: " + Gdx.gl.glGetString(com.badlogic.gdx.graphics.GL20.GL_RENDERER));
		System.out.println("Display refresh rate: " + Gdx.graphics.getDisplayMode().refreshRate + "Hz");
		System.out.println("==========================");
		
		WIDTH = Gdx.graphics.getWidth();
		HEIGHT = Gdx.graphics.getHeight();

		camera = new OrthographicCamera(WIDTH, HEIGHT);
		camera.translate(WIDTH / 2, HEIGHT / 2);
		camera.update();

		Gdx.input.setInputProcessor(new GameInputProcessor());

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
	@Override
	public void render () {
		ScreenUtils.clear(0, 0, 0, 1); //black

		float deltaTime = Gdx.graphics.getDeltaTime();
		
		// Защита от нулевого или отрицательного deltaTime
		if (deltaTime <= 0) {
			System.out.println("WARNING: Invalid deltaTime in render: " + deltaTime + ", using 0.016f");
			deltaTime = 0.016f;
		}
		
		// Ограничиваем максимальный deltaTime (защита от больших скачков)
		if (deltaTime > 0.1f) {
			System.out.println("WARNING: Large deltaTime: " + deltaTime + ", clamping to 0.1f");
			deltaTime = 0.1f;
		}

		gameStateManager.update(deltaTime);
		gameStateManager.draw();

		GameKeys.update();
		
		// Мониторинг FPS каждые 60 кадров
		if (Gdx.graphics.getFrameId() % 60 == 0) {
			if (deltaTime > 0) {
				float fps = 1.0f / deltaTime;
				if (fps < 100) {
					System.out.println("Current FPS: " + fps + " | Delta: " + deltaTime + "ms");
				}
			} else {
				System.out.println("WARNING: Delta time is 0 or negative! Delta: " + deltaTime);
			}
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
}
