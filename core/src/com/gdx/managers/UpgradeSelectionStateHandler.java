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
import com.gdx.managers.ProgressionManager.Upgrade;
import com.gdx.utils.Fonts;

import java.util.List;

public final class UpgradeSelectionStateHandler implements ImprovedGameStateManager.StateHandler {
    public interface SessionAccess {
        ProgressionManager progression();
        void selectionCommitted();
    }

    private static final float CARD_GAP = 24f;
    private static final float CARD_HEIGHT = 250f;
    private static final float SIDE_MARGIN = 42f;

    private final SessionAccess sessionAccess;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final GlyphLayout layout = new GlyphLayout();
    private ImprovedGameStateManager stateManager;
    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private List<Upgrade> offer = List.of();
    private int selectedIndex;
    private float width;
    private float height;

    public UpgradeSelectionStateHandler(SessionAccess sessionAccess) {
        this.sessionAccess = sessionAccess;
    }

    @Override
    public void onEnter() {
        if (batch == null) {
            batch = new SpriteBatch();
            shapes = new ShapeRenderer();
        }
        refreshLayout();
        prepareOffer();
    }

    @Override
    public void onUpdate(float deltaTime) {
        if (Gdx.graphics.getWidth() != width || Gdx.graphics.getHeight() != height) {
            refreshLayout();
        }
        int hovered = findCard(Gdx.input.getX(), height - Gdx.input.getY());
        if (hovered >= 0) selectedIndex = hovered;
    }

    @Override
    public void onRender() {
        if (batch == null || offer.isEmpty()) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.015f, 0.02f, 0.07f, 0.82f);
        shapes.rect(0f, 0f, width, height);

        float cardWidth = cardWidth();
        float cardY = cardY();
        for (int i = 0; i < offer.size(); i++) {
            float x = cardX(i, cardWidth);
            boolean selected = i == selectedIndex;
            shapes.setColor(selected
                    ? new Color(0.12f, 0.25f, 0.38f, 0.98f)
                    : new Color(0.055f, 0.08f, 0.14f, 0.96f));
            shapes.rect(x, cardY, cardWidth, CARD_HEIGHT);
            shapes.setColor(selected ? 0.35f : 0.18f, selected ? 0.9f : 0.38f,
                    selected ? 1f : 0.55f, 1f);
            float border = selected ? 4f : 2f;
            shapes.rect(x, cardY + CARD_HEIGHT - border, cardWidth, border);
            shapes.rect(x, cardY, cardWidth, border);
            shapes.rect(x, cardY, border, CARD_HEIGHT);
            shapes.rect(x + cardWidth - border, cardY, border, CARD_HEIGHT);
        }
        shapes.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        BitmapFont title = Fonts.getTitleFont();
        BitmapFont heading = Fonts.getHeadingFont();
        BitmapFont body = Fonts.getBodyFont();
        title.setColor(0.55f, 0.92f, 1f, 1f);
        drawCentered(title, "ВЫБЕРИТЕ УЛУЧШЕНИЕ", height - 76f);

        body.setColor(0.78f, 0.83f, 0.9f, 1f);
        drawCentered(body, "Осталось выборов: "
                + sessionAccess.progression().getPendingUpgradeChoices(), height - 116f);

        for (int i = 0; i < offer.size(); i++) {
            Upgrade upgrade = offer.get(i);
            float x = cardX(i, cardWidth);
            float center = x + cardWidth / 2f;
            body.setColor(0.45f, 0.85f, 1f, 1f);
            drawCenteredAt(body, Integer.toString(i + 1), center, cardY + CARD_HEIGHT - 28f);
            heading.setColor(i == selectedIndex ? Color.WHITE : new Color(0.82f, 0.88f, 0.95f, 1f));
            drawCenteredAt(heading, upgrade.getName(), center, cardY + CARD_HEIGHT - 82f);
            body.setColor(0.72f, 0.77f, 0.84f, 1f);
            drawCenteredAt(body, upgrade.getDescription(), center, cardY + 92f);
        }

        body.setColor(0.62f, 0.7f, 0.78f, 1f);
        drawCentered(body, "← → выбрать   •   Enter подтвердить   •   1–3 быстрый выбор", 46f);
        batch.end();
    }

    @Override
    public void onHandleInput() {
        if (offer.isEmpty()) return;
        if (GameKeys.isPressed(GameKeys.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            selectedIndex = (selectedIndex - 1 + offer.size()) % offer.size();
        }
        if (GameKeys.isPressed(GameKeys.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            selectedIndex = (selectedIndex + 1) % offer.size();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) select(0);
        else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) select(1);
        else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) select(2);
        else if (GameKeys.isPressed(GameKeys.ENTER)) select(selectedIndex);
        else if (Gdx.input.justTouched()) {
            int clicked = findCard(Gdx.input.getX(), height - Gdx.input.getY());
            if (clicked >= 0) select(clicked);
        }
    }

    @Override public void onExit() {}

    @Override
    public void onDispose() {
        if (batch != null) batch.dispose();
        if (shapes != null) shapes.dispose();
        batch = null;
        shapes = null;
    }

    public void setStateManager(ImprovedGameStateManager stateManager) {
        this.stateManager = stateManager;
    }

    private void select(int index) {
        if (index < 0 || index >= offer.size()) return;
        if (!sessionAccess.progression().selectUpgrade(offer.get(index).getType())) return;
        sessionAccess.selectionCommitted();
        if (sessionAccess.progression().hasPendingUpgradeChoice()) {
            prepareOffer();
        } else if (stateManager != null) {
            stateManager.setState(ImprovedGameStateManager.GameState.PLAYING);
        }
    }

    private void prepareOffer() {
        if (!sessionAccess.progression().hasPendingUpgradeChoice()) {
            offer = List.of();
            if (stateManager != null) stateManager.setState(ImprovedGameStateManager.GameState.PLAYING);
            return;
        }
        offer = sessionAccess.progression().createUpgradeOffer(3);
        selectedIndex = 0;
    }

    private void refreshLayout() {
        width = Gdx.graphics.getWidth();
        height = Gdx.graphics.getHeight();
        camera.setToOrtho(false, width, height);
        camera.update();
    }

    private float cardWidth() {
        return Math.min(310f, (width - SIDE_MARGIN * 2f - CARD_GAP * 2f) / 3f);
    }

    private float cardY() {
        return Math.max(88f, (height - CARD_HEIGHT) * 0.46f);
    }

    private float cardX(int index, float cardWidth) {
        float total = cardWidth * offer.size() + CARD_GAP * (offer.size() - 1);
        return (width - total) / 2f + index * (cardWidth + CARD_GAP);
    }

    private int findCard(float x, float y) {
        float cardWidth = cardWidth();
        float cardY = cardY();
        for (int i = 0; i < offer.size(); i++) {
            float cardX = cardX(i, cardWidth);
            if (x >= cardX && x <= cardX + cardWidth && y >= cardY && y <= cardY + CARD_HEIGHT) {
                return i;
            }
        }
        return -1;
    }

    private void drawCentered(BitmapFont font, String text, float baseline) {
        drawCenteredAt(font, text, width / 2f, baseline);
    }

    private void drawCenteredAt(BitmapFont font, String text, float center, float baseline) {
        layout.setText(font, text);
        font.draw(batch, text, center - layout.width / 2f, baseline);
    }
}
