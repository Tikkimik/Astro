package com.gdx.gamestates;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.gdx.managers.GameKeys;
import com.gdx.managers.GameStateManager;
import com.gdx.entities.Particle;
import com.gdx.game.MyGdxGame;

import java.util.ArrayList;

public class MenuState extends GameState {

    private SpriteBatch spriteBatch;
    private BitmapFont titleFont;
    private BitmapFont menuFont;
    private ShapeRenderer shapeRenderer;
    
    private ArrayList<Particle> particles;
    
    private String[] menuItems = {"PLAY", "SETTINGS", "EXIT"};
    private int lastHighScore = 0;
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

    public MenuState(GameStateManager gameStateManager) {
        super(gameStateManager);
    }

    @Override
    public void init() {
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
    }

    @Override
    public void update(float dt) {
        handleInput();
        
        // Анимация заголовка
        titleY += (titleTargetY - titleY) * titleAnimationSpeed;
        
        // Анимация меню
        menuAnimationTimer += dt * menuAnimationSpeed;
        
        // Обновляем частицы
        updateParticles(dt);
        
        // Удаляем мертвые частицы
        particles.removeIf(particle -> particle.shouldRemove());
        
        // Добавляем новые частицы
        if (MathUtils.random() < 0.1f) {
            createRandomParticle();
        }
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
            (screenWidth - hint2Width) / 2, screenHeight * 0.1f);
        
        spriteBatch.end();
    }

    @Override
    public void handleInput() {
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

    private void selectMenuItem() {
        switch (selectedItem) {
            case 0: // PLAY
                gameStateManager.setState(GameStateManager.PLAY);
                break;
            case 1: // SETTINGS
                gameStateManager.setState(GameStateManager.SETTINGS);
                break;
            case 2: // EXIT
                Gdx.app.exit();
                break;
        }
    }

    private void createBackgroundParticles() {
        for (int i = 0; i < 50; i++) {
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
