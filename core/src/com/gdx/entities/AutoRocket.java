package com.gdx.entities;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.gdx.managers.Camera;
import java.util.ArrayList;


public class AutoRocket extends SpaceObject {
    
    private float initialSpeed = 200f; // Начальная скорость (медленнее)
    private float maxSpeed = 600f; // Максимальная скорость
    private float currentSpeed = 200f;
    private float acceleration = 800f; // Ускорение ракеты
    private float lifeTime = 8f; // Увеличиваем время жизни
    private float lifeTimer = 0f;
    private boolean remove = false;
    
    // Фазы полета
    private float launchPhase = 0.5f; // Время начальной фазы (0.5 секунды)
    private float launchTimer = 0f;
    private boolean isLaunched = false;
    
    // Цель ракеты (теперь любой враг)
    private Enemy target;
    
    // Урон ракеты
    private float damage = 1f;
    
    // Частицы ракеты
    private ArrayList<RocketParticle> rocketParticles;
    
    public AutoRocket(float x, float y, float rocketDirection, Enemy target) {
        this.x = x;
        this.y = y;
        this.target = target;
        
        // Начальное направление ракеты (уже задано извне)
        this.radians = rocketDirection;
        this.dx = MathUtils.cos(radians) * initialSpeed;
        this.dy = MathUtils.sin(radians) * initialSpeed;
        
        width = height = 3;
        
        // Инициализируем частицы ракеты
        rocketParticles = new ArrayList<>();
    }
    
    public void update(float dt) {
        // Обновляем таймеры
        lifeTimer += dt;
        launchTimer += dt;
        
        // Проверяем время жизни
        if (lifeTimer > lifeTime) {
            remove = true;
            return;
        }
        
        // Начальная фаза - летим прямо вверх
        if (launchTimer < launchPhase) {
            // Просто летим вверх с начальной скоростью
            x += dx * dt;
            y += dy * dt;
        } else {
            // Фаза самонаведения
            if (!isLaunched) {
                isLaunched = true;
                // Начинаем самонаведение
                recalculateTrajectory();
            }
            
            // Постоянно пересчитываем траекторию для самонаведения
            recalculateTrajectory();
            
            // Ускоряем ракету
            currentSpeed += acceleration * dt;
            if (currentSpeed > maxSpeed) {
                currentSpeed = maxSpeed;
            }
            
            // Обновляем позицию с новой скоростью
            float speedMultiplier = currentSpeed / initialSpeed;
            x += dx * speedMultiplier * dt;
            y += dy * speedMultiplier * dt;
        }
        
        // Проверяем границы мира
        wrap();
        
        // Создаем густые серые частицы ракеты (из задней части) - независимо от FPS
        float particleChance = 2.5f * dt * 60f; // Увеличили с 0.9f до 2.5f для более густого следа
        if (MathUtils.random() < particleChance) {
            float trailAngle = (float) Math.atan2(dy, dx) + MathUtils.PI; // Противоположное направление движения
            rocketParticles.add(new RocketParticle(x, y, trailAngle));
        }
        
        // Создаем дополнительные частицы для еще более плотного следа
        float extraParticleChance = 1.8f * dt * 60f; // Увеличили с 0.3f до 1.8f
        if (MathUtils.random() < extraParticleChance) {
            float trailAngle = (float) Math.atan2(dy, dx) + MathUtils.PI;
            rocketParticles.add(new RocketParticle(x, y, trailAngle));
        }
        
        // Еще больше частиц для очень густого следа
        float thirdParticleChance = 1.2f * dt * 60f; // Новый уровень частиц
        if (MathUtils.random() < thirdParticleChance) {
            float trailAngle = (float) Math.atan2(dy, dx) + MathUtils.PI;
            rocketParticles.add(new RocketParticle(x, y, trailAngle));
        }
        
        // Обновляем частицы ракеты
        for(int i = rocketParticles.size() - 1; i >= 0; i--) {
            RocketParticle particle = rocketParticles.get(i);
            particle.update(dt);
            
            if(particle.shouldRemove()) {
                rocketParticles.remove(i);
            }
        }
    }
    
    // Пересчет траектории для самонаведения
    private void recalculateTrajectory() {
        if (target == null) return;
        
        // Вычисляем направление к цели
        float targetDx = target.getX() - x;
        float targetDy = target.getY() - y;
        float distance = (float) Math.sqrt(targetDx * targetDx + targetDy * targetDy);
        
        if (distance > 0) {
            // Плавно поворачиваем к цели
            float targetAngle = (float) Math.atan2(targetDy, targetDx);
            float currentAngle = (float) Math.atan2(dy, dx);
            
            // Вычисляем разность углов
            float angleDiff = targetAngle - currentAngle;
            
            // Нормализуем угол
            while (angleDiff > MathUtils.PI) angleDiff -= 2 * MathUtils.PI;
            while (angleDiff < -MathUtils.PI) angleDiff += 2 * MathUtils.PI;
            
            // Плавный поворот (не слишком резкий)
            float turnSpeed = 3f; // Скорость поворота
            float maxTurn = turnSpeed * 0.016f; // Максимальный поворот за кадр
            
            if (Math.abs(angleDiff) > maxTurn) {
                angleDiff = Math.signum(angleDiff) * maxTurn;
            }
            
            // Обновляем направление
            float newAngle = currentAngle + angleDiff;
            dx = MathUtils.cos(newAngle) * currentSpeed;
            dy = MathUtils.sin(newAngle) * currentSpeed;
        }
    }
    
    public boolean shouldRemove() {
        return remove;
    }
    
    public void draw(ShapeRenderer shapeRenderer, Camera camera) {
        // Рисуем ракету как маленький треугольник
        float screenX = camera.worldToScreenX(x);
        float screenY = camera.worldToScreenY(y);
        
        // Вычисляем направление движения
        float direction = (float) Math.atan2(dy, dx);
        
        // Размер ракеты зависит от фазы полета и масштабируется с зумом
        float baseRocketSize = isLaunched ? 6 : 4; // Больше в фазе самонаведения
        float rocketSize = baseRocketSize * camera.getCurrentZoom(); // Масштабируем с зумом
        
        // Создаем треугольник ракеты
        float[] rocketX = new float[3];
        float[] rocketY = new float[3];
        
        // Нос ракеты
        rocketX[0] = screenX + MathUtils.cos(direction) * rocketSize;
        rocketY[0] = screenY + MathUtils.sin(direction) * rocketSize;
        
        // Боковые точки
        rocketX[1] = screenX + MathUtils.cos(direction + 2.5f) * (rocketSize * 0.6f);
        rocketY[1] = screenY + MathUtils.sin(direction + 2.5f) * (rocketSize * 0.6f);
        
        rocketX[2] = screenX + MathUtils.cos(direction - 2.5f) * (rocketSize * 0.6f);
        rocketY[2] = screenY + MathUtils.sin(direction - 2.5f) * (rocketSize * 0.6f);
        
        // Цвет зависит от фазы полета
        if (isLaunched) {
            // В фазе самонаведения - ярко-оранжевый
            shapeRenderer.setColor(1, 0.3f, 0, 1);
        } else {
            // В начальной фазе - желтый
            shapeRenderer.setColor(1, 1, 0, 1);
        }
        
        shapeRenderer.triangle(rocketX[0], rocketY[0], rocketX[1], rocketY[1], rocketX[2], rocketY[2]);
        
        // Рисуем контур
        shapeRenderer.setColor(1, 1, 1, 1);
        shapeRenderer.triangle(rocketX[0], rocketY[0], rocketX[1], rocketY[1], rocketX[2], rocketY[2]);
        
        // В фазе самонаведения добавляем след
        if (isLaunched) {
            drawTrail(shapeRenderer, camera, direction);
        }
        
        // Рисуем синие частицы ракеты
        drawRocketParticles(shapeRenderer, camera);
    }
    
    // Рисуем след ракеты
    private void drawTrail(ShapeRenderer shapeRenderer, Camera camera, float direction) {
        float screenX = camera.worldToScreenX(x);
        float screenY = camera.worldToScreenY(y);
        
        // След за ракетой (масштабируется с зумом)
        float baseTrailLength = 12;
        float trailLength = baseTrailLength * camera.getCurrentZoom();
        float trailX = screenX - MathUtils.cos(direction) * trailLength;
        float trailY = screenY - MathUtils.sin(direction) * trailLength;
        
        shapeRenderer.setColor(1, 0.5f, 0, 0.5f); // Полупрозрачный оранжевый
        shapeRenderer.line(screenX, screenY, trailX, trailY);
    }
    
        // Проверяем столкновение с любым врагом (оптимизированная)
    public boolean intersects(Enemy enemy) {
        float dx = x - enemy.getX();
        float dy = y - enemy.getY();
        float distanceSquared = dx * dx + dy * dy;
        float radiusSum = enemy.getWidth() / 2 + width / 2;
        return distanceSquared < radiusSum * radiusSum;
    }
    
    // Рисуем синие частицы ракеты
    private void drawRocketParticles(ShapeRenderer shapeRenderer, Camera camera) {
        for(RocketParticle particle : rocketParticles) {
            particle.draw(shapeRenderer, camera);
        }
    }
    
    // Метод для установки урона ракеты
    public void setDamage(float damage) {
        this.damage = damage;
    }
    
    // Геттер для урона ракеты
    public float getDamage() {
        return damage;
    }
}
