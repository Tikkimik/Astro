package com.gdx.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;

public class ParticleTextures implements Disposable {
    
    private static ParticleTextures instance;
    private final ObjectMap<String, Texture> textureCache;
    private final ShaderProgram flameShader;
    private final FrameBuffer frameBuffer;
    
    // Размеры текстур частиц
    public static final int PARTICLE_SIZE = 64;
    public static final int FLAME_SIZE = 128;
    
    private ParticleTextures() {
        textureCache = new ObjectMap<>();
        frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, FLAME_SIZE, FLAME_SIZE, false);
        
        // Загружаем шейдер для огня
        String vertexShader = Gdx.files.internal("shaders/flame.vert").readString();
        String fragmentShader = Gdx.files.internal("shaders/flame.frag").readString();
        flameShader = new ShaderProgram(vertexShader, fragmentShader);
        
        if (!flameShader.isCompiled()) {
            System.err.println("Ошибка компиляции шейдера огня: " + flameShader.getLog());
        }
        
        // Генерируем базовые текстуры
        generateParticleTextures();
    }
    
    public static ParticleTextures getInstance() {
        if (instance == null) {
            instance = new ParticleTextures();
        }
        return instance;
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
        frameBuffer.begin();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        // Используем шейдер для генерации огня
        SpriteBatch batch = new SpriteBatch();
        batch.setShader(flameShader);
        
        batch.begin();
        flameShader.setUniformf("u_time", 0.5f); // Время для анимации
        flameShader.setUniformf("u_intensity", 1.0f);
        batch.draw(getWhiteTexture(), 0, 0, FLAME_SIZE, FLAME_SIZE);
        batch.end();
        
        batch.dispose();
        
        // Создаем текстуру из framebuffer
        Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0, FLAME_SIZE, FLAME_SIZE);
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        textureCache.put("flame", texture);
        
        frameBuffer.end();
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
    
    private Texture getWhiteTexture() {
        // Создаем простую белую текстуру для шейдера
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
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
        
        if (flameShader != null) {
            flameShader.dispose();
        }
        
        if (frameBuffer != null) {
            frameBuffer.dispose();
        }
    }
}
