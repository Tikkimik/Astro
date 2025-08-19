package com.gdx.gamestates;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.Gdx;
import com.gdx.managers.GameStateManager;
import com.gdx.game.MyGdxGame;

/**
 * Минимальный тест FPS - только пустой экран и счетчик
 */
public class FPSTestState extends GameState {
    
    private BitmapFont font;
    private SpriteBatch spriteBatch;
    private int currentFPS = 0;
    private float fpsTimer = 0f;
    private int frameCount = 0;
    
    public FPSTestState(GameStateManager gameStateManager) {
        super(gameStateManager);
    }

    @Override
    public void init() {
        font = new BitmapFont();
        spriteBatch = new SpriteBatch();
        System.out.println("=== FPS TEST STATE INITIALIZED ===");
    }

    @Override
    public void update(float dt) {
        frameCount++;
        
        // Простейший расчет FPS
        fpsTimer += dt;
        if (fpsTimer >= 1.0f) {
            currentFPS = frameCount;
            frameCount = 0;
            fpsTimer = 0f;
            
            // Логируем каждую секунду
            System.out.println("FPS Test: " + currentFPS + " | DeltaTime: " + dt);
        }
    }

    @Override
    public void draw() {
        // Минимальная отрисовка - только текст
        spriteBatch.begin();
        font.setColor(1, 1, 1, 1); // Белый цвет
        font.draw(spriteBatch, "FPS TEST MODE", 50, MyGdxGame.HEIGHT - 50);
        font.draw(spriteBatch, "FPS: " + currentFPS, 50, MyGdxGame.HEIGHT - 100);
        font.draw(spriteBatch, "Frame: " + frameCount, 50, MyGdxGame.HEIGHT - 150);
        spriteBatch.end();
    }

    @Override
    public void handleInput() {
        // Пустая функция - никакого ввода
    }

    @Override
    public void dispose() {
        if (font != null) font.dispose();
        if (spriteBatch != null) spriteBatch.dispose();
    }
}
