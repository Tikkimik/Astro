package com.mygdx.game;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.gdx.game.MyGdxGame;

// Please note that on macOS your application needs to be started with the -XstartOnFirstThread JVM argument
public class DesktopLauncher {
	public static void main (String[] arg) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setForegroundFPS(600);
		config.setTitle("Astro");
		config.setWindowedMode(1280, 720); // Фиксированный размер окна
		// config.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());
		config.setResizable(true);
		
		// МИНИМАЛЬНЫЕ настройки для теста FPS
		config.setBackBufferConfig(8, 8, 8, 8, 16, 0, 0); // Без антиалиасинга
		
		// Принудительно отключаем V-Sync
		config.useVsync(false);
		
		// Устанавливаем начальный лимит FPS (60)
		config.setForegroundFPS(60);
		
		// Простые настройки OpenGL
		System.setProperty("org.lwjgl.opengl.Display.allowSoftwareOpenGL", "false");
		System.setProperty("org.lwjgl.opengl.Display.enableHighDPI", "false");
		

		new Lwjgl3Application(new MyGdxGame(), config);
	}
}
