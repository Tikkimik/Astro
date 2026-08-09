package com.gdx.entities;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.gdx.managers.GameObjectManager;
import com.gdx.game.MyGdxGame;
import com.gdx.managers.Camera;

import com.gdx.utils.Line2D;
import com.gdx.utils.Point2D;
import com.gdx.utils.ParticlePool;

public class Player extends SpaceObject{

    private final int MAX_BULLETS = 4;
    private Array<Bullet> bullets; // Оставляем для совместимости, но не используем
    private GameObjectManager gameObjectManager; // Ссылка на менеджер объектов

    private final float[] flameX;
    private final float[] flameY;
    
    // Детали корабля для красивого рендеринга
    private final float[] engineX;
    private final float[] engineY;
    private final float[] cockpitX;
    private final float[] cockpitY;
    private final float[] wingX;
    private final float[] wingY;
    private final float[] weaponX;
    private final float[] weaponY;
    private final float[] antennaX;
    private final float[] antennaY;

    protected boolean left;
    protected boolean right;
    protected boolean up; //speedBoost

    private float maxSpeed;
    private float acceleration; //скорость разгона игрока
    private float deceleration; //скорость замедления игрока

    private float acceleratingTimer;
    private float engineGlowTimer; // Таймер для мерцания двигателей
    private float particleTimer = 0f; // Таймер для создания частиц с фиксированной частотой
    private Array<FlameParticle> flameParticles; // Частицы огня
    private Array<ShieldParticle> shieldParticles; // Синие частицы щита
    
    // Система неуязвимости (i-frames)
    private float invulnerabilityTimer;
    private final float invulnerabilityTime = 3.0f; // 3 секунды неуязвимости
    private boolean isInvulnerable;

    private boolean hit;
    private boolean dead;

    private float hitTimer;
    private float hitTime;

    private Line2D.Float[] hitLines;
    private Point2D.Float[] hitLinesVector;
    
    private Texture shipTexture;

    // Кэш последней геометрии: пересчитываем фигуру только при сдвиге/повороте
    private float lastShapeX = Float.NaN;
    private float lastShapeY = Float.NaN;
    private float lastShapeRad = Float.NaN;

    public Player(Array<Bullet> bullets){

        this.bullets = bullets;
        this.gameObjectManager = null; // Будет установлен позже

        x = MyGdxGame.WIDTH / 2;
        y = MyGdxGame.WIDTH / 2;

        maxSpeed = com.gdx.utils.GameSettings.getPlayerSpeed();
        acceleration = 200;
        deceleration = 10;

        shapeX = new float[4];
        shapeY = new float[4];
        flameX = new float[3];
        flameY = new float[3];
        
        // Инициализируем массивы для деталей корабля
        engineX = new float[4];
        engineY = new float[4];
        cockpitX = new float[3];
        cockpitY = new float[3];
        wingX = new float[6];
        wingY = new float[6];
        weaponX = new float[4];
        weaponY = new float[4];
        antennaX = new float[2];
        antennaY = new float[2];

        radians = MathUtils.HALF_PI;
        rotationSpeed = 3;

        hit = false;
        hitTimer = 0;
        hitTime = 2;
        
        // Инициализируем списки частиц
        flameParticles = new Array<>();
        shieldParticles = new Array<>();
        com.gdx.utils.ParticleBudget.register(flameParticles);
        com.gdx.utils.ParticleBudget.register(shieldParticles);
        
        // Активируем неуязвимость при создании корабля
        startInvulnerability();
        
        // Создаем текстуру корабля
        createShipTexture();
    }

    private void setShape(){
        // Основной корпус корабля
        shapeX[0] = x + MathUtils.cos(radians) * 10;
        shapeY[0] = y + MathUtils.sin(radians) * 10;

        shapeX[1] = x + MathUtils.cos(radians - 4 * MathUtils.PI / 5) * 8;
        shapeY[1] = y + MathUtils.sin(radians - 4 * MathUtils.PI / 5) * 8;

        shapeX[2] = x + MathUtils.cos(radians + MathUtils.PI) * 6;
        shapeY[2] = y + MathUtils.sin(radians + MathUtils.PI) * 6;

        shapeX[3] = x + MathUtils.cos(radians + 4 * MathUtils.PI / 5) * 8;
        shapeY[3] = y + MathUtils.sin(radians + 4 * MathUtils.PI / 5) * 8;
        
        // Обновляем детали корабля
        setEngineDetails();
        setCockpitDetails();
        setWingDetails();
        setWeaponDetails();
        setAntennaDetails();
    }
    
    private void setEngineDetails() {
        // Двигатели по бокам
        engineX[0] = x + MathUtils.cos(radians - 3 * MathUtils.PI / 4) * 6;
        engineY[0] = y + MathUtils.sin(radians - 3 * MathUtils.PI / 4) * 6;
        
        engineX[1] = x + MathUtils.cos(radians - 3 * MathUtils.PI / 4) * 4;
        engineY[1] = y + MathUtils.sin(radians - 3 * MathUtils.PI / 4) * 4;
        
        engineX[2] = x + MathUtils.cos(radians + 3 * MathUtils.PI / 4) * 6;
        engineY[2] = y + MathUtils.sin(radians + 3 * MathUtils.PI / 4) * 6;
        
        engineX[3] = x + MathUtils.cos(radians + 3 * MathUtils.PI / 4) * 4;
        engineY[3] = y + MathUtils.sin(radians + 3 * MathUtils.PI / 4) * 4;
    }
    
    private void setCockpitDetails() {
        // Кабина пилота
        cockpitX[0] = x + MathUtils.cos(radians) * 3;
        cockpitY[0] = y + MathUtils.sin(radians) * 3;
        
        cockpitX[1] = x + MathUtils.cos(radians - MathUtils.PI / 6) * 2;
        cockpitY[1] = y + MathUtils.sin(radians - MathUtils.PI / 6) * 2;
        
        cockpitX[2] = x + MathUtils.cos(radians + MathUtils.PI / 6) * 2;
        cockpitY[2] = y + MathUtils.sin(radians + MathUtils.PI / 6) * 2;
    }
    
    private void setWingDetails() {
        // Крылья корабля
        wingX[0] = x + MathUtils.cos(radians - 2 * MathUtils.PI / 3) * 12;
        wingY[0] = y + MathUtils.sin(radians - 2 * MathUtils.PI / 3) * 12;
        
        wingX[1] = x + MathUtils.cos(radians - 2 * MathUtils.PI / 3) * 8;
        wingY[1] = y + MathUtils.sin(radians - 2 * MathUtils.PI / 3) * 8;
        
        wingX[2] = x + MathUtils.cos(radians - MathUtils.PI / 2) * 10;
        wingY[2] = y + MathUtils.sin(radians - MathUtils.PI / 2) * 10;
        
        wingX[3] = x + MathUtils.cos(radians + 2 * MathUtils.PI / 3) * 12;
        wingY[3] = y + MathUtils.sin(radians + 2 * MathUtils.PI / 3) * 12;
        
        wingX[4] = x + MathUtils.cos(radians + 2 * MathUtils.PI / 3) * 8;
        wingY[4] = y + MathUtils.sin(radians + 2 * MathUtils.PI / 3) * 8;
        
        wingX[5] = x + MathUtils.cos(radians + MathUtils.PI / 2) * 10;
        wingY[5] = y + MathUtils.sin(radians + MathUtils.PI / 2) * 10;
    }
    
    private void setWeaponDetails() {
        // Орудия по бокам
        weaponX[0] = x + MathUtils.cos(radians - MathUtils.PI / 3) * 9;
        weaponY[0] = y + MathUtils.sin(radians - MathUtils.PI / 3) * 9;
        
        weaponX[1] = x + MathUtils.cos(radians - MathUtils.PI / 3) * 7;
        weaponY[1] = y + MathUtils.sin(radians - MathUtils.PI / 3) * 7;
        
        weaponX[2] = x + MathUtils.cos(radians + MathUtils.PI / 3) * 9;
        weaponY[2] = y + MathUtils.sin(radians + MathUtils.PI / 3) * 9;
        
        weaponX[3] = x + MathUtils.cos(radians + MathUtils.PI / 3) * 7;
        weaponY[3] = y + MathUtils.sin(radians + MathUtils.PI / 3) * 7;
    }
    
    private void setAntennaDetails() {
        // Антенны на носу корабля
        antennaX[0] = x + MathUtils.cos(radians) * 12;
        antennaY[0] = y + MathUtils.sin(radians) * 12;
        
        antennaX[1] = x + MathUtils.cos(radians) * 14;
        antennaY[1] = y + MathUtils.sin(radians) * 14;
    }
    
    private void createShipTexture() {
        // Загружаем текстуру корабля из файла
        // try {
            shipTexture = new Texture("ship.png");
        // } catch (Exception e) {
            // Если текстура не найдена, используем заглушку
            // shipTexture = new Texture("badlogic.jpg");
        // }
    }

    private void setFlame(){
        flameX[0] = x + MathUtils.cos(radians - 5 * MathUtils.PI / 6) * 5;
        flameY[0] = y + MathUtils.sin(radians - 5 * MathUtils.PI / 6) * 5;

        flameX[1] = x + MathUtils.cos(radians - MathUtils.PI) * (6 + acceleratingTimer * 50);
        flameY[1] = y + MathUtils.sin(radians - MathUtils.PI) * (6 + acceleratingTimer * 50);

        flameX[2] = x + MathUtils.cos(radians + 5 * MathUtils.PI / 6) * 5;
        flameY[2] = y + MathUtils.sin(radians + 5 * MathUtils.PI / 6) * 5;
    }

    public void setLeft(boolean b){
        left = b;
    }

    public void setRight(boolean b){
        right = b;
    }

    public void setUp(boolean b){
        up = b;
    }
    
    // Методы для применения улучшений
    public void setSpeed(float speed) {
        this.maxSpeed = speed;
    }
    
    public void setFireRate(float fireRate) {
        // Здесь можно добавить логику для изменения скорости стрельбы
        // Пока что просто сохраняем значение
    }
    
    public void setMaxHealth(int maxHealth) {
        // Здесь можно добавить логику для изменения максимального здоровья
        // Пока что просто сохраняем значение
    }
    
    public void setHealthRegen(float healthRegen) {
        // Здесь можно добавить логику для регенерации здоровья
        // Пока что просто сохраняем значение
    }
    
    public void setGameObjectManager(GameObjectManager gameObjectManager) {
        this.gameObjectManager = gameObjectManager;
    }

    public boolean intersects(SpaceObject other) {
        float dx = x - other.getX();
        float dy = y - other.getY();
        float distSq = dx * dx + dy * dy;
        float radiusSum = getWidth() / 2f + other.getWidth() / 2f;
        return distSq < radiusSum * radiusSum;
    }

    public void shoot(){
        if (gameObjectManager != null) {
            // Используем новую систему через GameObjectManager
            if (gameObjectManager.getProjectileCount() >= MAX_BULLETS) return;
            gameObjectManager.addBullet(new Bullet(x, y, radians));
        } else {
            // Fallback на старую систему для совместимости
            if(bullets.size == MAX_BULLETS) return;
            bullets.add(new Bullet(x, y, radians));
        }
    }

    public boolean isHit() {
        return hit;
    }

    public boolean isDead() {
        return dead;
    }

    public void reset() {
        x = MyGdxGame.WIDTH / 2;
        y = MyGdxGame.HEIGHT / 2;
        setShape();
        hit = dead = false;
        // Активируем неуязвимость при воскрешении
        startInvulnerability();
    }
    
    private void startInvulnerability() {
        isInvulnerable = true;
        invulnerabilityTimer = invulnerabilityTime;
    }
    
    public boolean isInvulnerable() {
        return isInvulnerable;
    }
    
    // Метод для пересоздания текстуры, если она потерялась
    public void recreateTextureIfNeeded() {
        // Всегда пересоздаем текстуру после восстановления состояния
        // так как OpenGL текстуры не могут быть сериализованы
        if (shipTexture != null) {
            shipTexture.dispose(); // Освобождаем старую текстуру
        }
        createShipTexture();
    }

    public void hit() {
        if(hit || isInvulnerable){
            return; // Не получаем урон если уже подбиты или неуязвимы
        }

        hit = true;
        dx = dy = 0;
        left = right = up = false;

        hitLines = new Line2D.Float[4];

        for(int i = 0, j = hitLines.length - 1; i < hitLines.length; j = i++) {
            hitLines[i] = new Line2D.Float(
                    shapeX[i], shapeY[i], shapeX[j], shapeY[j]
            );
        }

        hitLinesVector = new Point2D.Float[4];
        hitLinesVector[0] = new Point2D.Float(
                MathUtils.cos(radians + 1.5f),
                MathUtils.sin(radians + 1.5f)
        );

        hitLinesVector[1] = new Point2D.Float(
                MathUtils.cos(radians - 1.5f),
                MathUtils.sin(radians - 1.5f)
        );

        hitLinesVector[2] = new Point2D.Float(
                MathUtils.cos(radians - 2.8f),
                MathUtils.sin(radians - 2.8f)
        );

        hitLinesVector[3] = new Point2D.Float(
                MathUtils.cos(radians + 2.8f),
                MathUtils.sin(radians + 2.8f)
        );
    }

    public void update(float dt) {

        // Настройка «Скорость игрока» — единый источник максимальной скорости.
        // Читается каждый кадр логики, поэтому изменение применяется сразу,
        // без пересоздания игрока (проверяется в Debug-меню по факту скорости).
        maxSpeed = com.gdx.utils.GameSettings.getPlayerSpeed();

        //hit check
        if(hit) {
            hitTimer += dt;
            if(hitTimer > hitTime) {
                dead = true;
                hitTimer = 0;
            }
            for(int i = 0; i < hitLines.length; i++) {
                hitLines[i].setLine(
                        hitLines[i].x1 + hitLinesVector[i].x * 10 * dt,
                        hitLines[i].y1 + hitLinesVector[i].y * 10 * dt,
                        hitLines[i].x2 + hitLinesVector[i].x * 10 * dt,
                        hitLines[i].y2 + hitLinesVector[i].y * 10 * dt
                );
            }
            return;
        }

        //turning
        if(left){
            radians += rotationSpeed * dt;
        } else if(right){
            radians -= rotationSpeed * dt;
        }

        //accelerating
        if(up){
            dx += MathUtils.cos(radians) * acceleration * dt;
            dy += MathUtils.sin(radians) * acceleration * dt;
            acceleratingTimer += dt;
            if(acceleratingTimer > 0.1f){
                acceleratingTimer = 0;
            }
        } else {
            acceleratingTimer = 0;
        }

        //deaceleration
        float vector = (float) Math.sqrt(dx * dx + dy * dy);
        if(vector > 0){
            dx -= (dx / vector) * deceleration * dt;
            dy -= (dy / vector) * deceleration * dt;
        }
        if(vector > maxSpeed){
            dx = (dx / vector) * maxSpeed;
            dy = (dy / vector) * maxSpeed;
        }

        //setPosition
        x += dx * dt;
        y += dy * dt;

        //set shape (только если изменилась позиция или угол)
        if (x != lastShapeX || y != lastShapeY || radians != lastShapeRad) {
            setShape();
            lastShapeX = x;
            lastShapeY = y;
            lastShapeRad = radians;
        }

        //set flame
        if(up){
            setFlame();
        }
        
        // Обновляем эффекты двигателей
        engineGlowTimer += dt * 3f;
        if (engineGlowTimer > MathUtils.PI2) {
            engineGlowTimer -= MathUtils.PI2;
        }
        
        // Создаем частицы огня при движении (фиксированная частота)
        createFlameParticles(dt);
        
        // Обновляем частицы огня
        for(int i = flameParticles.size - 1; i >= 0; i--) {
            FlameParticle particle = flameParticles.get(i);
            // Используем фиксированный временной шаг для обновления частиц
            particle.update(com.gdx.utils.TimeManager.getFixedStep());
            
            if(particle.shouldRemove()) {
                // Возвращаем частицу в пул
                ParticlePool.freeFlameParticle(particle);
                flameParticles.removeIndex(i);
                com.gdx.utils.ParticleBudget.release(1);
            }
        }
        

        
        // Обновляем таймер неуязвимости
        if(isInvulnerable) {
            invulnerabilityTimer -= dt;
            if(invulnerabilityTimer <= 0) {
                isInvulnerable = false;
            }
        }
        
        // Создаем синие частицы щита только во время неуязвимости
        if (isInvulnerable && MathUtils.random() < 0.5f && com.gdx.utils.ParticleBudget.canSpawn(x, y)) { // 50% шанс создания частицы каждый кадр
            float angle = MathUtils.random() * MathUtils.PI2;
            float radius = 20 + MathUtils.random() * 10; // Радиус от 20 до 30
            
            // Получаем частицу из пула
            ShieldParticle particle = ParticlePool.obtainShieldParticle();
            particle.init(x, y, angle, radius);
            shieldParticles.add(particle);
            com.gdx.utils.ParticleBudget.add(1);
        }
        
        // Обновляем синие частицы
        for(int i = shieldParticles.size - 1; i >= 0; i--) {
            ShieldParticle particle = shieldParticles.get(i);
            // Используем фиксированный временной шаг для обновления частиц
            particle.update(com.gdx.utils.TimeManager.getFixedStep());
            
            if(particle.shouldRemove()) {
                // Возвращаем частицу в пул
                ParticlePool.freeShieldParticle(particle);
                shieldParticles.removeIndex(i);
                com.gdx.utils.ParticleBudget.release(1);
            }
        }

        //screen warp
        wrap();
    }
    
    private void createFlameParticles(float dt) {
        // Создаем частицы огня из сопла (каждый кадр игровой логики)
        if(up) {
            float nozzleX = x - MathUtils.cos(radians) * 8;
            float nozzleY = y - MathUtils.sin(radians) * 8 - 2; // Смещаем немного ниже
            float flameAngle = radians + MathUtils.PI; // Огонь направлен назад
            
            // Не создаём декоративные частицы, если эмиттер далеко за пределами
            // расширенной области спавна (камера далеко — огонь всё равно не виден).
            if (!com.gdx.utils.ParticleBudget.isInSpawnBounds(nozzleX, nozzleY)) {
                return;
            }
            
            // Используем настройку количества частиц (частиц за кадр логики)
            int particleCount = com.gdx.utils.GameSettings.getShipEngineParticles();
            
            // Создаем частицы согласно настройке (с учётом глобального лимита)
            for(int i = 0; i < particleCount && com.gdx.utils.ParticleBudget.canSpawn(nozzleX, nozzleY); i++) {
                float spreadAngle = flameAngle + (MathUtils.random() - 0.5f) * 0.4f; // Больший разброс
                float flameSpeed = 150 + MathUtils.random() * 150; // Случайная скорость
                
                // Получаем частицу из пула
                FlameParticle particle = ParticlePool.obtainFlameParticle();
                particle.init(nozzleX, nozzleY, spreadAngle, flameSpeed);
                flameParticles.add(particle);
                com.gdx.utils.ParticleBudget.add(1);
            }
        }
    }

    public void draw(ShapeRenderer shapeRenderer, Camera camera, SpriteBatch spriteBatch){

        //hit check
        if(hit) {
            // Эффект взрыва - красные линии
            shapeRenderer.setColor(1, 0.3f, 0.1f, 1);
            for(int i = 0; i < hitLines.length; i++) {
                float screenX1 = camera.worldToScreenX(hitLines[i].x1);
                float screenY1 = camera.worldToScreenY(hitLines[i].y1);
                float screenX2 = camera.worldToScreenX(hitLines[i].x2);
                float screenY2 = camera.worldToScreenY(hitLines[i].y2);
                shapeRenderer.line(screenX1, screenY1, screenX2, screenY2);
            }
            return;
        }

        // Рисуем текстуру корабля
        if (shipTexture != null) {
            float screenX = camera.worldToScreenX(x);
            float screenY = camera.worldToScreenY(y);
            float textureSize = 32 * camera.getCurrentZoom(); // Масштабируем с зумом
            
            spriteBatch.begin();
            
            // Основная текстура корабля
            if(isInvulnerable) {
                // Мерцание во время неуязвимости
                float alpha = 0.5f + MathUtils.sin(invulnerabilityTimer * 20f) * 0.5f; // Быстрое мерцание
                spriteBatch.setColor(1, 1, 1, alpha);
            } else {
                spriteBatch.setColor(1, 1, 1, 1);
            }
            spriteBatch.draw(
                shipTexture, 
                screenX - textureSize/2, 
                screenY - textureSize/2, 
                textureSize/2, textureSize/2, // Точка вращения
                textureSize, textureSize, // Размер
                1, 1, // Масштаб
                (radians * MathUtils.radiansToDegrees) + 270, // Поворот + 180 градусов
                0, 0, // Область текстуры
                shipTexture.getWidth(), shipTexture.getHeight(), // Размер области
                false, false
            ); // Переворот
            
            // Эффект свечения поверх основной текстуры (если двигатель включен)
            if(up) {
                // Аддитивное смешивание для эффекта свечения
                spriteBatch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                
                float flameIntensity = 0.3f + MathUtils.sin(engineGlowTimer * 6f) * 0.1f; // Мерцание
                spriteBatch.setColor(1.0f, 0.4f, 0.1f, flameIntensity); // Оранжевое свечение
                
                // Рисуем свечение немного больше основной текстуры
                float glowSize = textureSize * 1.2f;
                spriteBatch.draw(
                    shipTexture, 
                    screenX - glowSize/2, 
                    screenY - glowSize/2, 
                    glowSize/2, glowSize/2, // Точка вращения
                    glowSize, glowSize, // Размер
                    1, 1, // Масштаб
                    (radians * MathUtils.radiansToDegrees) + 270, // Поворот
                    0, 0, // Область текстуры
                    shipTexture.getWidth(), shipTexture.getHeight(), // Размер области
                    false, false
                );
                
                // Возвращаем обычное смешивание
                spriteBatch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            }
            
            spriteBatch.setColor(1, 1, 1, 1);
            spriteBatch.end();
        }

        // Частицы огня двигателей (создаются в update с фиксированной частотой)
        // Здесь только отрисовка
        
        // Рисуем частицы огня (заполненные круги)
        // Только видимые: внеэкранные частицы не должны тратить draw-вызовы
        shapeRenderer.end(); // Заканчиваем рендеринг линий
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        for(int i = flameParticles.size - 1; i >= 0; i--) {
            FlameParticle particle = flameParticles.get(i);
            if (camera.isInRenderBounds(particle.getX(), particle.getY(), particle.getCullRadius())) {
                particle.draw(shapeRenderer, camera);
            }
        }
        
        // Рисуем синие частицы щита (только видимые)
        for(int i = shieldParticles.size - 1; i >= 0; i--) {
            ShieldParticle particle = shieldParticles.get(i);
            if (camera.isInRenderBounds(particle.getX(), particle.getY(), particle.getCullRadius())) {
                particle.draw(shapeRenderer, camera);
            }
        }
        
        shapeRenderer.end(); // Заканчиваем рендеринг заполненных объектов
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line); // Возвращаемся к линиям
    }
    
    public int getWidth() {
        return 16; // Примерный размер корабля
    }
    
    /**
     * Текущая максимальная скорость корабля (из настройки «Скорость игрока»).
     */
    public float getMaxSpeed() {
        return maxSpeed;
    }
    
    public void dispose() {
        // Очищаем частицы огня
        if (flameParticles != null) {
            com.gdx.utils.ParticleBudget.release(flameParticles.size);
            com.gdx.utils.ParticleBudget.unregister(flameParticles);
            flameParticles.clear();
        }
        
        // Очищаем синие частицы щита
        if (shieldParticles != null) {
            com.gdx.utils.ParticleBudget.release(shieldParticles.size);
            com.gdx.utils.ParticleBudget.unregister(shieldParticles);
            shieldParticles.clear();
        }
        
        // Освобождаем текстуру
        if (shipTexture != null) {
            shipTexture.dispose();
        }
    }
    
    public Array<FlameParticle> getFlameParticles() {
        return flameParticles;
    }

    public Array<ShieldParticle> getShieldParticles() {
        return shieldParticles;
    }
}
