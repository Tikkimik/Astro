package com.gdx.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.gdx.managers.GameKeys;
import com.gdx.entities.Particle;
import com.gdx.game.MyGdxGame;
import com.gdx.utils.GameLogger;

import java.util.ArrayList;

/**
 * Обработчик состояния "Главное меню"
 * Перенесен из старого MenuState
 */
public class MenuStateHandler implements ImprovedGameStateManager.StateHandler {
    
    private boolean isInitialized = false;
    
    // Графика
    private SpriteBatch spriteBatch;
    private BitmapFont titleFont;
    private BitmapFont menuFont;
    private ShapeRenderer shapeRenderer;
    
    // Частицы
    private ArrayList<Particle> particles;
    
    // Меню
    private String[] menuItems = new String[3];
    private int selectedItem = 0;
    
    // Анимации
    private float titleY = 0;
    private float titleTargetY = 0;
    private float titleAnimationSpeed = 0.1f;
    private float menuAnimationTimer = 0f;
    private float menuAnimationSpeed = 2f;
    
    // Цвета
    private Color titleColor;
    private Color menuColor;
    private Color selectedColor;
    
    // Размеры экрана
    private float screenWidth;
    private float screenHeight;
    
    // Ссылка на менеджер состояний для переходов
    private ImprovedGameStateManager stateManager;
    
    @Override
    public void onEnter() {
        GameLogger.info("=== ВХОД В ГЛАВНОЕ МЕНЮ ===");
        
        if (!isInitialized) {
            GameLogger.info("Первая инициализация меню...");
            initializeMenu();
            isInitialized = true;
            GameLogger.info("Меню инициализировано, isInitialized = " + isInitialized);
        } else {
            GameLogger.info("Меню уже инициализировано, isInitialized = " + isInitialized);
        }
        
        // Курсор всегда на первом пункте при входе в меню
        selectedItem = 0;
        
        GameLogger.info("Главное меню активировано");
    }
    
    @Override
    public void onUpdate(float deltaTime) {
        // Проверяем, что меню инициализировано
        if (!isInitialized) {
            GameLogger.debug("onUpdate: меню не инициализировано, isInitialized = " + isInitialized);
            return;
        }
        
        // Анимация заголовка (независимо от FPS)
        titleY += (titleTargetY - titleY) * titleAnimationSpeed;
        
        // Анимация меню (независимо от FPS)
        menuAnimationTimer += deltaTime * menuAnimationSpeed;
        
        // Обновляем частицы
        updateParticles(deltaTime);
        
        // Удаляем мертвые частицы
        particles.removeIf(particle -> particle.shouldRemove());
        
        // Добавляем новые частицы (независимо от FPS)
        if (MathUtils.random() < 0.1f * deltaTime * 60f) {
            createRandomParticle();
        }
    }
    
    @Override
    public void onRender() {
        // Проверяем, что меню инициализировано
        if (!isInitialized) {
            GameLogger.debug("onRender: меню не инициализировано, isInitialized = " + isInitialized);
            return;
        }
        
        // Очищаем экран
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        // Рисуем частицы фона
        drawParticles();
        
        spriteBatch.begin();
        
        // Обновляем тексты
        updateTexts();
        
        // Рисуем заголовок
        String title = "ASTRO";
        float titleWidth = titleFont.draw(spriteBatch, title, 0, 0).width;
        titleFont.setColor(titleColor);
        titleFont.draw(spriteBatch, title, 
            (screenWidth - titleWidth) / 2, titleY);
        
        // Рисуем пункты меню
        float menuStartY = screenHeight * 0.5f;
        float menuSpacing = 80f;
        
        for (int i = 0; i < menuItems.length; i++) {
            float y = menuStartY - i * menuSpacing;
            float alpha = 1f;
            
            // Анимация появления пунктов меню
            if (y > screenHeight * 0.3f) {
                alpha = MathUtils.clamp((y - screenHeight * 0.3f) / 100f, 0f, 1f);
            }
            
            if (i == selectedItem) {
                menuFont.setColor(selectedColor.r, selectedColor.g, selectedColor.b, alpha);
                // Эффект пульсации для выбранного пункта
                float pulse = 1f + 0.1f * MathUtils.sin(menuAnimationTimer * 5f);
                menuFont.getData().setScale(2f * pulse);
            } else {
                menuFont.setColor(menuColor.r, menuColor.g, menuColor.b, alpha);
                menuFont.getData().setScale(2f);
            }
            
            String item = menuItems[i];
            float itemWidth = menuFont.draw(spriteBatch, item, 0, 0).width;
            menuFont.draw(spriteBatch, item, 
                (screenWidth - itemWidth) / 2, y);
        }
        
        // Рисуем рекорд
        menuFont.getData().setScale(1.2f);
        menuFont.setColor(1, 1, 0, 0.8f); // Желтый цвет
        String recordText = "High Score: " + MyGdxGame.highScore + " points";
        float recordWidth = menuFont.draw(spriteBatch, recordText, 0, 0).width;
        menuFont.draw(spriteBatch, recordText, 
            (screenWidth - recordWidth) / 2, screenHeight * 0.2f);
        
        // Рисуем подсказки
        menuFont.getData().setScale(1f);
        menuFont.setColor(0.6f, 0.6f, 0.6f, 0.8f);
        String hint = "Use arrows to navigate, ENTER to select";
        float hintWidth = menuFont.draw(spriteBatch, hint, 0, 0).width;
        menuFont.draw(spriteBatch, hint, 
            (screenWidth - hintWidth) / 2, screenHeight * 0.15f);
        
        String hint2 = "In game: ESC - return to menu, SPACE - shoot";
        float hint2Width = menuFont.draw(spriteBatch, hint2, 0, 0).width;
        menuFont.draw(spriteBatch, hint2, 
            (screenWidth - hint2Width) / 2, screenHeight * 0.12f);
        
        String hint3 = "Your progress is automatically saved when you pause";
        float hint3Width = menuFont.draw(spriteBatch, hint3, 0, 0).width;
        menuFont.draw(spriteBatch, hint3, 
            (screenWidth - hint3Width) / 2, screenHeight * 0.08f);
        
        spriteBatch.end();
    }
    
    @Override
    public void onHandleInput() {
        if (GameKeys.isPressed(GameKeys.UP)) {
            selectedItem = (selectedItem - 1 + menuItems.length) % menuItems.length;
        }
        
        if (GameKeys.isPressed(GameKeys.DOWN)) {
            selectedItem = (selectedItem + 1) % menuItems.length;
        }
        
        if (GameKeys.isPressed(GameKeys.ENTER)) {
            selectMenuItem();
        }
        
        // Сброс состояния клавиш
        GameKeys.update();
    }
    
    @Override
    public void onExit() {
        GameLogger.info("=== ВЫХОД ИЗ ГЛАВНОГО МЕНЮ ===");
        
        // Сохраняем настройки меню
        saveMenuSettings();
        
        GameLogger.info("Главное меню деактивировано");
    }
    
    /**
     * Инициализация меню
     */
    private void initializeMenu() {
        GameLogger.info("Инициализация главного меню...");
        
        // Инициализируем графику
        spriteBatch = new SpriteBatch();
        titleFont = new BitmapFont();
        titleFont.getData().setScale(3f);
        
        menuFont = new BitmapFont();
        menuFont.getData().setScale(2f);
        
        // Инициализируем цвета
        titleColor = new Color(1, 1, 1, 1);
        menuColor = new Color(0.8f, 0.8f, 1f, 1);
        selectedColor = new Color(1, 1, 0, 1);
        
        titleFont.setColor(titleColor);
        menuFont.setColor(menuColor);
        
        shapeRenderer = new ShapeRenderer();
        particles = new ArrayList<>();
        
        screenWidth = Gdx.graphics.getWidth();
        screenHeight = Gdx.graphics.getHeight();
        
        titleTargetY = screenHeight * 0.8f;
        titleY = screenHeight + 100;
        
        // Создаем частицы для фона
        createBackgroundParticles();
        
        // Обновляем тексты
        updateTexts();
        
        GameLogger.info("Главное меню инициализировано");
    }
    
    /**
     * Сохранение настроек меню
     */
    private void saveMenuSettings() {
        GameLogger.info("Сохранение настроек меню...");
        
        // Сохраняем выбранные опции
        // saveSelectedOptions();
        
        // Сохраняем позицию курсора
        // saveCursorPosition();
        
        GameLogger.info("Настройки меню сохранены");
    }
    
    /**
     * Выбор пункта меню
     */
    private void selectMenuItem() {
        switch (selectedItem) {
            case 0: // PLAY
                if (stateManager != null) {
                    stateManager.setState(ImprovedGameStateManager.GameState.PLAYING);
                }
                break;
            case 1: // SETTINGS
                if (stateManager != null) {
                    stateManager.setState(ImprovedGameStateManager.GameState.SETTINGS);
                }
                break;
            case 2: // EXIT
                Gdx.app.exit();
                break;
        }
    }
    
    /**
     * Создание фоновых частиц
     */
    private void createBackgroundParticles() {
        for (int i = 0; i < 50; i++) {
            createRandomParticle();
        }
    }
    
    /**
     * Создание случайной частицы
     */
    private void createRandomParticle() {
        float x = MathUtils.random(screenWidth);
        float y = MathUtils.random(screenHeight);
        
        Particle particle = new Particle(x, y);
        particles.add(particle);
    }
    
    /**
     * Обновление частиц
     */
    private void updateParticles(float deltaTime) {
        for (Particle particle : particles) {
            particle.update(deltaTime);
        }
    }
    
    /**
     * Отрисовка частиц
     */
    private void drawParticles() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        for (Particle particle : particles) {
            shapeRenderer.setColor(0.5f, 0.7f, 1f, 0.3f);
            shapeRenderer.circle(particle.getX(), particle.getY(), 2f);
        }
        
        shapeRenderer.end();
    }
    
    /**
     * Обновление текстов меню
     */
    private void updateTexts() {
        // Пока используем только английский, чтобы не было квадратов
        menuItems[0] = "PLAY";
        menuItems[1] = "SETTINGS";
        menuItems[2] = "EXIT";
    }
    
    /**
     * Установка ссылки на менеджер состояний
     */
    public void setStateManager(ImprovedGameStateManager stateManager) {
        this.stateManager = stateManager;
    }
}
