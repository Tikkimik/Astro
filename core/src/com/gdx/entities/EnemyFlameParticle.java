package com.gdx.entities;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.gdx.managers.Camera;

/**
 * Пламя двигателя вражеского корабля.
 *
 * Сознательно разрежено (спавн по таймеру, ~16 частиц/сек вместо 480),
 * чтобы не забивать глобальный бюджет частиц, но выглядит эффектно за счёт
 * крупного размера и аддитивного смешивания (свечение) — включается в
 * EnemyShip.draw перед отрисовкой пламени.
 */
public class EnemyFlameParticle extends Particle {

    private float life;
    private float maxLife;
    private float size;
    private float maxSize;
    private float speed;
    private float angle;

    public EnemyFlameParticle(float x, float y, float angle, float speed) {
        super(x, y);
        init(x, y, angle, speed);
    }

    // Метод инициализации для пула объектов
    public void init(float x, float y, float angle, float speed) {
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.speed = speed;
        this.maxLife = 0.7f + MathUtils.random() * 0.5f; // 0.7-1.2 секунды
        this.life = maxLife;
        this.maxSize = 10f + MathUtils.random() * 10f; // 10-20 пикселей
        this.size = maxSize;

        // Начальная скорость частицы
        this.dx = MathUtils.cos(angle) * speed;
        this.dy = MathUtils.sin(angle) * speed;
    }

    @Override
    public void update(float deltaTime) {
        x += dx * deltaTime;
        y += dy * deltaTime;

        // Уменьшаем жизнь частицы
        life -= deltaTime;

        // Уменьшаем размер по мере затухания
        size = maxSize * (life / maxLife);

        // Замедляем частицу (плавный вытянутый след)
        dx *= 0.94f;
        dy *= 0.94f;
    }

    @Override
    public void draw(ShapeRenderer shapeRenderer, Camera camera) {
        if (life <= 0) return;

        float lifeRatio = life / maxLife;

        float screenX = camera.worldToScreenX(x);
        float screenY = camera.worldToScreenY(y);

        // Масштабируем размер частицы в зависимости от зума камеры
        float scaledSize = size * camera.getCurrentZoom();

        // Аддитивное свечение: внешняя мягкая оболочка, пламя и яркое ядро
        shapeRenderer.setColor(0.9f, 0.3f, 0.05f, lifeRatio * 0.35f);
        shapeRenderer.circle(screenX, screenY, scaledSize);

        shapeRenderer.setColor(1.0f, 0.55f, 0.1f, lifeRatio * 0.6f);
        shapeRenderer.circle(screenX, screenY, scaledSize * 0.7f);

        shapeRenderer.setColor(1.0f, 0.9f, 0.4f, lifeRatio * 0.8f);
        shapeRenderer.circle(screenX, screenY, scaledSize * 0.4f);
    }

    @Override
    public boolean shouldRemove() {
        return life <= 0;
    }

    @Override
    public float getCullRadius() {
        return maxSize * 0.75f + 1f;
    }
}
