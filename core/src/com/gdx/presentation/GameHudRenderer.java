package com.gdx.presentation;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

public final class GameHudRenderer {

    private final BitmapFont font;
    private float experienceBarTimer;
    private float lastExperienceProgress;
    private float animatedProgress;
    private boolean levelingUp;
    private float levelUpTimer;
    private float bottomY;

    public GameHudRenderer(BitmapFont font) {
        this.font = font;
    }

    public void update(float delta, GameHudView view) {
        experienceBarTimer += delta;
        float currentProgress = view.experienceProgress();
        if (currentProgress < lastExperienceProgress) {
            levelingUp = true;
            levelUpTimer = 0f;
        }
        if (levelingUp) {
            levelUpTimer += delta;
            if (levelUpTimer >= 1f) {
                levelingUp = false;
                animatedProgress = 0f;
            }
        }
        if (!levelingUp) {
            float difference = currentProgress - animatedProgress;
            if (Math.abs(difference) > 0.001f) animatedProgress += difference * delta * 5f;
        }
        lastExperienceProgress = currentProgress;
    }

    public void draw(SpriteBatch batch, ShapeRenderer shapes, GameHudView view) {
        drawText(batch, view);
        drawExperienceBar(shapes, view.viewportHeight());
    }

    private void drawText(SpriteBatch batch, GameHudView view) {
        float x = 10f;
        float lineHeight = font.getLineHeight();
        float spacing = 4f;
        float y = view.viewportHeight() - 8f;

        batch.begin();
        font.setColor(1f, 1f, 1f, 1f);
        font.draw(batch, "Score: " + view.score(), x, y); y -= lineHeight + spacing;
        font.draw(batch, "High: " + view.highScore(), x, y); y -= lineHeight + spacing;
        font.setColor(0f, 1f, 1f, 1f);
        font.draw(batch, "Level: " + view.level(), x, y); y -= lineHeight + spacing;
        font.setColor(1f, 1f, 0f, 1f);
        font.draw(batch, "XP: " + view.experience() + "/" + view.experienceToNextLevel(), x, y);
        batch.end();

        bottomY = y - lineHeight;
    }

    private void drawExperienceBar(ShapeRenderer shapes, int viewportHeight) {
        float width = 200f;
        float height = 12f;
        float x = 10f;
        float y = viewportHeight - 95f;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.5f);
        shapes.rect(x + 2, y - 2, width, height);
        shapes.setColor(0.2f, 0.2f, 0.3f, 1f);
        shapes.rect(x, y, width, height);
        shapes.setColor(0.1f, 0.1f, 0.2f, 1f);
        shapes.rect(x + 1, y + 1, width - 2, height - 2);

        if (levelingUp) {
            float pulse = 0.5f + 0.5f * MathUtils.sin(levelUpTimer * 10f);
            shapes.setColor(1f, 1f, 0f, pulse);
            shapes.rect(x + 2, y + 2, width - 4, height - 4);
            shapes.setColor(1f, 0.8f, 0f, pulse * 0.5f);
            shapes.rect(x + 2, y + 2, width - 4, height - 4);
        } else {
            float fillWidth = (width - 4) * animatedProgress;
            for (int i = 0; i < fillWidth; i += 2) {
                float segmentProgress = i / fillWidth;
                float alpha = 0.8f + 0.2f * MathUtils.sin(experienceBarTimer * 3f + i * 0.1f);
                shapes.setColor(1f, 0.5f + 0.5f * segmentProgress, 0f, alpha);
                shapes.rect(x + 2 + i, y + 2, 2, height - 4);
            }
            if (animatedProgress > 0f) {
                float glow = 0.3f + 0.2f * MathUtils.sin(experienceBarTimer * 5f);
                shapes.setColor(1f, 1f, 0f, glow);
                shapes.rect(x + 2 + fillWidth - 4, y + 2, 4, height - 4);
            }
        }

        shapes.setColor(0.6f, 0.6f, 0.8f, 1f);
        shapes.rect(x, y, width, 2);
        shapes.rect(x, y + height - 2, width, 2);
        shapes.rect(x, y, 2, height);
        shapes.rect(x + width - 2, y, 2, height);
        if (animatedProgress > 0.9f) {
            float readyPulse = 0.2f + 0.3f * MathUtils.sin(experienceBarTimer * 8f);
            shapes.setColor(1f, 0f, 0f, readyPulse);
            shapes.rect(x + 2, y + 2, width - 4, 2);
        }
        shapes.end();
    }

    public float getBottomY() {
        return bottomY;
    }
}
