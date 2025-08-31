package com.gdx.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Пример системы паузы 
 * 
 * Основные принципы:
 * 1. Локальная пауза - игра останавливается, но не переключается в другое состояние
 * 2. Оверлей паузы - полупрозрачный фон с текстом
 * 3. Продолжение рендеринга - игра продолжает отрисовываться, но логика останавливается
 * 4. Простое управление - одна клавиша для включения/выключения
 */
public class PauseSystemExample {
    
    // Состояние паузы
    private boolean isPaused = false;
    private float pauseTime = 0f;
    private float totalPauseTime = 0f;
    
    // UI элементы
    private BitmapFont font;
    private SpriteBatch spriteBatch;
    private ShapeRenderer shapeRenderer;
    
    public PauseSystemExample() {
        font = new BitmapFont();
        spriteBatch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
    }
    
    /**
     * Обновление логики игры
     * @param dt время между кадрами
     */
    public void update(float dt) {
        // Если игра на паузе - пропускаем всю игровую логику
        if (isPaused) {
            pauseTime += dt;
            totalPauseTime += dt;
            return; // Выходим из метода, не обновляя игру
        }
        
        // Здесь идет обычная игровая логика
        updatePlayer(dt);
        updateEnemies(dt);
        updateBullets(dt);
        updateCollisions(dt);
    }
    
    /**
     * Отрисовка игры
     */
    public void render() {
        // Отрисовываем игру всегда (даже на паузе)
        renderGame();
        
        // Отрисовываем оверлей паузы, если игра на паузе
        if (isPaused) {
            renderPauseOverlay();
        }
    }
    
    /**
     * Обработка ввода
     */
    public void handleInput() {
        // Проверяем нажатие клавиши паузы (например, ESC)
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
            togglePause();
        }
    }
    
    /**
     * Переключение паузы
     */
    private void togglePause() {
        isPaused = !isPaused;
        if (isPaused) {
            System.out.println("=== ПАУЗА АКТИВИРОВАНА ===");
            pauseTime = 0f;
        } else {
            System.out.println("=== ПАУЗА ОТКЛЮЧЕНА ===");
            System.out.println("Общее время в паузе: " + totalPauseTime + " сек");
        }
    }
    
    /**
     * Отрисовка оверлея паузы
     */
    private void renderPauseOverlay() {
        // Полупрозрачный фон
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.7f);
        shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapeRenderer.end();
        
        // Текст паузы
        spriteBatch.begin();
        font.setColor(Color.WHITE);
        font.getData().setScale(2f);
        
        String pauseText = "ПАУЗА";
        float textWidth = font.draw(spriteBatch, pauseText, 0, 0).width;
        font.draw(spriteBatch, pauseText, 
                 (Gdx.graphics.getWidth() - textWidth) / 2, 
                 Gdx.graphics.getHeight() / 2 + 50);
        
        // Подсказка
        font.getData().setScale(1f);
        String hintText = "Нажмите ESC для продолжения";
        textWidth = font.draw(spriteBatch, hintText, 0, 0).width;
        font.draw(spriteBatch, hintText, 
                 (Gdx.graphics.getWidth() - textWidth) / 2, 
                 Gdx.graphics.getHeight() / 2 - 20);
        
        // Время в паузе
        font.setColor(0.8f, 0.8f, 0.8f, 1);
        String timeText = String.format("Время в паузе: %.1f сек", pauseTime);
        textWidth = font.draw(spriteBatch, timeText, 0, 0).width;
        font.draw(spriteBatch, timeText, 
                 (Gdx.graphics.getWidth() - textWidth) / 2, 
                 Gdx.graphics.getHeight() / 2 - 50);
        
        spriteBatch.end();
        
        // Сбрасываем размер шрифта
        font.getData().setScale(1f);
    }
    
    // Заглушки для игровой логики
    private void updatePlayer(float dt) {
        // Обновление игрока
    }
    
    private void updateEnemies(float dt) {
        // Обновление врагов
    }
    
    private void updateBullets(float dt) {
        // Обновление пуль
    }
    
    private void updateCollisions(float dt) {
        // Проверка коллизий
    }
    
    private void renderGame() {
        // Отрисовка игры
    }
    
    /**
     * Освобождение ресурсов
     */
    public void dispose() {
        if (font != null) font.dispose();
        if (spriteBatch != null) spriteBatch.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
    }
    
    // Геттеры
    public boolean isPaused() { return isPaused; }
    public float getPauseTime() { return pauseTime; }
    public float getTotalPauseTime() { return totalPauseTime; }
}
