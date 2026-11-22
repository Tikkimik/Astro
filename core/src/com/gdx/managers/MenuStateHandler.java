package com.gdx.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.gdx.entities.Particle;
import com.gdx.game.MyGdxGame;
import com.gdx.utils.Fonts;
import com.gdx.utils.GameLogger;

import java.util.ArrayList;

/** Главное меню с общей типографикой и адаптивной раскладкой интерфейса. */
public class MenuStateHandler implements ImprovedGameStateManager.StateHandler {

    private static final String[] MENU_ITEMS = {"ИГРАТЬ", "НАСТРОЙКИ", "ВЫХОД"};
    private static final float MENU_SPACING = 62f;
    private static final float CLICK_HORIZONTAL_PADDING = 32f;

    private boolean isInitialized;
    private SpriteBatch spriteBatch;
    private ShapeRenderer shapeRenderer;
    private final OrthographicCamera uiCamera = new OrthographicCamera();
    private final GlyphLayout layout = new GlyphLayout();
    private ArrayList<Particle> particles;

    private int selectedItem;
    private float titleY;
    private float titleTargetY;
    private float menuAnimationTimer;
    private float screenWidth;
    private float screenHeight;
    private float lastWidth = -1f;
    private float lastHeight = -1f;
    private int lastMouseX;
    private int lastMouseY;

    private final Color titleColor = new Color(1f, 1f, 1f, 1f);
    private final Color menuColor = new Color(0.8f, 0.8f, 1f, 1f);
    private final Color selectedColor = new Color(1f, 1f, 0.35f, 1f);

    private ImprovedGameStateManager stateManager;

    @Override
    public void onEnter() {
        GameLogger.info("=== ВХОД В ГЛАВНОЕ МЕНЮ ===");
        if (!isInitialized) {
            initializeMenu();
        }
        selectedItem = 0;
        lastMouseX = Gdx.input.getX();
        lastMouseY = Gdx.input.getY();
        GameLogger.info("Главное меню активировано");
    }

    @Override
    public void onUpdate(float deltaTime) {
        if (!isInitialized) {
            return;
        }

        // Экспоненциальное приближение сохраняет скорость анимации при любом FPS.
        float titleLerp = 1f - (float) Math.exp(-6f * deltaTime);
        titleY += (titleTargetY - titleY) * titleLerp;
        menuAnimationTimer += deltaTime * 2f;

        for (Particle particle : particles) {
            particle.update(deltaTime);
        }
        particles.removeIf(Particle::shouldRemove);
        if (MathUtils.random() < 0.1f * deltaTime * 60f) {
            createRandomParticle();
        }
    }

    @Override
    public void onRender() {
        if (!isInitialized) {
            return;
        }
        ensureProjection();

        Gdx.gl.glClearColor(0.1f, 0.1f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        drawParticles();

        BitmapFont titleFont = Fonts.getTitleFont();
        BitmapFont headingFont = Fonts.getHeadingFont();
        BitmapFont bodyFont = Fonts.getBodyFont();

        spriteBatch.begin();
        titleFont.setColor(titleColor);
        drawCentered(titleFont, "ASTRO", titleY);

        for (int i = 0; i < MENU_ITEMS.length; i++) {
            float baseline = getMenuBaseline(i);
            if (i == selectedItem) {
                float pulse = 0.85f + 0.15f * MathUtils.sin(menuAnimationTimer * 5f);
                headingFont.setColor(selectedColor.r, selectedColor.g, selectedColor.b, pulse);
            } else {
                headingFont.setColor(menuColor);
            }
            drawCentered(headingFont, MENU_ITEMS[i], baseline);
        }

        bodyFont.setColor(1f, 1f, 0.25f, 0.9f);
        drawCentered(bodyFont, "Рекорд: " + MyGdxGame.highScore + " очков", screenHeight * 0.23f);

        bodyFont.setColor(0.65f, 0.65f, 0.72f, 0.9f);
        drawCentered(bodyFont, "↑/↓ или W/S — выбрать · Enter/клик — подтвердить", screenHeight * 0.15f);
        drawCentered(bodyFont, "В игре: Esc/P — пауза · Space — огонь", screenHeight * 0.10f);
        spriteBatch.end();
    }

    @Override
    public void onHandleInput() {
        if (!isInitialized) {
            return;
        }

        if (GameKeys.isPressed(GameKeys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            selectedItem = (selectedItem - 1 + MENU_ITEMS.length) % MENU_ITEMS.length;
        }
        if (GameKeys.isPressed(GameKeys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            selectedItem = (selectedItem + 1) % MENU_ITEMS.length;
        }
        if (GameKeys.isPressed(GameKeys.ENTER)) {
            selectMenuItem();
            return;
        }

        int rawMouseX = Gdx.input.getX();
        int rawMouseY = Gdx.input.getY();
        int hoveredItem = findMenuItem(rawMouseX, Gdx.graphics.getHeight() - rawMouseY);
        if ((rawMouseX != lastMouseX || rawMouseY != lastMouseY) && hoveredItem >= 0) {
            selectedItem = hoveredItem;
        }
        if (Gdx.input.justTouched() && hoveredItem >= 0) {
            selectedItem = hoveredItem;
            selectMenuItem();
        }
        lastMouseX = rawMouseX;
        lastMouseY = rawMouseY;
    }

    @Override
    public void onExit() {
        GameLogger.info("=== ВЫХОД ИЗ ГЛАВНОГО МЕНЮ ===");
        GameLogger.info("Главное меню деактивировано");
    }

    @Override
    public void onDispose() {
        if (spriteBatch != null) {
            spriteBatch.dispose();
            spriteBatch = null;
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
            shapeRenderer = null;
        }
        if (particles != null) {
            particles.clear();
            particles = null;
        }
        isInitialized = false;
    }

    private void initializeMenu() {
        spriteBatch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        particles = new ArrayList<>();
        ensureProjection();
        titleY = screenHeight + 100f;
        for (int i = 0; i < 50; i++) {
            createRandomParticle();
        }
        isInitialized = true;
        GameLogger.info("Главное меню инициализировано");
    }

    private void ensureProjection() {
        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();
        if (width == lastWidth && height == lastHeight) {
            return;
        }

        float oldTargetY = titleTargetY;
        screenWidth = width;
        screenHeight = height;
        titleTargetY = screenHeight * 0.80f;
        if (lastHeight > 0f) {
            titleY += titleTargetY - oldTargetY;
        }

        uiCamera.setToOrtho(false, width, height);
        uiCamera.update();
        spriteBatch.setProjectionMatrix(uiCamera.combined);
        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        lastWidth = width;
        lastHeight = height;
    }

    private float getMenuBaseline(int index) {
        return screenHeight * 0.53f - index * MENU_SPACING;
    }

    private int findMenuItem(float x, float y) {
        BitmapFont font = Fonts.getHeadingFont();
        float halfHeight = font.getLineHeight() * 0.75f;
        for (int i = 0; i < MENU_ITEMS.length; i++) {
            float width = measure(font, MENU_ITEMS[i]);
            float left = (screenWidth - width) / 2f - CLICK_HORIZONTAL_PADDING;
            float right = (screenWidth + width) / 2f + CLICK_HORIZONTAL_PADDING;
            float baseline = getMenuBaseline(i);
            if (x >= left && x <= right && y >= baseline - halfHeight && y <= baseline + halfHeight) {
                return i;
            }
        }
        return -1;
    }

    private void selectMenuItem() {
        if (selectedItem == 0 && stateManager != null) {
            stateManager.setState(ImprovedGameStateManager.GameState.PLAYING);
        } else if (selectedItem == 1 && stateManager != null) {
            stateManager.setState(ImprovedGameStateManager.GameState.SETTINGS);
        } else if (selectedItem == 2) {
            Gdx.app.exit();
        }
    }

    private void createRandomParticle() {
        particles.add(new Particle(MathUtils.random(screenWidth), MathUtils.random(screenHeight)));
    }

    private void drawParticles() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.5f, 0.7f, 1f, 0.3f);
        for (Particle particle : particles) {
            shapeRenderer.circle(particle.getX(), particle.getY(), 2f);
        }
        shapeRenderer.end();
    }

    private void drawCentered(BitmapFont font, String text, float baseline) {
        font.draw(spriteBatch, text, (screenWidth - measure(font, text)) / 2f, baseline);
    }

    private float measure(BitmapFont font, String text) {
        layout.setText(font, text);
        return layout.width;
    }

    public void setStateManager(ImprovedGameStateManager stateManager) {
        this.stateManager = stateManager;
    }
}
