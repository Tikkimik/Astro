package com.gdx.entities;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.gdx.game.MyGdxGame;
import com.gdx.managers.Camera;

import java.util.ArrayList;

public class Background {

    private ArrayList<Star> stars;
    private final int NUM_STARS = 2000; // Значительно больше звезд для плотного фона
    private Camera camera;

    public Background(Camera camera) {
        this.camera = camera;
        stars = new ArrayList<>();
        generateStars();
    }

    private void generateStars() {
        // Генерируем звезды в большом радиусе вокруг центра для бесконечного мира
        float starRadius = 15000; // Увеличенный радиус для большего покрытия
        for (int i = 0; i < NUM_STARS; i++) {
            float angle = MathUtils.random(0, MathUtils.PI2);
            float distance = MathUtils.random(0, starRadius);
            float x = MathUtils.cos(angle) * distance;
            float y = MathUtils.sin(angle) * distance;
            float size = MathUtils.random(0.2f, 2.5f); // Больше разнообразия в размерах
            float brightness = MathUtils.random(0.2f, 1.0f); // Больше разнообразия в яркости
            stars.add(new Star(x, y, size, brightness));
        }
    }

    public void update(float dt) {
        // Звезды могут мерцать или двигаться
        for (Star star : stars) {
            star.update(dt);
        }

        // Динамически добавляем новые звезды по мере движения игрока
        float playerX = camera.getCameraX();
        float playerY = camera.getCameraY();

        // Если игрок ушел далеко от центра, добавляем новые звезды
        float distanceFromCenter = (float) Math.sqrt(playerX * playerX + playerY * playerY);
        if (distanceFromCenter > 5000 && stars.size() < NUM_STARS * 3) { // Увеличили лимит
            // Добавляем звезды в новом секторе
            float angle = MathUtils.random(0, MathUtils.PI2);
            float newDistance = distanceFromCenter + MathUtils.random(1000, 3000);
            float x = MathUtils.cos(angle) * newDistance;
            float y = MathUtils.sin(angle) * newDistance;
            float size = MathUtils.random(0.2f, 2.5f); // Больше разнообразия в размерах
            float brightness = MathUtils.random(0.2f, 1.0f); // Больше разнообразия в яркости
            stars.add(new Star(x, y, size, brightness));
        }
    }

    public void draw(ShapeRenderer shapeRenderer, Camera camera) {
        shapeRenderer.setColor(0, 0, 0, 1); // Черный фон
        shapeRenderer.rect(0, 0, MyGdxGame.WIDTH, MyGdxGame.HEIGHT);

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
            this.twinkleSpeed = MathUtils.random(0.5f, 4f); // Больше разнообразия в скорости мерцания
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
            shapeRenderer.circle(screenX, screenY, size);
        }
    }
}