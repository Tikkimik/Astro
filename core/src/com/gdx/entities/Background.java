package com.gdx.entities;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.gdx.game.MyGdxGame;
import com.gdx.managers.Camera;

import java.util.ArrayList;

public class Background {
    
    private ArrayList<Star> stars;
    private final int NUM_STARS = 800; // Больше звезд для большего мира
    private Camera camera;
    
    public Background(Camera camera) {
        this.camera = camera;
        stars = new ArrayList<>();
        generateStars();
    }
    
    private void generateStars() {
        for (int i = 0; i < NUM_STARS; i++) {
            float x = MathUtils.random(camera.getWorldWidth());
            float y = MathUtils.random(camera.getWorldHeight());
            float size = MathUtils.random(0.5f, 2.0f);
            float brightness = MathUtils.random(0.3f, 1.0f);
            stars.add(new Star(x, y, size, brightness));
        }
    }
    
    public void update(float dt) {
        // Звезды могут мерцать или двигаться
        for (Star star : stars) {
            star.update(dt);
        }
    }
    
    public void draw(ShapeRenderer shapeRenderer) {
        shapeRenderer.setColor(0, 0, 0, 1); // Черный фон
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.rect(0, 0, MyGdxGame.WIDTH, MyGdxGame.HEIGHT);
        shapeRenderer.end();
        
        // Рисуем только видимые звезды
        for (Star star : stars) {
            if (camera.isInView(star.x, star.y, star.size)) {
                star.draw(shapeRenderer, camera);
            }
        }
    }
    
    private static class Star {
        private float x, y;
        private float size;
        private float brightness;
        private float twinkleTimer;
        private float twinkleSpeed;
        
        public Star(float x, float y, float size, float brightness) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.brightness = brightness;
            this.twinkleTimer = MathUtils.random(0, MathUtils.PI2);
            this.twinkleSpeed = MathUtils.random(1f, 3f);
        }
        
        public void update(float dt) {
            twinkleTimer += dt * twinkleSpeed;
            if (twinkleTimer > MathUtils.PI2) {
                twinkleTimer -= MathUtils.PI2;
            }
        }
        
        public void draw(ShapeRenderer shapeRenderer, Camera camera) {
            // Мерцание звезды
            float currentBrightness = brightness * (0.5f + 0.5f * MathUtils.sin(twinkleTimer));
            
            // Преобразуем мировые координаты в экранные
            float screenX = camera.worldToScreenX(x);
            float screenY = camera.worldToScreenY(y);
            
            shapeRenderer.setColor(currentBrightness, currentBrightness, currentBrightness, 1);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.circle(screenX, screenY, size);
            shapeRenderer.end();
        }
    }
}
