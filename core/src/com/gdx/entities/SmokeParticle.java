package com.gdx.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.gdx.managers.Camera;
import com.gdx.utils.ParticleTextures;

/**
 * Дымовой клуб из выхлопа вражеского корабля.
 *
 * Второй слой следа за пламенем: растёт по мере рассеивания (в отличие от
 * обычных частиц, которые уменьшаются), полупрозрачный, мягкая текстура.
 * Рисуется через SpriteBatch с обычным смешиванием (не аддитивным).
 */
public class SmokeParticle extends Particle {

    private float life;
    private float maxLife;
    private float size;
    private float maxSize;
    private float speed;
    private float angle;

    public SmokeParticle(float x, float y, float angle, float speed) {
        super(x, y);
        init(x, y, angle, speed);
    }

    // Метод инициализации для пула объектов
    public void init(float x, float y, float angle, float speed) {
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.speed = speed;
        this.maxLife = 1.0f + MathUtils.random() * 0.6f; // 1.0-1.6 секунды
        this.life = maxLife;
        this.maxSize = 14f + MathUtils.random() * 8f; // 14-22 пикселя
        this.size = maxSize * 0.3f; // начинаем маленьким

        // Начальная скорость (медленный разлёт)
        this.dx = MathUtils.cos(angle) * speed;
        this.dy = MathUtils.sin(angle) * speed;
    }

    @Override
    public void update(float deltaTime) {
        x += dx * deltaTime;
        y += dy * deltaTime;

        life -= deltaTime;

        // Дым растёт по мере рассеивания
        float growth = 1.0f - (life / maxLife);
        size = maxSize * (0.3f + growth * 0.9f);

        // Лёгкая турбулентность
        dx += (MathUtils.random() - 0.5f) * 20f * deltaTime;
        dy += (MathUtils.random() - 0.5f) * 20f * deltaTime;
        dx *= 0.98f;
        dy *= 0.98f;
    }

    public void draw(SpriteBatch batch, Camera camera) {
        if (life <= 0) return;

        float lifeRatio = life / maxLife;
        float alpha = lifeRatio * lifeRatio * 0.55f; // полупрозрачный, мягко исчезает

        float screenX = camera.worldToScreenX(x);
        float screenY = camera.worldToScreenY(y);
        float scaledSize = size * camera.getCurrentZoom();

        Texture texture = ParticleTextures.getInstance().getParticleTexture("smoke");
        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(texture, screenX - scaledSize / 2f, screenY - scaledSize / 2f, scaledSize, scaledSize);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    @Override
    public void draw(ShapeRenderer shapeRenderer, Camera camera) {
        // Дым рисуется только через SpriteBatch
    }

    @Override
    public boolean shouldRemove() {
        return life <= 0;
    }

    @Override
    public float getCullRadius() {
        return maxSize * 0.8f + 1f;
    }
}
