package com.gdx.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.gdx.managers.Camera;
import com.gdx.managers.GameObjectManager;
import com.gdx.utils.ParticlePool;

public class EnemyShip extends Enemy implements GameObject.Drawable {
    
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
    private GameObjectManager gameObjectManager;
    
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
    private Array<EnemyFlameParticle> flameParticles;
    // Спавн пламени по таймеру: разреженный (~16 частиц/сек), чтобы не забивать
    // глобальный бюджет частиц. Красоту даёт размер и аддитивное свечение.
    private float flameSpawnTimer = 0f;
    private final float flameSpawnInterval = 0.06f;
    
    public EnemyShip(float x, float y, Player target, Array<OrkBullet> enemyBullets) {
        super(x, y, 3); // Здоровье = 3 для вражеского корабля
        this.target = target;
        this.enemyBullets = enemyBullets != null ? enemyBullets : new Array<>();
        
        // Инициализация размеров
        width = height = 24;
        
        // Начальное направление
        radians = MathUtils.random(2 * MathUtils.PI);
        
        // Инициализация частиц
        flameParticles = new Array<>();
        com.gdx.utils.ParticleBudget.register(flameParticles);
        
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
        
        float targetDx = target.getX() - x;
        float targetDy = target.getY() - y;
        float distance = (float) Math.sqrt(targetDx * targetDx + targetDy * targetDy);
        
        // Всегда поворачиваемся к цели
        float targetAngle = (float) Math.atan2(targetDy, targetDx);
        float angleDiff = targetAngle - radians;
        while (angleDiff > MathUtils.PI) angleDiff -= MathUtils.PI2;
        while (angleDiff < -MathUtils.PI) angleDiff += MathUtils.PI2;
        
        float rotSpeed = (distance < 400) ? rotationSpeed : rotationSpeed * 0.5f;
        if (Math.abs(angleDiff) > 0.1f) {
            radians += Math.signum(angleDiff) * rotSpeed * dt;
        }
        
        if (distance < 150) {
            // Атака — двигаемся к цели
            dx += MathUtils.cos(radians) * acceleration * dt;
            dy += MathUtils.sin(radians) * acceleration * dt;
            isPatrolling = false;
        } else if (distance < 350) {
            // Средняя дистанция — орбита (движемся перпендикулярно)
            float strafeAngle = targetAngle + MathUtils.HALF_PI * (float)Math.signum(MathUtils.random(-1, 1));
            dx += MathUtils.cos(strafeAngle) * acceleration * 0.5f * dt;
            dy += MathUtils.sin(strafeAngle) * acceleration * 0.5f * dt;
            isPatrolling = false;
        } else {
            // Далеко — двигаемся к цели
            dx += MathUtils.cos(radians) * acceleration * 0.4f * dt;
            dy += MathUtils.sin(radians) * acceleration * 0.4f * dt;
            isPatrolling = true;
        }
        
        // Ограничение скорости
        float speed = (float) Math.sqrt(dx * dx + dy * dy);
        if (speed > maxSpeed) {
            dx = (dx / speed) * maxSpeed;
            dy = (dy / speed) * maxSpeed;
        }
        
        x += dx * dt;
        y += dy * dt;
        
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
            OrkBullet bullet = new OrkBullet(x, y, angle);
            if (gameObjectManager != null) {
                gameObjectManager.addOrkBullet(bullet);
            }
            enemyBullets.add(bullet);
        }
    }
    
    public void setGameObjectManager(GameObjectManager gameObjectManager) {
        this.gameObjectManager = gameObjectManager;
    }
    
    private void createFlameParticles(float dt) {
        float speed = (float) Math.sqrt(dx * dx + dy * dy);
        
        // Создаем частицы только при движении
        if (speed > 10) {
            flameSpawnTimer += dt;
            if (flameSpawnTimer < flameSpawnInterval) {
                return;
            }
            flameSpawnTimer = 0f;
            
            float nozzleX = x - MathUtils.cos(radians) * 8;
            float nozzleY = y - MathUtils.sin(radians) * 8;
            float flameAngle = radians + MathUtils.PI; // Огонь направлен назад
            
            // Не создаём декоративные частицы, если корабль далеко за пределами
            // расширенной области спавна: далёкие враги двигаются и участвуют в
            // логике, но их невидимое пламя не должно генерировать частицы.
            if (!com.gdx.utils.ParticleBudget.isInSpawnBounds(nozzleX, nozzleY)) {
                return;
            }
            if (!com.gdx.utils.ParticleBudget.canSpawn(nozzleX, nozzleY)) {
                return;
            }
            
            float spreadAngle = flameAngle + (MathUtils.random() - 0.5f) * 0.35f;
            float flameSpeed = 60 + MathUtils.random() * 120;
            EnemyFlameParticle particle = ParticlePool.obtainEnemyFlameParticle();
            particle.init(nozzleX, nozzleY, spreadAngle, flameSpeed);
            flameParticles.add(particle);
            com.gdx.utils.ParticleBudget.add(1);
        }
    }
    
    private void updateParticles(float dt) {
        for (int i = flameParticles.size - 1; i >= 0; i--) {
            EnemyFlameParticle particle = flameParticles.get(i);
            // Используем фиксированный временной шаг для обновления частиц
            particle.update(com.gdx.utils.TimeManager.getFixedStep());
            
            if (particle.shouldRemove()) {
                ParticlePool.freeEnemyFlameParticle(particle);
                flameParticles.removeIndex(i);
                com.gdx.utils.ParticleBudget.release(1);
            }
        }
    }
    
    @Override
    public void draw(ShapeRenderer shapeRenderer, Camera camera) {
        float zoom = camera.getCurrentZoom();
        float r = width / 2f;
        
        // Треугольный корабль: нос, левое крыло, правое крыло
        float noseX = camera.worldToScreenX(x + MathUtils.cos(radians) * r);
        float noseY = camera.worldToScreenY(y + MathUtils.sin(radians) * r);
        float leftX = camera.worldToScreenX(x + MathUtils.cos(radians - 3 * MathUtils.PI / 4) * r * 0.9f);
        float leftY = camera.worldToScreenY(y + MathUtils.sin(radians - 3 * MathUtils.PI / 4) * r * 0.9f);
        float rightX = camera.worldToScreenX(x + MathUtils.cos(radians + 3 * MathUtils.PI / 4) * r * 0.9f);
        float rightY = camera.worldToScreenY(y + MathUtils.sin(radians + 3 * MathUtils.PI / 4) * r * 0.9f);
        
        shapeRenderer.setColor(0.9f, 0.1f, 0.1f, 1);
        shapeRenderer.triangle(noseX, noseY, leftX, leftY, rightX, rightY);
        
        // Кабина
        float cockpitCX = camera.worldToScreenX(x + MathUtils.cos(radians) * r * 0.3f);
        float cockpitCY = camera.worldToScreenY(y + MathUtils.sin(radians) * r * 0.3f);
        shapeRenderer.setColor(0.2f, 0.6f, 1f, 1);
        shapeRenderer.circle(cockpitCX, cockpitCY, 2 * zoom);
        
        // Частицы огня: только видимые, с аддитивным смешиванием для эффекта
        // свечения (внеэкранные частицы не тратят draw-вызовы и видимый бюджет)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (EnemyFlameParticle particle : flameParticles) {
            if (camera.isInRenderBounds(particle.getX(), particle.getY(), particle.getCullRadius())) {
                particle.draw(shapeRenderer, camera);
            }
        }
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
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
            for (EnemyFlameParticle p : flameParticles) {
                ParticlePool.freeEnemyFlameParticle(p);
            }
            com.gdx.utils.ParticleBudget.release(flameParticles.size);
            com.gdx.utils.ParticleBudget.unregister(flameParticles);
            flameParticles.clear();
        }
        
        if (shipTexture != null) {
            shipTexture.dispose();
        }
    }
    
    public Array<EnemyFlameParticle> getFlameParticles() {
        return flameParticles;
    }
}
