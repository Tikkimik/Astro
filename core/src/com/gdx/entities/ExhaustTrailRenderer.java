package com.gdx.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.gdx.managers.Camera;

/**
 * Атмосферный дым за ракетным факелом. В отличие от самого огня он рисуется
 * обычной прозрачностью, хранится в мировых координатах и продолжает жить
 * после выключения тяги.
 */
public class ExhaustTrailRenderer {

    private static final int MAX_SAMPLES = 80;
    private static final float LIFE = 1.25f;
    private static final float EXHAUST_SPEED = 135f;
    private static final float EMISSION_INTERVAL = 0.018f;

    private final Array<Sample> smoke = new Array<>(MAX_SAMPLES);
    private final Matrix4 projection = new Matrix4();
    private final SpriteBatch batch = new SpriteBatch();
    private final Texture cloudTexture;
    private float emissionTimer = EMISSION_INTERVAL;
    private int sampleIndex;

    public ExhaustTrailRenderer() {
        cloudTexture = createCloudTexture();
    }

    public void update(float dt, boolean emitting,
            float leftX, float leftY, float rightX, float rightY,
            float shipVx, float shipVy, float angle) {
        for (int i = smoke.size - 1; i >= 0; i--) {
            Sample sample = smoke.get(i);
            sample.age += dt;
            sample.x += sample.vx * dt;
            sample.y += sample.vy * dt;
            if (sample.age >= LIFE) smoke.removeIndex(i);
        }

        if (!emitting) {
            emissionTimer = EMISSION_INTERVAL;
            return;
        }

        emissionTimer += dt;
        if (emissionTimer < EMISSION_INTERVAL) return;
        emissionTimer -= EMISSION_INTERVAL;

        float forwardX = MathUtils.cos(angle);
        float forwardY = MathUtils.sin(angle);
        float normalX = -forwardY;
        float normalY = forwardX;
        float centerX = (leftX + rightX) * 0.5f;
        float centerY = (leftY + rightY) * 0.5f;

        if (smoke.size == MAX_SAMPLES) smoke.removeIndex(0);
        // Центр рождения всегда один и тот же. Небольшая волна нужна только
        // для живого края облака, а не для разбрасывания дыма вокруг сопла.
        float sizeVariation = 0.94f + 0.08f * MathUtils.sin(sampleIndex * 1.73f);
        float opacityVariation = 0.88f + 0.12f * MathUtils.sin(sampleIndex * 2.31f + 0.7f);
        float drift = MathUtils.sin(sampleIndex * 0.47f) * 1.4f;
        smoke.add(new Sample(centerX, centerY,
                shipVx - forwardX * EXHAUST_SPEED + normalX * drift,
                shipVy - forwardY * EXHAUST_SPEED + normalY * drift,
                sizeVariation, opacityVariation));
        sampleIndex++;
    }

    public void render(Camera camera) {
        if (smoke.size == 0) return;

        projection.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.setProjectionMatrix(projection);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.begin();
        drawSmokeLayer(camera, true);
        drawSmokeLayer(camera, false);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        drawHotTransition(camera);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    /** Горячий край дыма закрывает шов между аддитивным огнём и серым облаком. */
    private void drawHotTransition(Camera camera) {
        float zoom = camera.getCurrentZoom();
        for (int i = 0; i < smoke.size; i++) {
            Sample sample = smoke.get(i);
            float reveal = smoothStep(0.02f, 0.10f, sample.age);
            float hot = 1f - smoothStep(0.30f, 0.58f, sample.age);
            float density = reveal * hot * sample.opacity;
            if (density < 0.003f) continue;

            float t = MathUtils.clamp(sample.age / 0.58f, 0f, 1f);
            float size = MathUtils.lerp(8f, 17f, t) * zoom * sample.size;
            float sx = camera.worldToScreenX(sample.x);
            float sy = camera.worldToScreenY(sample.y);
            batch.setColor(1f, MathUtils.lerp(0.42f, 0.22f, t), 0.035f,
                    density * 0.22f);
            batch.draw(cloudTexture, sx - size * 0.5f, sy - size * 0.5f, size, size);
        }
    }

    private void drawSmokeLayer(Camera camera, boolean halo) {
        float zoom = camera.getCurrentZoom();
        for (int i = 0; i < smoke.size; i++) {
            Sample sample = smoke.get(i);
            float age = MathUtils.clamp(sample.age / LIFE, 0f, 1f);

            // Дым становится заметен только ближе к концу яркого факела.
            // Потом он сильно расширяется и медленно растворяется.
            float reveal = smoothStep(0.02f, 0.10f, sample.age);
            float fade = 1f - smoothStep(0.82f, LIFE, sample.age);
            float density = reveal * fade * sample.opacity;
            if (density < 0.003f) continue;

            float baseSize = MathUtils.lerp(10f, 27f,
                    (float) Math.pow(age, 0.70f)) * zoom * sample.size;
            // При текущей скорости возраст частицы у конца факела около
            // 0.4 секунды: там она ещё янтарная и только затем сереет.
            float warmth = 1f - smoothStep(0.25f, 0.75f, sample.age);
            float r = MathUtils.lerp(0.78f, 0.98f, warmth);
            float g = MathUtils.lerp(0.82f, 0.62f, warmth);
            float b = MathUtils.lerp(0.88f, 0.38f, warmth);

            float size;
            float alpha;
            if (halo) {
                size = baseSize * 1.32f;
                alpha = density * MathUtils.lerp(0.055f, 0.075f, warmth);
            } else {
                size = baseSize;
                alpha = density * MathUtils.lerp(0.16f, 0.22f, warmth);
            }

            float sx = camera.worldToScreenX(sample.x);
            float sy = camera.worldToScreenY(sample.y);
            batch.setColor(r, g, b, alpha);
            batch.draw(cloudTexture, sx - size * 0.5f, sy - size * 0.5f, size, size);
        }
    }

    private Texture createCloudTexture() {
        final int size = 96;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        float center = (size - 1) * 0.5f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float nx = (x - center) / center;
                float ny = (y - center) / center;
                float radius = (float) Math.sqrt(nx * nx + ny * ny);
                float angle = MathUtils.atan2(ny, nx);
                float edge = 1f + 0.07f * MathUtils.sin(angle * 3f + 0.4f)
                        + 0.04f * MathUtils.sin(angle * 7f - 1.1f);
                float density = MathUtils.clamp(1f - radius / edge, 0f, 1f);
                float alpha = density * density * (3f - 2f * density);
                pixmap.drawPixel(x, y, Color.rgba8888(1f, 1f, 1f, alpha));
            }
        }
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }

    private float smoothStep(float edge0, float edge1, float value) {
        float t = MathUtils.clamp((value - edge0) / (edge1 - edge0), 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    public void dispose() {
        batch.dispose();
        cloudTexture.dispose();
        smoke.clear();
    }

    public int getSampleCount() {
        return smoke.size;
    }

    private static final class Sample {
        float x;
        float y;
        final float vx;
        final float vy;
        final float size;
        final float opacity;
        float age;

        Sample(float x, float y, float vx, float vy, float size, float opacity) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.opacity = opacity;
        }
    }
}
