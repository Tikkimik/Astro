package com.gdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.gdx.managers.GameInputProcessor;
import com.gdx.managers.GameKeys;
import com.gdx.managers.GameStateManager;

public class MyGdxGame extends ApplicationAdapter {
	public SpriteBatch batch;
	public GameStateManager gameStateManager;
	public static int WIDTH ;
	public static int HEIGHT;
	public static OrthographicCamera camera;

	/**
	 * метод инициализации
	 */
	@Override
	public void create () {  //метод инициализации по сути
		WIDTH = Gdx.graphics.getWidth();
		HEIGHT = Gdx.graphics.getHeight();

		camera = new OrthographicCamera(WIDTH, HEIGHT);
		camera.setToOrtho(true);
//		camera.translate(WIDTH, HEIGHT);
//		camera.update();

		Gdx.input.setInputProcessor(new GameInputProcessor());

		gameStateManager = new GameStateManager();

		batch = new SpriteBatch();
	}

	/**
	 * тут происходить вся игра
	 * нужно все делить все на разные составляющие чтобы тут небыло хаоса
	 */
	@Override
	public void render () {
		ScreenUtils.clear(0, 0, 0, 1); //black

		gameStateManager.update(Gdx.graphics.getDeltaTime());
		gameStateManager.draw();

		GameKeys.update();

		batch.begin();
		batch.end();
	}

	@Override
	public void resize (int width, int height) {

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
	}
}