package com.gdx.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.gdx.utils.GameLogger;

/**
 * Обработчик состояния "Пауза"
 * Показывает экран паузы с возможностью возврата в игру или в меню
 */
public class PausedStateHandler implements ImprovedGameStateManager.StateHandler {
    
    private boolean isInitialized = false;
    
    // Графика
    private SpriteBatch spriteBatch;
    private BitmapFont titleFont;
    private BitmapFont menuFont;
    private ShapeRenderer shapeRenderer;
    
    // Меню паузы - упрощаем до одной кнопки
    private String pauseButtonText = "Return to Menu";
    
    // Анимация
    private float animationTimer = 0f;
    private float animationSpeed = 2f;
    
    // Ссылка на менеджер состояний для переходов
    private ImprovedGameStateManager stateManager;
    
    @Override
    public void onEnter() {
        GameLogger.info("=== ВХОД В СОСТОЯНИЕ ПАУЗЫ ===");
        
        if (!isInitialized) {
            initializePauseScreen();
            isInitialized = true;
        }
        
        GameLogger.info("Экран паузы активирован");
    }
    
    @Override
    public void onUpdate(float deltaTime) {
        if (!isInitialized) {
            return;
        }
        
        // Обновляем анимацию
        animationTimer += deltaTime * animationSpeed;
    }
    
    @Override
    public void onRender() {
        if (!isInitialized) {
            return;
        }
        
        // НЕ очищаем экран - оставляем игру видимой на фоне
        // Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 0.8f);
        // Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        // Рисуем полупрозрачный оверлей поверх игры
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        
        // Полупрозрачный темный оверлей
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.3f); // Очень прозрачный черный
        shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapeRenderer.end();
        
        Gdx.gl.glDisable(GL20.GL_BLEND);
        
        spriteBatch.begin();
        
        // Рисуем заголовок "PAUSED"
        String title = "PAUSED";
        float titleWidth = titleFont.draw(spriteBatch, title, 0, 0).width;
        titleFont.setColor(Color.WHITE);
        titleFont.draw(spriteBatch, title, 
            (Gdx.graphics.getWidth() - titleWidth) / 2, 
            Gdx.graphics.getHeight() * 0.7f);
        
        // Рисуем кнопку выхода в меню
        float buttonY = Gdx.graphics.getHeight() * 0.5f;
        
        // Эффект пульсации для кнопки
        float pulse = 1f + 0.1f * (float)Math.sin(animationTimer * 5f);
        float scale = 1.5f * pulse;
        
        menuFont.getData().setScale(scale);
        menuFont.setColor(Color.YELLOW);
        
        float buttonWidth = menuFont.draw(spriteBatch, pauseButtonText, 0, 0).width;
        menuFont.draw(spriteBatch, pauseButtonText, 
            (Gdx.graphics.getWidth() - buttonWidth) / 2, buttonY);
        
        // Рисуем подсказки
        menuFont.getData().setScale(1f);
        menuFont.setColor(0.7f, 0.7f, 0.7f, 1f);
        String hint = "Press ENTER to return to menu or ESC to continue game";
        float hintWidth = menuFont.draw(spriteBatch, hint, 0, 0).width;
        menuFont.draw(spriteBatch, hint, 
            (Gdx.graphics.getWidth() - hintWidth) / 2, 
            Gdx.graphics.getHeight() * 0.2f);
        
        spriteBatch.end();
    }
    
    @Override
    public void onHandleInput() {
        if (!isInitialized) {
            return;
        }
        
        // Обработка нажатия Enter для выхода в меню
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ENTER)) {
            GameLogger.info("Enter pressed in pause - return to main menu");
            if (stateManager != null) {
                stateManager.setState(ImprovedGameStateManager.GameState.MENU);
            }
        }
        
        // ESC также возвращает в игру
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
            GameLogger.info("ESC pressed in pause - return to game");
            if (stateManager != null) {
                stateManager.setState(ImprovedGameStateManager.GameState.PLAYING);
            }
        }
    }
    
    @Override
    public void onExit() {
        GameLogger.info("=== ВЫХОД ИЗ СОСТОЯНИЯ ПАУЗЫ ===");
        
        // Сохраняем настройки паузы
        savePauseSettings();
        
        GameLogger.info("Экран паузы деактивирован");
    }
    
    /**
     * Инициализация экрана паузы
     */
    private void initializePauseScreen() {
        GameLogger.info("Инициализация экрана паузы...");
        
        // Инициализируем графику
        spriteBatch = new SpriteBatch();
        titleFont = new BitmapFont();
        titleFont.getData().setScale(4f);
        
        menuFont = new BitmapFont();
        menuFont.getData().setScale(1.5f);
        
        shapeRenderer = new ShapeRenderer();
        
        GameLogger.info("Экран паузы инициализирован");
    }
    

    
    /**
     * Сохранение настроек паузы
     */
    private void savePauseSettings() {
        GameLogger.info("Сохранение настроек паузы...");
        
        // Здесь можно сохранить настройки паузы
        // Например, выбранный пункт меню
        
        GameLogger.info("Настройки паузы сохранены");
    }
    
    /**
     * Установить ссылку на менеджер состояний
     */
    public void setStateManager(ImprovedGameStateManager stateManager) {
        this.stateManager = stateManager;
    }
}
