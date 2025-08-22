package com.gdx.gamestates;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.gdx.game.MyGdxGame;
import com.gdx.managers.GameKeys;
import com.gdx.managers.GameStateManager;
import com.gdx.entities.Particle;
import com.gdx.utils.GameSettings;

import java.util.ArrayList;

public class SettingsState extends GameState {

    private SpriteBatch spriteBatch;
    private BitmapFont titleFont;
    private BitmapFont menuFont;
    private ShapeRenderer shapeRenderer;
    
    private ArrayList<Particle> particles;
    
    private String[] settingsItems = {
        "Logic FPS: " + GameSettings.getTargetLogicFPS(),
        "Render FPS: " + (GameSettings.getMaxRenderFPS() == 0 ? "UNLIMITED" : GameSettings.getMaxRenderFPS()),
        "Particles: " + GameSettings.getMaxParticles(),
        "Player Speed: " + (int)GameSettings.getPlayerSpeed(),
        "Bullet Speed: " + (int)GameSettings.getBulletSpeed(),
        "BACK"
    };
    
    private String[] settingDescriptions = {
        "Game logic update frequency (30-240 FPS)",
        "Maximum render FPS (30-240, UNLIMITED)",
        "Maximum particle count (100-5000)",
        "Player ship movement speed (100-1000)",
        "Bullet projectile speed (100-1000)",
        "Return to main menu"
    };
    
    private String getCurrentValueDescription() {
        switch (selectedItem) {
            case 0: // Logic FPS
                int logicFPS = GameSettings.getTargetLogicFPS();
                if (logicFPS <= 30) return "For weak devices";
                else if (logicFPS <= 60) return "Standard setting";
                else if (logicFPS <= 120) return "For powerful devices";
                else return "For top devices";
                
            case 1: // Render FPS
                int renderFPS = GameSettings.getMaxRenderFPS();
                if (renderFPS == 0) return "Unlimited";
                else if (renderFPS <= 30) return "Battery saving";
                else if (renderFPS <= 60) return "Standard setting";
                else if (renderFPS <= 120) return "For 120Hz monitors";
                else return "Maximum smoothness";
                
            case 2: // Particles
                int particles = GameSettings.getMaxParticles();
                if (particles <= 500) return "Minimal effects";
                else if (particles <= 1000) return "Standard effects";
                else if (particles <= 2000) return "Enhanced effects";
                else return "Maximum effects";
                
            case 3: // Player Speed
                float playerSpeed = GameSettings.getPlayerSpeed();
                if (playerSpeed <= 200) return "Slow movement";
                else if (playerSpeed <= 300) return "Standard speed";
                else if (playerSpeed <= 500) return "Fast movement";
                else return "Very fast movement";
                
            case 4: // Bullet Speed
                float bulletSpeed = GameSettings.getBulletSpeed();
                if (bulletSpeed <= 200) return "Slow bullets";
                else if (bulletSpeed <= 350) return "Standard speed";
                else if (bulletSpeed <= 500) return "Fast bullets";
                else return "Very fast bullets";
                
            default:
                return "";
        }
    }
    private int selectedItem = 0;
    
    private float titleY = 0;
    private float titleTargetY = 0;
    private float titleAnimationSpeed = 0.1f;
    
    private float menuAnimationTimer = 0f;
    private float menuAnimationSpeed = 2f;
    
    private Color titleColor;
    private Color menuColor;
    private Color selectedColor;
    
    private float screenWidth;
    private float screenHeight;

    public SettingsState(GameStateManager gameStateManager) {
        super(gameStateManager);
    }

    @Override
    public void init() {
        spriteBatch = new SpriteBatch();
        titleFont = new BitmapFont();
        titleFont.getData().setScale(2.5f);
        
        menuFont = new BitmapFont();
        menuFont.getData().setScale(1.8f);
        
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
    }

    @Override
    public void update(float dt) {
        handleInput();
        
        // Анимация заголовка (независимо от FPS)
        titleY += (titleTargetY - titleY) * titleAnimationSpeed;
        
        // Анимация меню (независимо от FPS)
        menuAnimationTimer += dt * menuAnimationSpeed;
        
        // Обновляем частицы
        updateParticles(dt);
        
        // Удаляем мертвые частицы
        particles.removeIf(particle -> particle.shouldRemove());
        
        // Добавляем новые частицы (независимо от FPS)
        if (MathUtils.random() < 0.1f * dt * 60f) { // Нормализуем к 60 FPS
            createRandomParticle();
        }
        
        // Обновляем тексты настроек
        updateSettingsTexts();
    }

    @Override
    public void draw() {
        // Очищаем экран
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        // Рисуем частицы фона
        drawParticles();
        
        spriteBatch.begin();
        
        // Рисуем заголовок
        String title = "SETTINGS";
        float titleWidth = titleFont.draw(spriteBatch, title, 0, 0).width;
        titleFont.setColor(titleColor);
        titleFont.draw(spriteBatch, title, 
            (screenWidth - titleWidth) / 2, titleY);
        
        // Рисуем подсказки в верхней части экрана
        menuFont.getData().setScale(0.8f);
        menuFont.setColor(0.6f, 0.6f, 0.6f, 0.8f);
        String hint = "Use arrows to navigate, ENTER to select";
        float hintWidth = menuFont.draw(spriteBatch, hint, 0, 0).width;
        menuFont.draw(spriteBatch, hint, 
            (screenWidth - hintWidth) / 2, screenHeight * 0.85f);
        
        String hint2 = "Settings: ← → to change, ESC to return";
        float hint2Width = menuFont.draw(spriteBatch, hint2, 0, 0).width;
        menuFont.draw(spriteBatch, hint2, 
            (screenWidth - hint2Width) / 2, screenHeight * 0.80f);
        
        // Рисуем описание текущего параметра
        menuFont.getData().setScale(1f);
        menuFont.setColor(0.8f, 0.8f, 1f, 0.9f);
        String description = settingDescriptions[selectedItem];
        float descWidth = menuFont.draw(spriteBatch, description, 0, 0).width;
        menuFont.draw(spriteBatch, description, 
            (screenWidth - descWidth) / 2, screenHeight * 0.75f);
        
        // Рисуем рекомендацию по текущему значению
        menuFont.getData().setScale(0.9f);
        menuFont.setColor(1f, 1f, 0.5f, 0.8f);
        String recommendation = getCurrentValueDescription();
        if (!recommendation.isEmpty()) {
            float recWidth = menuFont.draw(spriteBatch, recommendation, 0, 0).width;
            menuFont.draw(spriteBatch, recommendation, 
                (screenWidth - recWidth) / 2, screenHeight * 0.70f);
        }
        
        // Рисуем пункты настроек (подняли выше)
        float menuStartY = screenHeight * 0.6f;
        float menuSpacing = 70f;
        
        for (int i = 0; i < settingsItems.length; i++) {
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
                menuFont.getData().setScale(1.8f * pulse);
            } else {
                menuFont.setColor(menuColor.r, menuColor.g, menuColor.b, alpha);
                menuFont.getData().setScale(1.8f);
            }
            
            String item = settingsItems[i];
            float itemWidth = menuFont.draw(spriteBatch, item, 0, 0).width;
            menuFont.draw(spriteBatch, item, 
                (screenWidth - itemWidth) / 2, y);
        }
        

        
        spriteBatch.end();
    }
    
    private void updateSettingsTexts() {
        settingsItems[0] = "Logic FPS: " + GameSettings.getTargetLogicFPS();
        settingsItems[1] = "Render FPS: " + (GameSettings.getMaxRenderFPS() == 0 ? "UNLIMITED" : GameSettings.getMaxRenderFPS());
        settingsItems[2] = "Particles: " + GameSettings.getMaxParticles();
        settingsItems[3] = "Player Speed: " + (int)GameSettings.getPlayerSpeed();
        settingsItems[4] = "Bullet Speed: " + (int)GameSettings.getBulletSpeed();
    }

    @Override
    public void handleInput() {
        if (GameKeys.isPressed(GameKeys.UP)) {
            selectedItem = (selectedItem - 1 + settingsItems.length) % settingsItems.length;
        }
        
        if (GameKeys.isPressed(GameKeys.DOWN)) {
            selectedItem = (selectedItem + 1) % settingsItems.length;
        }
        
        if (GameKeys.isPressed(GameKeys.ENTER)) {
            selectMenuItem();
        }
        
        // Управление настройками
        if (selectedItem == 0) { // Logic FPS
            if (GameKeys.isPressed(GameKeys.LEFT)) {
                GameSettings.setTargetLogicFPS(GameSettings.getTargetLogicFPS() - 10);
            }
            if (GameKeys.isPressed(GameKeys.RIGHT)) {
                GameSettings.setTargetLogicFPS(GameSettings.getTargetLogicFPS() + 10);
            }
        }
        else if (selectedItem == 1) { // Render FPS
            if (GameKeys.isPressed(GameKeys.LEFT)) {
                int current = GameSettings.getMaxRenderFPS();
                if (current == 0) current = 240;
                else if (current <= 30) current = 0;
                else current -= 30;
                GameSettings.setMaxRenderFPS(current);
            }
            if (GameKeys.isPressed(GameKeys.RIGHT)) {
                int current = GameSettings.getMaxRenderFPS();
                if (current == 0) current = 30;
                else if (current >= 240) current = 0;
                else current += 30;
                GameSettings.setMaxRenderFPS(current);
            }
        }
        else if (selectedItem == 2) { // Particles
            if (GameKeys.isPressed(GameKeys.LEFT)) {
                GameSettings.setMaxParticles(GameSettings.getMaxParticles() - 100);
            }
            if (GameKeys.isPressed(GameKeys.RIGHT)) {
                GameSettings.setMaxParticles(GameSettings.getMaxParticles() + 100);
            }
        }
        else if (selectedItem == 3) { // Player Speed
            if (GameKeys.isPressed(GameKeys.LEFT)) {
                GameSettings.setPlayerSpeed(GameSettings.getPlayerSpeed() - 25);
            }
            if (GameKeys.isPressed(GameKeys.RIGHT)) {
                GameSettings.setPlayerSpeed(GameSettings.getPlayerSpeed() + 25);
            }
        }
        else if (selectedItem == 4) { // Bullet Speed
            if (GameKeys.isPressed(GameKeys.LEFT)) {
                GameSettings.setBulletSpeed(GameSettings.getBulletSpeed() - 25);
            }
            if (GameKeys.isPressed(GameKeys.RIGHT)) {
                GameSettings.setBulletSpeed(GameSettings.getBulletSpeed() + 25);
            }
        }
        
        // Возврат в меню
        if (GameKeys.isPressed(GameKeys.ESCAPE)) {
            gameStateManager.setState(GameStateManager.MENU);
        }
        
        // Сброс состояния клавиш
        GameKeys.update();
    }

    private void selectMenuItem() {
        switch (selectedItem) {
            case 5: // BACK
                gameStateManager.setState(GameStateManager.MENU);
                break;
        }
    }

    private void createBackgroundParticles() {
        for (int i = 0; i < 30; i++) {
            createRandomParticle();
        }
    }

    private void createRandomParticle() {
        float x = MathUtils.random(screenWidth);
        float y = MathUtils.random(screenHeight);
        
        Particle particle = new Particle(x, y);
        particles.add(particle);
    }

    private void updateParticles(float dt) {
        for (Particle particle : particles) {
            particle.update(dt);
        }
    }

    private void drawParticles() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        for (Particle particle : particles) {
            shapeRenderer.setColor(0.5f, 0.7f, 1f, 0.3f);
            shapeRenderer.circle(particle.getX(), particle.getY(), particle.getWidth() / 2);
        }
        
        shapeRenderer.end();
    }

    @Override
    public void dispose() {
        if (spriteBatch != null) {
            spriteBatch.dispose();
        }
        if (titleFont != null) {
            titleFont.dispose();
        }
        if (menuFont != null) {
            menuFont.dispose();
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
    }
}
