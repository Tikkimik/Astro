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
import com.gdx.utils.DisplayManager;
import com.gdx.utils.GameLogger;

import java.util.ArrayList;

public class SettingsState extends GameState {

    private SpriteBatch spriteBatch;
    private BitmapFont titleFont;
    private BitmapFont menuFont;
    private BitmapFont tabFont;
    private ShapeRenderer shapeRenderer;
    
    private ArrayList<Particle> particles;
    
    // Вкладки
    private String[] tabs = {"GAMEPLAY", "GRAPHICS"};
    private int selectedTab = 0;
    
    // Настройки Gameplay
    private String[] gameplayItems = {
        "Logic FPS: " + GameSettings.getTargetLogicFPS(),
        "Render FPS: " + (GameSettings.getMaxRenderFPS() == 0 ? "UNLIMITED" : GameSettings.getMaxRenderFPS()),
        "Particles: " + GameSettings.getMaxParticles(),
        "Ship Engine Particles: " + GameSettings.getShipEngineParticles(),
        "Player Speed: " + (int)GameSettings.getPlayerSpeed(),
        "Bullet Speed: " + (int)GameSettings.getBulletSpeed(),
        "Pause System: " + getPauseSystemText(),
        "BACK"
    };
    
    // Настройки Graphics
    private String[] graphicsItems = {
        "Display Mode: " + getDisplayModeText(),
        "Debug Mode: " + (GameSettings.isDebugMode() ? "ON" : "OFF"),
        "Log Level: " + getLogLevelText(),
        "BACK"
    };
    
    private String[] gameplayDescriptions = {
        "Game logic update frequency (30-240 FPS)",
        "Maximum render FPS (30-240, UNLIMITED)",
        "Maximum particle count (100-5000)",
        "Ship engine particle count (0-100)",
        "Player ship movement speed (100-1000)",
        "Bullet projectile speed (100-1000)",
        "Pause system type (Style/Traditional)",
        "Return to main menu"
    };
    
    private String[] graphicsDescriptions = {
        "Window mode (Windowed/Borderless/Fullscreen)",
        "Enable debug mode for detailed logging",
        "Logging level (None/Error/Warn/Info/Debug)",
        "Return to main menu"
    };
    
    private int selectedItem = 0;
    
    private float titleY = 0;
    private float titleTargetY = 0;
    private float titleAnimationSpeed = 0.1f;
    
    private float menuAnimationTimer = 0f;
    private float menuAnimationSpeed = 2f;
    
    private Color titleColor;
    private Color menuColor;
    private Color selectedColor;
    private Color tabColor;
    private Color selectedTabColor;
    
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
        
        tabFont = new BitmapFont();
        tabFont.getData().setScale(1.2f);
        
        // Инициализируем цвета
        titleColor = new Color(1, 1, 1, 1);
        menuColor = new Color(0.8f, 0.8f, 1f, 1);
        selectedColor = new Color(1, 1, 0, 1);
        tabColor = new Color(0.6f, 0.6f, 0.8f, 1);
        selectedTabColor = new Color(1, 1, 0.5f, 1);
        
        titleFont.setColor(titleColor);
        menuFont.setColor(menuColor);
        
        shapeRenderer = new ShapeRenderer();
        particles = new ArrayList<>();
        
        screenWidth = Gdx.graphics.getWidth();
        screenHeight = Gdx.graphics.getHeight();
        
        titleTargetY = screenHeight * 0.85f;
        titleY = screenHeight + 100;
        
        // Создаем частицы для фона
        createBackgroundParticles();
    }

    @Override
    public void update(float dt) {
        // Анимация заголовка
        titleY += (titleTargetY - titleY) * titleAnimationSpeed;
        
        // Анимация меню
        menuAnimationTimer += dt * menuAnimationSpeed;
        
        // Обновляем частицы
        updateParticles(dt);
        
        // Обновляем тексты настроек
        updateSettingsTexts();
    }

    @Override
    public void draw() {
        // Обновляем размеры экрана при изменении режима отображения
        float newScreenWidth = Gdx.graphics.getWidth();
        float newScreenHeight = Gdx.graphics.getHeight();
        
        if (newScreenWidth != screenWidth || newScreenHeight != screenHeight) {
            screenWidth = newScreenWidth;
            screenHeight = newScreenHeight;
            titleTargetY = screenHeight * 0.85f;
            GameLogger.info("Screen size updated: " + (int)screenWidth + "x" + (int)screenHeight);
        }
        
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
        
        // Рисуем вкладки
        drawTabs();
        
        // Убираем подсказки по управлению
        
        // Рисуем описание текущего параметра ниже кнопки BACK
        menuFont.getData().setScale(1f);
        menuFont.setColor(0.8f, 0.8f, 1f, 0.9f);
        String[] descriptions = selectedTab == 0 ? gameplayDescriptions : graphicsDescriptions;
        String description = descriptions[selectedItem];
        float descWidth = menuFont.draw(spriteBatch, description, 0, 0).width;
        menuFont.draw(spriteBatch, description, 
            (screenWidth - descWidth) / 2, screenHeight * 0.08f);
        
        // Рисуем рекомендацию по текущему значению
        menuFont.getData().setScale(0.9f);
        menuFont.setColor(1f, 1f, 0.5f, 0.8f);
        String recommendation = getCurrentValueDescription();
        if (!recommendation.isEmpty()) {
            float recWidth = menuFont.draw(spriteBatch, recommendation, 0, 0).width;
            menuFont.draw(spriteBatch, recommendation, 
                (screenWidth - recWidth) / 2, screenHeight * 0.03f);
        }
        
        // Рисуем пункты настроек
        drawMenuItems();
        
        spriteBatch.end();
    }
    
    private void drawTabs() {
        float tabY = screenHeight * 0.80f;
        float tabSpacing = 200f;
        float startX = (screenWidth - (tabs.length - 1) * tabSpacing) / 2;
        
        for (int i = 0; i < tabs.length; i++) {
            float x = startX + i * tabSpacing;
            
            if (i == selectedTab) {
                tabFont.setColor(selectedTabColor);
                tabFont.getData().setScale(1.4f);
            } else {
                tabFont.setColor(tabColor);
                tabFont.getData().setScale(1.2f);
            }
            
            float tabWidth = tabFont.draw(spriteBatch, tabs[i], 0, 0).width;
            tabFont.draw(spriteBatch, tabs[i], x - tabWidth / 2, tabY);
        }
    }
    
    private void drawMenuItems() {
        String[] items = selectedTab == 0 ? gameplayItems : graphicsItems;
        float menuStartY = screenHeight * 0.70f;

        float menuSpacing = 70f;
        
        for (int i = 0; i < items.length; i++) {
            float y = menuStartY - i * menuSpacing;
            
            if (i == selectedItem) {
                menuFont.setColor(selectedColor.r, selectedColor.g, selectedColor.b, 1f);
                // Эффект пульсации для выбранного пункта
                float pulse = 1f + 0.1f * MathUtils.sin(menuAnimationTimer * 5f);
                menuFont.getData().setScale(1.8f * pulse);
            } else {
                menuFont.setColor(menuColor.r, menuColor.g, menuColor.b, 1f);
                menuFont.getData().setScale(1.8f);
            }
            
            String item = items[i];
            float itemWidth = menuFont.draw(spriteBatch, item, 0, 0).width;
            menuFont.draw(spriteBatch, item, 
                (screenWidth - itemWidth) / 2, y);
        }
    }
    
    private String getCurrentValueDescription() {
        if (selectedTab == 0) {
            // Gameplay описания
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
                    
                case 3: // Ship Engine Particles
                    int engineParticles = GameSettings.getShipEngineParticles();
                    if (engineParticles == 0) return "No engine effects";
                    else if (engineParticles <= 5) return "Minimal engine effects";
                    else if (engineParticles <= 15) return "Standard engine effects";
                    else if (engineParticles <= 30) return "Enhanced engine effects";
                    else return "Maximum engine effects";
                    
                case 4: // Player Speed
                    float playerSpeed = GameSettings.getPlayerSpeed();
                    if (playerSpeed <= 200) return "Slow movement";
                    else if (playerSpeed <= 300) return "Standard speed";
                    else if (playerSpeed <= 500) return "Fast movement";
                    else return "Very fast movement";
                    
                case 5: // Bullet Speed
                    float bulletSpeed = GameSettings.getBulletSpeed();
                    if (bulletSpeed <= 200) return "Slow bullets";
                    else if (bulletSpeed <= 350) return "Standard speed";
                    else if (bulletSpeed <= 500) return "Fast bullets";
                    else return "Very fast bullets";
                    
                default:
                    return "";
            }
        } else {
            // Graphics описания
            switch (selectedItem) {
                case 0: // Display Mode
                    int mode = GameSettings.getDisplayMode();
                    if (mode == 0) return "Standard windowed mode";
                    else if (mode == 1) return "Borderless windowed mode";
                    else return "Fullscreen mode";
                    
                case 1: // Resolution
                    int res = GameSettings.getResolution();
                    if (res == 0) return "1280x720 (HD)";
                    else if (res == 1) return "1920x1080 (FHD)";
                    else if (res == 2) return "2560x1440 (2K)";
                    else return "3840x2160 (4K)";
                    
                case 2: // V-Sync
                    if (GameSettings.isVSyncEnabled()) return "Reduces screen tearing";
                    else return "May cause screen tearing";
                    
                case 3: // Texture Quality
                    int quality = GameSettings.getTextureQuality();
                    if (quality == 0) return "Lower memory usage";
                    else if (quality == 1) return "Balanced quality";
                    else return "Best visual quality";
                    
                default:
                    return "";
            }
        }
    }
    
    private String getDisplayModeText() {
        switch (GameSettings.getDisplayMode()) {
            case 0: return "WINDOWED";
            case 1: return "BORDERLESS";
            case 2: return "FULLSCREEN";
            default: return "WINDOWED";
        }
    }
    
    private String getResolutionText() {
        switch (GameSettings.getResolution()) {
            case 0: return "HD (1280x720)";
            case 1: return "FHD (1920x1080)";
            case 2: return "2K (2560x1440)";
            case 3: return "4K (3840x2160)";
            default: return "FHD (1920x1080)";
        }
    }
    
    private String getTextureQualityText() {
        switch (GameSettings.getTextureQuality()) {
            case 0: return "LOW";
            case 1: return "MEDIUM";
            case 2: return "HIGH";
            default: return "MEDIUM";
        }
    }
    
    private String getLogLevelText() {
        return GameLogger.getCurrentLogLevelName();
    }
    
    private String getPauseSystemText() {
        switch (GameSettings.getPauseSystemType()) {
            case 0: return "Style";
            case 1: return "Traditional";
            default: return "Style";
        }
    }
    
    private void updateSettingsTexts() {
        // Обновляем Gameplay настройки
        gameplayItems[0] = "Logic FPS: " + GameSettings.getTargetLogicFPS();
        gameplayItems[1] = "Render FPS: " + (GameSettings.getMaxRenderFPS() == 0 ? "UNLIMITED" : GameSettings.getMaxRenderFPS());
        gameplayItems[2] = "Particles: " + GameSettings.getMaxParticles();
        gameplayItems[3] = "Ship Engine Particles: " + GameSettings.getShipEngineParticles();
        gameplayItems[4] = "Player Speed: " + (int)GameSettings.getPlayerSpeed();
        gameplayItems[5] = "Bullet Speed: " + (int)GameSettings.getBulletSpeed();
        gameplayItems[6] = "Pause System: " + getPauseSystemText();
        
        // Обновляем Graphics настройки
        graphicsItems[0] = "Display Mode: " + getDisplayModeText();
        graphicsItems[1] = "Debug Mode: " + (GameSettings.isDebugMode() ? "ON" : "OFF");
        graphicsItems[2] = "Log Level: " + getLogLevelText();
    }

    @Override
    public void handleInput() {
        // Переключение вкладок стрелочками вверх/вниз (когда выбран первый или последний пункт)
        String[] items = selectedTab == 0 ? gameplayItems : graphicsItems;
        
        if (GameKeys.isPressed(GameKeys.UP)) {
            if (selectedItem == 0) {
                // Если мы на первом пункте, переключаем на предыдущую вкладку
                selectedTab = (selectedTab - 1 + tabs.length) % tabs.length;
                selectedItem = 0; // Сбрасываем выбор на первый пункт
                GameLogger.info("Tab switched to: " + tabs[selectedTab]);
            } else {
                // Иначе переходим к предыдущему пункту
                selectedItem = (selectedItem - 1 + items.length) % items.length;
                GameLogger.info("Menu item changed UP to: " + items[selectedItem]);
            }
        }
        
        if (GameKeys.isPressed(GameKeys.DOWN)) {
            if (selectedItem == items.length - 1) {
                // Если мы на последнем пункте, переключаем на следующую вкладку
                selectedTab = (selectedTab + 1) % tabs.length;
                selectedItem = 0; // Сбрасываем выбор на первый пункт
                GameLogger.info("Tab switched to: " + tabs[selectedTab]);
            } else {
                // Иначе переходим к следующему пункту
                selectedItem = (selectedItem + 1) % items.length;
                GameLogger.info("Menu item changed DOWN to: " + items[selectedItem]);
            }
        }
        
        if (GameKeys.isPressed(GameKeys.ENTER)) {
            selectMenuItem();
        }
        
        // Управление настройками
        if (selectedTab == 0) {
            handleGameplaySettings();
        } else {
            handleGraphicsSettings();
        }
        
        // Возврат в меню
        if (GameKeys.isPressed(GameKeys.ESCAPE)) {
            gameStateManager.setState(GameStateManager.MENU);
        }
    }
    
    private void handleGameplaySettings() {
        if (selectedItem == 0) { // Logic FPS
            if (GameKeys.isPressed(GameKeys.LEFT)) {
                GameSettings.setTargetLogicFPS(GameSettings.getTargetLogicFPS() - 30);
            }
            if (GameKeys.isPressed(GameKeys.RIGHT)) {
                GameSettings.setTargetLogicFPS(GameSettings.getTargetLogicFPS() + 30);
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
        else if (selectedItem == 3) { // Ship Engine Particles
            if (GameKeys.isPressed(GameKeys.LEFT)) {
                GameSettings.setShipEngineParticles(GameSettings.getShipEngineParticles() - 1);
            }
            if (GameKeys.isPressed(GameKeys.RIGHT)) {
                GameSettings.setShipEngineParticles(GameSettings.getShipEngineParticles() + 1);
            }
        }
        else if (selectedItem == 4) { // Player Speed
            if (GameKeys.isPressed(GameKeys.LEFT)) {
                GameSettings.setPlayerSpeed(GameSettings.getPlayerSpeed() - 25);
            }
            if (GameKeys.isPressed(GameKeys.RIGHT)) {
                GameSettings.setPlayerSpeed(GameSettings.getPlayerSpeed() + 25);
            }
        }
        else if (selectedItem == 5) { // Bullet Speed
            if (GameKeys.isPressed(GameKeys.LEFT)) {
                GameSettings.setBulletSpeed(GameSettings.getBulletSpeed() - 25);
            }
            if (GameKeys.isPressed(GameKeys.RIGHT)) {
                GameSettings.setBulletSpeed(GameSettings.getBulletSpeed() + 25);
            }
        }
        else if (selectedItem == 6) { // Pause System
            if (GameKeys.isPressed(GameKeys.LEFT) || GameKeys.isPressed(GameKeys.RIGHT)) {
                int current = GameSettings.getPauseSystemType();
                current = (current + 1) % 2;
                GameSettings.setPauseSystemType(current);
                GameLogger.settings("Pause system changed to: " + getPauseSystemText());
            }
        }
    }
    
    private void handleGraphicsSettings() {
        if (selectedItem == 0) { // Display Mode
            if (GameKeys.isPressed(GameKeys.LEFT) || GameKeys.isPressed(GameKeys.RIGHT)) {
                int current = GameSettings.getDisplayMode();
                current = (current + 1) % 3;
                GameSettings.setDisplayMode(current);
                GameLogger.settings("Display mode changed to: " + getDisplayModeText());
                // Применяем настройки только при изменении
                applyDisplayMode();
            }
        }
        else if (selectedItem == 1) { // Debug Mode
            if (GameKeys.isPressed(GameKeys.LEFT) || GameKeys.isPressed(GameKeys.RIGHT)) {
                GameSettings.setDebugMode(!GameSettings.isDebugMode());
                GameLogger.settings("Debug mode " + (GameSettings.isDebugMode() ? "enabled" : "disabled"));
            }
        }
        else if (selectedItem == 2) { // Log Level
            if (GameKeys.isPressed(GameKeys.LEFT)) {
                int current = GameSettings.getLogLevel();
                current = (current - 1 + 5) % 5;
                GameSettings.setLogLevel(current);
                GameLogger.settings("Log level changed to: " + GameLogger.getCurrentLogLevelName());
            }
            if (GameKeys.isPressed(GameKeys.RIGHT)) {
                int current = GameSettings.getLogLevel();
                current = (current + 1) % 5;
                GameSettings.setLogLevel(current);
                GameLogger.settings("Log level changed to: " + GameLogger.getCurrentLogLevelName());
            }
        }
    }
    
    private void applyDisplayMode() {
        DisplayManager.applyDisplaySettings();
    }

    private void selectMenuItem() {
        String[] items = selectedTab == 0 ? gameplayItems : graphicsItems;
        int lastIndex = items.length - 1;
        
        if (selectedItem == lastIndex) { // BACK
            gameStateManager.setState(GameStateManager.MENU);
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
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle particle = particles.get(i);
            particle.update(dt);
            
            // Удаляем частицы, которые вышли за пределы экрана
            if (particle.getY() < -50) {
                particles.remove(i);
                createRandomParticle();
            }
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
        if (tabFont != null) {
            tabFont.dispose();
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
    }
}
