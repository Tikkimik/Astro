package com.gdx.gamestates;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.gdx.managers.GameKeys;
import com.gdx.managers.GameStateManager;
import com.gdx.managers.ProgressionManager;
import com.gdx.managers.ProgressionManager.Upgrade;
import com.gdx.gamestates.PlayState;
import com.badlogic.gdx.math.MathUtils;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class UpgradeSelectionState extends GameState {
    
    private ProgressionManager progressionManager;
    private List<Upgrade> allAvailableUpgrades; // Все доступные улучшения
    private List<Upgrade> displayUpgrades; // Только 3 случайных для отображения
    private int selectedUpgrade = 0;
    private BitmapFont titleFont;
    private BitmapFont menuFont;
    private SpriteBatch spriteBatch;
    private ShapeRenderer shapeRenderer;
    
    // Цвета как в MenuState
    private Color titleColor;
    private Color menuColor;
    private Color selectedColor;
    
    // Анимация
    private float animationTimer = 0f;
    private float selectionTimer = 0f;
    private float transitionTimer = 0f;
    private float menuAppearTimer = 0f;
    private int previousSelectedUpgrade = 0;
    private boolean isTransitioning = false;
    private boolean isMenuAppearing = true;
    
    // Цвета для плавного перехода фона
    private static final float GAME_BG_R = 0f;    // Черный фон игры
    private static final float GAME_BG_G = 0f;
    private static final float GAME_BG_B = 0f;
    private static final float MENU_BG_R = 0.1f; // Темно-синий фон меню
    private static final float MENU_BG_G = 0.1f;
    private static final float MENU_BG_B = 0.2f;
    
    public UpgradeSelectionState(GameStateManager gameStateManager, ProgressionManager progressionManager) {
        super(gameStateManager);
        this.progressionManager = progressionManager;
        if (progressionManager != null) {
            this.allAvailableUpgrades = progressionManager.getAvailableUpgrades();
        } else {
            this.allAvailableUpgrades = new ArrayList<>();
        }
    }
    
    @Override
    public void init() {
        // Полностью копируем подход из MenuState
        spriteBatch = new SpriteBatch();
        titleFont = new BitmapFont();
        titleFont.getData().setScale(3f);
        
        menuFont = new BitmapFont();
        menuFont.getData().setScale(2f);
        
        // Инициализируем цвета точно как в MenuState
        titleColor = new Color(1, 1, 1, 1);
        menuColor = new Color(0.8f, 0.8f, 1f, 1);
        selectedColor = new Color(1, 1, 0, 1);
        
        titleFont.setColor(titleColor);
        menuFont.setColor(menuColor);
        
        shapeRenderer = new ShapeRenderer();
        
        // Создаем список для отображения
        displayUpgrades = new ArrayList<>();
        
        // Проверяем, что у нас есть доступные улучшения
        if (allAvailableUpgrades == null || allAvailableUpgrades.isEmpty()) {
            // Если нет улучшений, создаем тестовые улучшения
            displayUpgrades.add(new ProgressionManager.Upgrade("Speed Boost", "Move speed +20%", () -> {}, "SPEED"));
            displayUpgrades.add(new ProgressionManager.Upgrade("Bullet Damage", "Bullet damage +30%", () -> {}, "BULLET_DAMAGE"));
            displayUpgrades.add(new ProgressionManager.Upgrade("Health Boost", "Max health +1", () -> {}, "MAX_HEALTH"));
        } else {
            // Выбираем 3 случайных улучшения для показа
            Random random = new Random();
            List<Upgrade> tempList = new ArrayList<>(allAvailableUpgrades);
            
            while (displayUpgrades.size() < 3 && !tempList.isEmpty()) {
                int randomIndex = random.nextInt(tempList.size());
                displayUpgrades.add(tempList.remove(randomIndex));
            }
            

        }
        

    }
    
    @Override
    public void update(float dt) {
        animationTimer += dt;
        selectionTimer += dt;
        
        // Обработка анимации появления меню
        if (isMenuAppearing) {
            menuAppearTimer += dt;
            if (menuAppearTimer >= 0.8f) { // Длительность появления 0.8 секунды
                isMenuAppearing = false;
            }
        }
        
        // Обработка анимации переходов
        if (isTransitioning) {
            transitionTimer += dt;
            if (transitionTimer >= 0.3f) { // Длительность перехода 0.3 секунды
                isTransitioning = false;
            }
        }
        
        handleInput();
    }
    
    @Override
    public void handleInput() {
        if (GameKeys.isPressed(GameKeys.LEFT)) {
            previousSelectedUpgrade = selectedUpgrade;
            selectedUpgrade = (selectedUpgrade + 1) % displayUpgrades.size();
            if (previousSelectedUpgrade != selectedUpgrade) {
                isTransitioning = true;
                transitionTimer = 0f;
            }
        }
        
        if (GameKeys.isPressed(GameKeys.RIGHT)) {
            previousSelectedUpgrade = selectedUpgrade;
            selectedUpgrade = (selectedUpgrade - 1 + displayUpgrades.size()) % displayUpgrades.size();
            if (previousSelectedUpgrade != selectedUpgrade) {
                isTransitioning = true;
                transitionTimer = 0f;
            }
        }
        
        if (GameKeys.isPressed(GameKeys.ENTER)) {
            selectUpgrade();
        }
    }
    
    private void selectUpgrade() {
        if (displayUpgrades.isEmpty()) return;
        
        Upgrade selected = displayUpgrades.get(selectedUpgrade);
        selected.apply();
        progressionManager.getSelectedUpgrades().add(selected);
        
        System.out.println("=== ВЫБОР УЛУЧШЕНИЯ ===");
        System.out.println("Выбрано улучшение: " + selected.getName());
        System.out.println("Возвращаемся к игре с ProgressionManager");
        
        // Возвращаемся к игре, передавая ProgressionManager
        gameStateManager.setState(GameStateManager.PLAY, progressionManager);
    }
    
    @Override
    public void draw() {
        // Плавный переход цветов фона
        float bgProgress = isMenuAppearing ? Math.min(menuAppearTimer / 0.8f, 1f) : 1f;
        
        // Интерполируем между цветом игры и цветом меню
        float currentR = GAME_BG_R + (MENU_BG_R - GAME_BG_R) * bgProgress;
        float currentG = GAME_BG_G + (MENU_BG_G - GAME_BG_G) * bgProgress;
        float currentB = GAME_BG_B + (MENU_BG_B - GAME_BG_B) * bgProgress;
        
        // Очищаем экран с плавным переходом цветов
        Gdx.gl.glClearColor(currentR, currentG, currentB, 1f);
        Gdx.gl.glClear(Gdx.gl.GL_COLOR_BUFFER_BIT);
        
        spriteBatch.begin();
        
        // Сначала рисуем карточки улучшений
        drawUpgradeCards();
        
        // Затем рисуем текст поверх всего
        spriteBatch.end();
        
        // Начинаем новый batch для текста
        spriteBatch.begin();
        
        // Анимация появления заголовков
        float headerDelay = 0.2f;
        float headerProgress = isMenuAppearing ? Math.max(0f, (menuAppearTimer - headerDelay) / (0.8f - headerDelay)) : 1f;
        headerProgress = Math.min(headerProgress, 1f);
        
        // Заголовок с анимацией
        String title = "LEVEL UP";
        titleFont.getData().setScale(3f);
        float titleWidth = titleFont.draw(spriteBatch, title, 0, 0).width;
        titleFont.setColor(titleColor.r, titleColor.g, titleColor.b, titleColor.a * headerProgress);
        float titleY = Gdx.graphics.getHeight() - 100 + (1f - headerProgress) * 30f;
        titleFont.draw(spriteBatch, title, 
            (Gdx.graphics.getWidth() - titleWidth) / 2, titleY);
        
        // Подзаголовок с анимацией
        titleFont.getData().setScale(1.5f);
        titleFont.setColor(menuColor.r, menuColor.g, menuColor.b, menuColor.a * headerProgress);
        String subtitle = "CHOOSE UPGRADE";
        float subtitleWidth = titleFont.draw(spriteBatch, subtitle, 0, 0).width;
        float subtitleY = Gdx.graphics.getHeight() - 140 + (1f - headerProgress) * 30f;
        titleFont.draw(spriteBatch, subtitle, 
            (Gdx.graphics.getWidth() - subtitleWidth) / 2, subtitleY);
        
        // Информация об уровне с анимацией
        titleFont.getData().setScale(2f);
        titleFont.setColor(selectedColor.r, selectedColor.g, selectedColor.b, selectedColor.a * headerProgress);
        String levelInfo = "LEVEL " + progressionManager.getCurrentLevel();
        float levelWidth = titleFont.draw(spriteBatch, levelInfo, 0, 0).width;
        float levelY = Gdx.graphics.getHeight() - 180 + (1f - headerProgress) * 30f;
        titleFont.draw(spriteBatch, levelInfo, 
            (Gdx.graphics.getWidth() - levelWidth) / 2, levelY);
        
        // Рисуем текст карточек
        drawCardTexts();
        
        // Инструкции с анимацией
        float instructionsDelay = 0.4f;
        float instructionsProgress = isMenuAppearing ? Math.max(0f, (menuAppearTimer - instructionsDelay) / (0.8f - instructionsDelay)) : 1f;
        instructionsProgress = Math.min(instructionsProgress, 1f);
        
        titleFont.getData().setScale(1f);
        titleFont.setColor(0.6f, 0.6f, 0.6f, 0.8f * instructionsProgress);
        String instructions = "Use arrows to select, ENTER to confirm";
        float instWidth = titleFont.draw(spriteBatch, instructions, 0, 0).width;
        float instY = 80 + (1f - instructionsProgress) * 20f;
        titleFont.draw(spriteBatch, instructions, 
            (Gdx.graphics.getWidth() - instWidth) / 2, instY);
        
        spriteBatch.end();
    }
    
    private void drawBackgroundStars() {
        // Рисуем мерцающие звезды на фоне
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < 50; i++) {
            float x = (i * 37) % Gdx.graphics.getWidth();
            float y = (i * 73) % Gdx.graphics.getHeight();
            float alpha = 0.3f + 0.2f * MathUtils.sin(animationTimer * 2f + i);
            shapeRenderer.setColor(1f, 1f, 1f, alpha);
            shapeRenderer.circle(x, y, 1f);
        }
        shapeRenderer.end();
    }
    
    private void drawUpgradeCards() {
        float cardWidth = 300f;
        float cardHeight = 180f;
        float spacing = 50f;
        float startX = (Gdx.graphics.getWidth() - (cardWidth * 3 + spacing * 2)) / 2;
        float startY = Gdx.graphics.getHeight() * 0.35f;
        
        for (int i = 0; i < displayUpgrades.size(); i++) {
            Upgrade upgrade = displayUpgrades.get(i);
            float x = startX + i * (cardWidth + spacing);
            float y = startY;
            
            // Анимация появления карточки
            float appearProgress = 0f;
            float cardY = y;
            float cardPulse = 1f;
            float cardAlpha = 1f;
            
            if (isMenuAppearing) {
                appearProgress = Math.min(menuAppearTimer / 0.8f, 1f);
                // Задержка для каждой карточки
                float cardDelay = i * 0.15f;
                float cardProgress = Math.max(0f, (menuAppearTimer - cardDelay) / (0.8f - cardDelay));
                cardProgress = Math.min(cardProgress, 1f);
                
                // Более естественный эффект fade-in и легкий slide-up
                cardY = y + (1f - cardProgress) * 20f;
                cardAlpha = cardProgress;
                cardPulse = 0.9f + cardProgress * 0.1f;
            }
            
            if (i == selectedUpgrade && !isMenuAppearing) {
                // Легкая анимация выбранной карточки (только после появления)
                cardPulse = 1f + 0.02f * MathUtils.sin(animationTimer * 2f);
                cardY += 3f * MathUtils.sin(animationTimer * 1.5f);
            }
            
            // Рисуем карточку с анимацией появления
            drawCard(x, cardY, cardWidth * cardPulse, cardHeight * cardPulse, i == selectedUpgrade, cardAlpha);
        }
    }
    
    private void drawCard(float x, float y, float width, float height, boolean isSelected, float alpha) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        // Тень карточки
        shapeRenderer.setColor(0f, 0f, 0f, 0.3f * alpha);
        shapeRenderer.rect(x + 5, y - 5, width, height);
        
        // Фон карточки
        if (isSelected) {
            shapeRenderer.setColor(0.3f, 0.5f, 0.8f, 0.95f * alpha);
        } else {
            shapeRenderer.setColor(0.2f, 0.3f, 0.5f, 0.9f * alpha);
        }
        shapeRenderer.rect(x, y, width, height);
        
        // Рамка карточки
        if (isSelected) {
            shapeRenderer.setColor(1f, 1f, 0f, 1f * alpha); // Желтый
        } else {
            shapeRenderer.setColor(0.6f, 0.6f, 0.8f, 0.8f * alpha);
        }
        
        float borderWidth = isSelected ? 4f : 3f;
        shapeRenderer.rect(x, y, width, borderWidth);
        shapeRenderer.rect(x, y + height - borderWidth, width, borderWidth);
        shapeRenderer.rect(x, y, borderWidth, height);
        shapeRenderer.rect(x + width - borderWidth, y, borderWidth, height);
        
        shapeRenderer.end();
    }
    
    private void drawCardTexts() {
        float cardWidth = 300f;
        float cardHeight = 180f;
        float spacing = 50f;
        float startX = (Gdx.graphics.getWidth() - (cardWidth * 3 + spacing * 2)) / 2;
        float startY = Gdx.graphics.getHeight() * 0.40f;
        
        for (int i = 0; i < displayUpgrades.size(); i++) {
            Upgrade upgrade = displayUpgrades.get(i);
            float x = startX + i * (cardWidth + spacing);
            float y = startY;
            
            // Анимация появления карточки для текста
            float cardPulse = 1f;
            float cardY = y;
            float textAlpha = 1f;
            
            if (isMenuAppearing) {
                // Задержка для каждой карточки
                float cardDelay = i * 0.15f;
                float cardProgress = Math.max(0f, (menuAppearTimer - cardDelay) / (0.8f - cardDelay));
                cardProgress = Math.min(cardProgress, 1f);
                
                // Более естественный эффект fade-in и легкий slide-up
                cardY = y + (1f - cardProgress) * 20f;
                textAlpha = cardProgress;
                cardPulse = 0.9f + cardProgress * 0.1f;
            }
            
            if (i == selectedUpgrade && !isMenuAppearing) {
                // Легкая анимация выбранной карточки (только после появления)
                cardPulse = 1f + 0.02f * MathUtils.sin(animationTimer * 2f);
                cardY += 3f * MathUtils.sin(animationTimer * 1.5f);
            }
            
            // Название улучшения
            if (i == selectedUpgrade) {
                titleFont.setColor(selectedColor.r, selectedColor.g, selectedColor.b, selectedColor.a * textAlpha);
                titleFont.getData().setScale(2.2f);
            } else {
                titleFont.setColor(menuColor.r, menuColor.g, menuColor.b, menuColor.a * textAlpha);
                titleFont.getData().setScale(2f);
            }
            
            // Используем реальные названия улучшений
            String name = upgrade.getName();
            float nameWidth = titleFont.draw(spriteBatch, name, 0, 0).width;
            titleFont.draw(spriteBatch, name, 
                x + (cardWidth * cardPulse - nameWidth) / 2, cardY + cardHeight * cardPulse - 90);
            
            // Описание
            titleFont.getData().setScale(1.3f);
            if (i == selectedUpgrade) {
                titleFont.setColor(1f, 1f, 1f, 0.9f * textAlpha);
            } else {
                titleFont.setColor(0.7f, 0.7f, 0.9f, 0.8f * textAlpha);
            }
            
            String description = upgrade.getDescription();
            float descWidth = titleFont.draw(spriteBatch, description, 0, 0).width;
            titleFont.draw(spriteBatch, description, 
                x + (cardWidth * cardPulse - descWidth) / 2, cardY + cardHeight * cardPulse - 130);
        }
    }
    
    private void drawCardContent(float x, float y, float width, float height, Upgrade upgrade, boolean isSelected) {
        // Этот метод больше не используется, но оставляем для совместимости
    }
    
    private String[] wrapText(String text, float maxWidth, BitmapFont font) {
        // Простое разбиение текста на строки
        if (text.length() <= 30) {
            return new String[]{text};
        }
        
        // Находим место для разрыва
        int breakPoint = text.lastIndexOf(' ', 30);
        if (breakPoint == -1) breakPoint = 30;
        
        return new String[]{
            text.substring(0, breakPoint),
            text.substring(breakPoint + 1)
        };
    }
    
    private String getUpgradeIcon(String type) {
        switch (type) {
            case "SPEED": return "S";
            case "BULLET_SPEED": return "V";
            case "BULLET_DAMAGE": return "D";
            case "FIRE_RATE": return "F";
            case "MAX_HEALTH": return "H";
            case "HEALTH_REGEN": return "R";
            case "CRIT_CHANCE": return "C";
            case "CRIT_DAMAGE": return "X";
            case "ROCKET_COOLDOWN": return "T";
            case "ROCKET_DAMAGE": return "B";
            default: return "*";
        }
    }
    
    @Override
    public void dispose() {
        if (titleFont != null) titleFont.dispose();
        if (menuFont != null) menuFont.dispose();
        if (spriteBatch != null) spriteBatch.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
    }
}
