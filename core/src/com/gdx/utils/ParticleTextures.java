package com.gdx.utils;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;

public class ParticleTextures implements Disposable {

    private static ParticleTextures instance;
    private final ObjectMap<String, Texture> textureCache;

    // Размеры текстур частиц
    public static final int PARTICLE_SIZE = 64;
    public static final int FLAME_SIZE = 128;

    private ParticleTextures() {
        textureCache = new ObjectMap<>();

        // Генерируем базовые текстуры
        generateParticleTextures();
    }

    public static ParticleTextures getInstance() {
        if (instance == null) {
            instance = new ParticleTextures();
        }
        return instance;
    }

    public static void disposeInstance() {
        if (instance != null) {
            instance.dispose();
        }
    }

    private void generateParticleTextures() {
        // Генерируем простую частицу (белый круг)
        generateSimpleParticle();

        // Генерируем огненную частицу
        generateFlameParticle();


        // Генерируем взрывную частицу
        generateExplosionParticle();

        // Генерируем дымовую частицу
        generateSmokeParticle();
    }

    private void generateSimpleParticle() {
        Pixmap pixmap = new Pixmap(PARTICLE_SIZE, PARTICLE_SIZE, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0); // Прозрачный фон
        pixmap.fill();

        // Рисуем белый круг с градиентом
        int center = PARTICLE_SIZE / 2;
        int radius = PARTICLE_SIZE / 2 - 2;

        for (int x = 0; x < PARTICLE_SIZE; x++) {
            for (int y = 0; y < PARTICLE_SIZE; y++) {
                float distance = (float) Math.sqrt((x - center) * (x - center) + (y - center) * (y - center));
                if (distance <= radius) {
                    float alpha = 1.0f - (distance / radius);
                    alpha = (float) Math.pow(alpha, 2); // Более мягкие края
                    int color = Color.rgba8888(1.0f, 1.0f, 1.0f, alpha);
                    pixmap.drawPixel(x, y, color);
                }
            }
        }

        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        textureCache.put("simple", texture);
        pixmap.dispose();
    }

    private void generateFlameParticle() {
        // Генерируем огненную частицу как мягкий радиальный градиент:
        // голубое ядро → жёлтый → красно-оранжевый край.
        // Рисуется аддитивно, поэтому мягкие края дают эффект свечения.
        Pixmap pixmap = new Pixmap(FLAME_SIZE, FLAME_SIZE, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();

        int center = FLAME_SIZE / 2;
        int radius = FLAME_SIZE / 2 - 2;

        for (int x = 0; x < FLAME_SIZE; x++) {
            for (int y = 0; y < FLAME_SIZE; y++) {
                float distance = (float) Math.sqrt((x - center) * (x - center) + (y - center) * (y - center));
                if (distance <= radius) {
                    float t = 1.0f - (distance / radius); // 1 в центре, 0 на краю
                    float alpha = (float) Math.pow(t, 2.0f);

                    float r, g, b;
                    if (t > 0.7f) {
                        // Голубое ядро
                        float inner = (t - 0.7f) / 0.3f;
                        r = 0.8f + 0.2f * inner;
                        g = 1.0f;
                        b = 1.0f;
                    } else if (t > 0.35f) {
                        // Жёлтый переход
                        float mid = (t - 0.35f) / 0.35f;
                        r = 1.0f;
                        g = 0.75f + 0.25f * mid;
                        b = 0.1f * (1.0f - mid);
                    } else {
                        // Красно-оранжевый край
                        float outer = t / 0.35f;
                        r = 1.0f;
                        g = 0.25f + 0.5f * outer;
                        b = 0.05f * outer;
                    }

                    int color = Color.rgba8888(r, g, b, alpha);
                    pixmap.drawPixel(x, y, color);
                }
            }
        }

        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        textureCache.put("flame", texture);
        pixmap.dispose();
    }

    private void generateExplosionParticle() {
        Pixmap pixmap = new Pixmap(PARTICLE_SIZE, PARTICLE_SIZE, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();

        int center = PARTICLE_SIZE / 2;
        int radius = PARTICLE_SIZE / 2 - 2;

        for (int x = 0; x < PARTICLE_SIZE; x++) {
            for (int y = 0; y < PARTICLE_SIZE; y++) {
                float distance = (float) Math.sqrt((x - center) * (x - center) + (y - center) * (y - center));
                if (distance <= radius) {
                    float alpha = 1.0f - (distance / radius);
                    alpha = (float) Math.pow(alpha, 1.5f);

                    // Оранжево-красный цвет взрыва
                    float r = 1.0f;
                    float g = 0.3f + alpha * 0.4f;
                    float b = alpha * 0.2f;

                    int color = Color.rgba8888(r, g, b, alpha);
                    pixmap.drawPixel(x, y, color);
                }
            }
        }

        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        textureCache.put("explosion", texture);
        pixmap.dispose();
    }

    private void generateSmokeParticle() {
        Pixmap pixmap = new Pixmap(PARTICLE_SIZE, PARTICLE_SIZE, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();

        int center = PARTICLE_SIZE / 2;
        int radius = PARTICLE_SIZE / 2 - 2;

        for (int x = 0; x < PARTICLE_SIZE; x++) {
            for (int y = 0; y < PARTICLE_SIZE; y++) {
                float distance = (float) Math.sqrt((x - center) * (x - center) + (y - center) * (y - center));
                if (distance <= radius) {
                    float alpha = 1.0f - (distance / radius);
                    alpha = (float) Math.pow(alpha, 3.0f); // Очень мягкие края
                    alpha *= 0.6f; // Полупрозрачный дым

                    // Серый цвет дыма
                    float gray = 0.3f + alpha * 0.4f;
                    int color = Color.rgba8888(gray, gray, gray, alpha);
                    pixmap.drawPixel(x, y, color);
                }
            }
        }

        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        textureCache.put("smoke", texture);
        pixmap.dispose();
    }

    public Texture getParticleTexture(String type) {
        return textureCache.get(type, textureCache.get("simple"));
    }

    public void drawParticle(SpriteBatch batch, String type, float x, float y, float size, float alpha) {
        Texture texture = getParticleTexture(type);
        batch.setColor(1, 1, 1, alpha);
        batch.draw(texture, x - size/2, y - size/2, size, size);
        batch.setColor(Color.WHITE);
    }

    @Override
    public void dispose() {
        for (Texture texture : textureCache.values()) {
            texture.dispose();
        }
        textureCache.clear();
        instance = null;
    }
}
