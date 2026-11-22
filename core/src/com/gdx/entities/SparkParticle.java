package com.gdx.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.gdx.managers.Camera;
import com.gdx.utils.ParticleTextures;

/**
 * Редкая искра из сопла двигателя корабля.
 *
 * Мелкая, короткоживущая, разлетается шире основной струи (±15°) и очень
 * быстро теряет яркость. Рисуется аддитивно той же мягкой текстурой, что и
 * пламя, чтобы сочетаться с остальным эффектом. Появляется нерегулярно и
 * не формирует непрерывную линию.
 */
public class SparkParticle extends Particle {

    private float life;
    private float maxLife;
    private float size;
    private float maxSize;
    private float speed;
    private float angle;

    public SparkParticle(float x, float y, float angle, float speed) {
        super(x, y);
        init(x, y, angle, speed);
    }

    // Метод инициализации для пула объектов
    public void init(float x, float y, float angle, float speed) {
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.speed = speed;
        this.maxLife = 0.20f + MathUtils.random() * 0.2f;  // 0.20-0.40 секунды
        this.life = maxLife;
        this.maxSize = 1f + MathUtils.random() * 1f;       // 1-2 ед.
        this.size = maxSize;

        this.dx = MathUtils.cos(angle) * speed;
        this.dy = MathUtils.sin(angle) * speed;
    }

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);

        life -= deltaTime;

        // Искры летят по инерции, чуть замедляясь
        dx *= 0.97f;
        dy *= 0.97f;
    }

    public void draw(SpriteBatch batch, Camera camera) {
        if (life <= 0) return;

        float lifeRatio = MathUtils.clamp(life / maxLife, 0f, 1f);

        // Быстрая потеря яркости: стартовая альфа ~0.4 (тусклее струи), резко падает
        float alpha = 0.4f * lifeRatio * lifeRatio;

        // Бело-жёлтая искра, тускнеет к оранжевой. Яркость ниже, чем у струи.
        float r = 1f;
        float g = 0.55f + 0.45f * lifeRatio;
        float b = 0.1f * lifeRatio;

        float screenX = camera.worldToScreenX(x);
        float screenY = camera.worldToScreenY(y);
        float scaledSize = size * camera.getCurrentZoom();

        Texture texture = ParticleTextures.getInstance().getParticleTexture("flame");
        batch.setColor(r, g, b, alpha);
        batch.draw(texture, screenX - scaledSize / 2f, screenY - scaledSize / 2f, scaledSize, scaledSize);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    @Override
    public void draw(ShapeRenderer shapeRenderer, Camera camera) {
        // Искры рисуются только через SpriteBatch
    }

    @Override
    public boolean shouldRemove() {
        return life <= 0;
    }

    @Override
    public float getCullRadius() {
        return maxSize + 1f;
    }
}
