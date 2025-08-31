package com.gdx.entities;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.gdx.managers.Camera;

public class EnemyShip extends Enemy {
    
    // Система стрельбы
    private float shootTimer = 0f;
    private final float shootInterval = 1.5f; // Интервал между выстрелами
    private final int burstSize = 2; // Количество выстрелов в очереди
    private int currentBurst = 0;
    private float burstTimer = 0f;
    private final float burstInterval = 0.3f; // Интервал между выстрелами в очереди
    
    // Цель для стрельбы
    private Player target;
    
    // Список пуль врага (используем Array вместо ArrayList)
    private Array<OrkBullet> enemyBullets;
    
    // Параметры движения
    private float maxSpeed = 120f;
    private float acceleration = 80f;
    private float deceleration = 15f;
    private float rotationSpeed = 2f;
    
    // Текстура корабля
    private Texture shipTexture;
    
    // Система патрулирования
    private float patrolTimer = 0f;
    private final float patrolChangeTime = 3f;
    private float targetAngle = 0f;
    private boolean isPatrolling = true;
    
    // Эффекты
    private float engineGlowTimer = 0f;
    private Array<FlameParticle> flameParticles;
    private float particleTimer = 0f; // Таймер для создания частиц
    
    public EnemyShip(float x, float y, Player target, Array<OrkBullet> enemyBullets) {
        super(x, y, 3); // Здоровье = 3 для вражеского корабля
        this.target = target;
        this.enemyBullets = enemyBullets;
        
        // Инициализация размеров
        width = height = 24;
        
        // Начальное направление
        radians = MathUtils.random(2 * MathUtils.PI);
        
        // Инициализация частиц
        flameParticles = new Array<>();
        
        // Загрузка текстуры
        createShipTexture();
    }
    
    private void createShipTexture() {
        try {
            // Загружаем текстуру try2.png
            shipTexture = new Texture("try2.png");
        } catch (Exception e) {
            // Если не получилось, используем заглушку
            shipTexture = new Texture("badlogic.jpg");
        }
    }
    
    @Override
    public void update(float dt) {
        // Обновляем таймеры
        shootTimer += dt;
        burstTimer += dt;
        patrolTimer += dt;
        engineGlowTimer += dt * 3f;
        
        if (engineGlowTimer > MathUtils.PI2) {
            engineGlowTimer -= MathUtils.PI2;
        }
        
        // Логика движения
        updateMovement(dt);
        
        // Логика стрельбы
        updateShooting(dt);
        
        // Обновляем частицы
        updateParticles(dt);
        
        // Проверяем границы мира
        wrap();
    }
    
    private void updateMovement(float dt) {
        if (target == null) return;
        
        // Вычисляем расстояние до цели
        float targetDx = target.getX() - x;
        float targetDy = target.getY() - y;
        float distance = (float) Math.sqrt(targetDx * targetDx + targetDy * targetDy);
        
        // Если цель близко, переходим в режим атаки
        if (distance < 200) {
            isPatrolling = false;
            // Поворачиваемся к цели
            float targetAngle = (float) Math.atan2(targetDy, targetDx);
            float angleDiff = targetAngle - radians;
            
            // Нормализуем разность углов
            while (angleDiff > MathUtils.PI) angleDiff -= MathUtils.PI2;
            while (angleDiff < -MathUtils.PI) angleDiff += MathUtils.PI2;
            
            // Поворачиваемся к цели
            if (Math.abs(angleDiff) > 0.1f) {
                if (angleDiff > 0) {
                    radians += rotationSpeed * dt;
                } else {
                    radians -= rotationSpeed * dt;
                }
            }
            
            // Двигаемся к цели
            dx += MathUtils.cos(radians) * acceleration * dt;
            dy += MathUtils.sin(radians) * acceleration * dt;
        } else {
            // Патрулирование
            isPatrolling = true;
            if (patrolTimer > patrolChangeTime) {
                targetAngle = MathUtils.random(2 * MathUtils.PI);
                patrolTimer = 0f;
            }
            
            // Поворачиваемся к цели патрулирования
            float angleDiff = targetAngle - radians;
            while (angleDiff > MathUtils.PI) angleDiff -= MathUtils.PI2;
            while (angleDiff < -MathUtils.PI) angleDiff += MathUtils.PI2;
            
            if (Math.abs(angleDiff) > 0.1f) {
                if (angleDiff > 0) {
                    radians += rotationSpeed * 0.5f * dt;
                } else {
                    radians -= rotationSpeed * 0.5f * dt;
                }
            }
            
            // Медленно двигаемся в направлении патрулирования
            dx += MathUtils.cos(radians) * acceleration * 0.3f * dt;
            dy += MathUtils.sin(radians) * acceleration * 0.3f * dt;
        }
        
        // Ограничиваем скорость
        float speed = (float) Math.sqrt(dx * dx + dy * dy);
        if (speed > maxSpeed) {
            dx = (dx / speed) * maxSpeed;
            dy = (dy / speed) * maxSpeed;
        }
        
        // Замедление
        if (speed > 0) {
            dx -= (dx / speed) * deceleration * dt;
            dy -= (dy / speed) * deceleration * dt;
        }
        
        // Обновляем позицию
        x += dx * dt;
        y += dy * dt;
        
        // Создаем частицы огня при движении (фиксированная частота)
        createFlameParticles(dt);
    }
    
    private void updateShooting(float dt) {
        if (target == null) return;
        
        // Вычисляем расстояние до цели
        float targetDx = target.getX() - x;
        float targetDy = target.getY() - y;
        float distance = (float) Math.sqrt(targetDx * targetDx + targetDy * targetDy);
        
        // Стреляем только если цель в пределах видимости
        if (distance < 300) {
            // Проверяем, нужно ли начать новую очередь выстрелов
            if (shootTimer >= shootInterval && currentBurst == 0) {
                currentBurst = burstSize;
                burstTimer = 0f;
            }
            
            // Стреляем в очереди
            if (currentBurst > 0 && burstTimer >= burstInterval) {
                shoot();
                currentBurst--;
                burstTimer = 0f;
                
                // Если очередь закончилась, сбрасываем основной таймер
                if (currentBurst == 0) {
                    shootTimer = 0f;
                }
            }
        }
    }
    
    private void shoot() {
        if (target == null) return;
        
        // Вычисляем направление к цели
        float targetDx = target.getX() - x;
        float targetDy = target.getY() - y;
        float distance = (float) Math.sqrt(targetDx * targetDx + targetDy * targetDy);
        
        if (distance > 0) {
            // Добавляем небольшое отклонение для реалистичности
            float accuracy = 0.2f; // Точность стрельбы
            float angle = (float) Math.atan2(targetDy, targetDx);
            angle += (MathUtils.random() - 0.5f) * accuracy;
            
            // Создаем пулю врага
            enemyBullets.add(new OrkBullet(x, y, angle));
        }
    }
    
    private void createFlameParticles(float dt) {
        // Создаем частицы огня из сопла (каждый кадр игровой логики)
        float speed = (float) Math.sqrt(dx * dx + dy * dy);
        
        // Создаем частицы только при движении
        if (speed > 10) {
            float nozzleX = x - MathUtils.cos(radians) * 8;
            float nozzleY = y - MathUtils.sin(radians) * 8;
            float flameAngle = radians + MathUtils.PI; // Огонь направлен назад
            
            // Создаем много частиц для обильного эффекта
            for(int i = 0; i < 6; i++) {
                float spreadAngle = flameAngle + (MathUtils.random() - 0.5f) * 0.4f;
                float flameSpeed = 80 + MathUtils.random() * 120;
                flameParticles.add(new FlameParticle(nozzleX, nozzleY, spreadAngle, flameSpeed));
            }
        }
    }
    
    private void updateParticles(float dt) {
        for (int i = flameParticles.size - 1; i >= 0; i--) {
            FlameParticle particle = flameParticles.get(i);
            // Используем фиксированный временной шаг для обновления частиц
            particle.update(com.gdx.utils.TimeManager.FIXED_TIMESTEP);
            
            if (particle.shouldRemove()) {
                flameParticles.removeIndex(i);
            }
        }
        

    }
    
    @Override
    public void draw(ShapeRenderer shapeRenderer, Camera camera) {
        // Рисуем контур для отладки
        float screenX = camera.worldToScreenX(x);
        float screenY = camera.worldToScreenY(y);
        float radius = (width / 2) * camera.getCurrentZoom();
        shapeRenderer.circle(screenX, screenY, radius);
    }
    
    public void drawWithSpriteBatch(ShapeRenderer shapeRenderer, Camera camera, SpriteBatch spriteBatch) {
        // Рисуем текстуру корабля (оптимизировано - без begin/end)
        if (shipTexture != null) {
            float screenX = camera.worldToScreenX(x);
            float screenY = camera.worldToScreenY(y);
            float textureSize = 24 * camera.getCurrentZoom();
            
            // Основная текстура корабля (предполагаем, что spriteBatch уже начат)
            spriteBatch.setColor(1, 1, 1, 1);
            spriteBatch.draw(
                shipTexture, 
                screenX - textureSize/2, 
                screenY - textureSize/2, 
                textureSize/2, textureSize/2, // Точка вращения
                textureSize, textureSize, // Размер
                1, 1, // Масштаб
                (radians * MathUtils.radiansToDegrees) + 270, // Поворот
                0, 0, // Область текстуры
                shipTexture.getWidth(), shipTexture.getHeight(), // Размер области
                false, false
            );
        }
    }
    
    public void drawParticles(ShapeRenderer shapeRenderer, Camera camera) {
        // Рисуем частицы огня отдельно (только если они есть)
        if (!flameParticles.isEmpty()) {
            shapeRenderer.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            
            for (FlameParticle particle : flameParticles) {
                particle.draw(shapeRenderer, camera);
            }
            
            shapeRenderer.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        }
    }
    
    @Override
    public int getWidth() {
        return width;
    }
    
    @Override
    public int getHeight() {
        return height;
    }
    
    @Override
    public String getEnemyType() {
        return "EnemyShip";
    }
    
    public void dispose() {
        if (flameParticles != null) {
            flameParticles.clear();
        }
        
        if (shipTexture != null) {
            shipTexture.dispose();
        }
    }
    
    public Array<FlameParticle> getFlameParticles() {
        return flameParticles;
    }
}
