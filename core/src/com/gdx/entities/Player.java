package com.gdx.entities;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.gdx.game.MyGdxGame;
import com.gdx.managers.Camera;

import com.gdx.utils.Line2D;
import com.gdx.utils.Point2D;
import com.gdx.utils.ParticlePool;

public class Player extends SpaceObject{

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
    private Array<SparkParticle> sparkParticles; // Редкие искры из сопел

    // === Эмиссия двигателя ===
    // Отдельные накопители и интервалы для левого/правого сопла: эмиссия не
    // синхронна, интервалы сбрасываются со случайным разбросом (±18%), поэтому
    // не образуется парный пунктир.
    private static final float STREAM_RATE = 75f;  // плотный непрерывный шлейф
    private static final float FLAME_RATE = 25f;   // уплотнение возле сопла
    private static final float SPARK_RATE = 3.5f;
    private static final float INTERVAL_JITTER = 0.15f;
    private float streamTimerLeft, streamTimerRight;
    private float flameTimerLeft, flameTimerRight;
    private float streamNextLeft, streamNextRight;
    private float flameNextLeft, flameNextRight;
    // Позиции сопел в начале шага — для интерполяции стартовых позиций частиц
    // между предыдущим и текущим положением сопла (несколько спавнов за шаг).
    private float lastLeftX, lastLeftY, lastRightX, lastRightY;
    // Плотность эффекта из настроек (0-4), пересчитывается в update.
    private float engineDensity = 1f;
    // Сглаженный изгиб газового факела от текущей траектории корабля.
    private float plumeBend;
    // Инерция тяги: шейдерный факел не исчезает одним кадром после отпускания.
    private float plumeThrottle;
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
    private FlameRenderer enginePlumeRenderer;
    private ExhaustTrailRenderer exhaustTrailRenderer;

    // Кэш последней геометрии: пересчитываем фигуру только при сдвиге/повороте
    private float lastShapeX = Float.NaN;
    private float lastShapeY = Float.NaN;
    private float lastShapeRad = Float.NaN;

    public Player(){
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
        sparkParticles = new Array<>();
        com.gdx.utils.ParticleBudget.register(flameParticles);
        com.gdx.utils.ParticleBudget.register(shieldParticles);
        com.gdx.utils.ParticleBudget.register(sparkParticles);

        // Стартовые интервалы эмиссии и позиции сопел
        streamNextLeft = streamNextRight = jitteredInterval(STREAM_RATE);
        flameNextLeft = flameNextRight = jitteredInterval(FLAME_RATE);
        lastLeftX = lastRightX = x;
        lastLeftY = lastRightY = y;

        // Активируем неуязвимость при создании корабля
        startInvulnerability();

        // Создаем текстуру корабля
        createShipTexture();
        enginePlumeRenderer = new FlameRenderer();
        exhaustTrailRenderer = new ExhaustTrailRenderer();
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
        // Сопла двигателей по краям корабля: в локальной системе корабля они
        // находятся на (±6, -8) от центра — точно в местах сопел на текстуре
        // ship.png (пиксели 10-24 и 22-24 при размере текстуры 32).
        // Право = (sin(radians), -cos(radians)), вперёд = (cos(radians), sin(radians)).
        float cos = MathUtils.cos(radians);
        float sin = MathUtils.sin(radians);

        // Левое сопло (side = -6)
        engineX[0] = x - sin * 6 - cos * 8;
        engineY[0] = y + cos * 6 - sin * 8;
        // Внутренняя точка левого сопла (side = -3)
        engineX[1] = x - sin * 3 - cos * 8;
        engineY[1] = y + cos * 3 - sin * 8;

        // Правое сопло (side = +6)
        engineX[2] = x + sin * 6 - cos * 8;
        engineY[2] = y - cos * 6 - sin * 8;
        // Внутренняя точка правого сопла (side = +3)
        engineX[3] = x + sin * 3 - cos * 8;
        engineY[3] = y - cos * 3 - sin * 8;
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

    public boolean intersects(SpaceObject other) {
        float dx = x - other.getX();
        float dy = y - other.getY();
        float distSq = dx * dx + dy * dy;
        float radiusSum = getWidth() / 2f + other.getWidth() / 2f;
        return distSq < radiusSum * radiusSum;
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

    public void restorePersistentState(float x, float y, float dx, float dy, float radians) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.radians = radians;
        hit = false;
        dead = false;
        setShape();
        startInvulnerability();
    }

    private void startInvulnerability() {
        isInvulnerable = true;
        invulnerabilityTimer = invulnerabilityTime;
    }

    public boolean isInvulnerable() {
        return isInvulnerable;
    }

    public void cancelInvulnerability() {
        isInvulnerable = false;
        invulnerabilityTimer = 0f;
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
        // Боковое скольжение и поворот смещают только дальнюю часть струи.
        // Вектор вправо в локальной системе корабля: (sin, -cos).
        float lateralSpeed = dx * MathUtils.sin(radians) - dy * MathUtils.cos(radians);
        float driftBend = maxSpeed > 0f ? lateralSpeed / maxSpeed * 0.42f : 0f;
        float turnBend = left ? 0.24f : (right ? -0.24f : 0f);
        float targetBend = MathUtils.clamp(driftBend + turnBend, -0.45f, 0.45f);
        plumeBend += (targetBend - plumeBend) * Math.min(1f, dt * 7f);

        // Плотность эффекта из настроек (используется и в эмиссии, и в отрисовке
        // привязанных факелов, чтобы настройка «0» полностью отключала двигатель)
        engineDensity = engineDensityFromSetting(com.gdx.utils.GameSettings.getShipEngineParticles());

        float throttleTarget = up && engineDensity > 0f ? 1f : 0f;
        float throttleResponse = throttleTarget > plumeThrottle ? 14f : 4.5f;
        plumeThrottle += (throttleTarget - plumeThrottle) * Math.min(1f, dt * throttleResponse);

        // Непрерывная мировая лента хранит инерционную историю обоих сопел.
        if (exhaustTrailRenderer != null) {
            exhaustTrailRenderer.update(dt, plumeThrottle > 0.08f && engineDensity > 0f,
                    engineX[0], engineY[0], engineX[2], engineY[2],
                    dx, dy, radians);
        }

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

        // Обновляем искры
        for(int i = sparkParticles.size - 1; i >= 0; i--) {
            SparkParticle particle = sparkParticles.get(i);
            particle.update(com.gdx.utils.TimeManager.getFixedStep());

            if(particle.shouldRemove()) {
                // Возвращаем частицу в пул
                ParticlePool.freeSparkParticle(particle);
                sparkParticles.removeIndex(i);
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

    /**
     * Интервал между эмиссиями со случайным разбросом ±INTERVAL_JITTER вокруг
     * среднего значения 1/rate. Средняя частота сохраняется, синхронность нет.
     */
    private float jitteredInterval(float rate) {
        return (1f / rate) * (1f - INTERVAL_JITTER + MathUtils.random() * 2f * INTERVAL_JITTER);
    }

    /**
     * Настройка 0-100: 8 остаётся нормальной плотностью 1x, а значения выше
     * продолжают плавно усиливать эффект вплоть до 4x на максимуме.
     */
    private float engineDensityFromSetting(int setting) {
        if (setting <= 8) return setting / 8f;
        return 1f + (setting - 8f) * (3f / 92f);
    }

    private void createFlameParticles(float dt) {
        if (!up) {
            return;
        }

        // Настройка «Ship Engine Particles» — плотность эффекта (8 = норма).
        // Значение кэшируется в engineDensity в update().
        float density = engineDensity;
        if (density <= 0f) {
            return;
        }

        // Точки эмиссии — два сопла из существующей геометрии корабля.
        // engineX/Y пересчитываются в setShape() при каждом сдвиге/повороте,
        // поэтому сопла корректно двигаются и поворачиваются вместе с кораблём.
        float leftX = engineX[0];
        float leftY = engineY[0];
        float rightX = engineX[2];
        float rightY = engineY[2];
        float backAngle = radians + MathUtils.PI; // назад от носа корабля

        // === Основная струя: отдельные накопители + интерполяция стартовых ===
        streamTimerLeft += dt * density;
        int nLeft = 0;
        while (streamTimerLeft >= streamNextLeft) {
            streamTimerLeft -= streamNextLeft;
            streamNextLeft = jitteredInterval(STREAM_RATE);
            nLeft++;
        }
        for (int k = 1; k <= nLeft; k++) {
            float f = nLeft > 1 ? k / (float)(nLeft + 1) : 1f;
            spawnStreamParticle(lastLeftX + (leftX - lastLeftX) * f,
                    lastLeftY + (leftY - lastLeftY) * f, backAngle, true);
        }

        streamTimerRight += dt * density;
        int nRight = 0;
        while (streamTimerRight >= streamNextRight) {
            streamTimerRight -= streamNextRight;
            streamNextRight = jitteredInterval(STREAM_RATE);
            nRight++;
        }
        for (int k = 1; k <= nRight; k++) {
            float f = nRight > 1 ? k / (float)(nRight + 1) : 1f;
            spawnStreamParticle(lastRightX + (rightX - lastRightX) * f,
                    lastRightY + (rightY - lastRightY) * f, backAngle, false);
        }

        // === Пламя у сопел: 25 частиц/сек на сопло при нормальной плотности ===
        flameTimerLeft += dt * density;
        int nFlameL = 0;
        while (flameTimerLeft >= flameNextLeft) {
            flameTimerLeft -= flameNextLeft;
            flameNextLeft = jitteredInterval(FLAME_RATE);
            nFlameL++;
        }
        for (int k = 1; k <= nFlameL; k++) {
            float f = nFlameL > 1 ? k / (float)(nFlameL + 1) : 1f;
            spawnFlameParticle(lastLeftX + (leftX - lastLeftX) * f,
                    lastLeftY + (leftY - lastLeftY) * f, backAngle);
        }

        flameTimerRight += dt * density;
        int nFlameR = 0;
        while (flameTimerRight >= flameNextRight) {
            flameTimerRight -= flameNextRight;
            flameNextRight = jitteredInterval(FLAME_RATE);
            nFlameR++;
        }
        for (int k = 1; k <= nFlameR; k++) {
            float f = nFlameR > 1 ? k / (float)(nFlameR + 1) : 1f;
            spawnFlameParticle(lastRightX + (rightX - lastRightX) * f,
                    lastRightY + (rightY - lastRightY) * f, backAngle);
        }

        // === Редкие искры (суммарно ~3-4/сек, случайно, без жёсткого ритма) ===
        if (MathUtils.random() < SPARK_RATE * dt * density) {
            boolean fromLeft = MathUtils.randomBoolean();
            spawnSparkParticle(fromLeft ? leftX : rightX, fromLeft ? leftY : rightY, backAngle);
        }

        // Позиции сопел на начало следующего шага
        lastLeftX = leftX; lastLeftY = leftY;
        lastRightX = rightX; lastRightY = rightY;
    }

    private void spawnStreamParticle(float nozzleX, float nozzleY, float backAngle, boolean fromLeft) {
        if (!com.gdx.utils.ParticleBudget.isInSpawnBounds(nozzleX, nozzleY)) {
            return;
        }
        if (!com.gdx.utils.ParticleBudget.canSpawn(nozzleX, nozzleY)) {
            return;
        }
        // Частица наследует скорость корабля. Без этого визуальная скорость
        // отрыва была равна скорости корабля + скорости струи и давала длинные
        // жёлтые штрихи далеко позади сопел.
        // Те же разброс и скорость, что у RocketParticle.
        float spreadAngle = backAngle + (MathUtils.random() - 0.5f) * 0.35f;
        float flameSpeed = 55 + MathUtils.random() * 30;
        // Боковая сходимость к центральной оси 20-35 ед./сек: левое сопло
        // сдвигается вправо, правое — влево. Через 20-40 px после корабля два
        // потока частично перекрываются, образуя общий объём шириной ~10-16 px,
        // но возле сопел два отдельных факела остаются читаемыми.
        // Право в локальной системе = (sin(radians), -cos(radians)).
        float lateral = 22f;
        float lx = (fromLeft ? 1f : -1f) * MathUtils.sin(radians);
        float ly = (fromLeft ? -1f : 1f) * MathUtils.cos(radians);
        FlameParticle particle = ParticlePool.obtainFlameParticle();
        particle.initWithVelocity(nozzleX, nozzleY,
                dx + MathUtils.cos(spreadAngle) * flameSpeed + lx * lateral,
                dy + MathUtils.sin(spreadAngle) * flameSpeed + ly * lateral);
        flameParticles.add(particle);
        com.gdx.utils.ParticleBudget.add(1);
    }

    private void spawnFlameParticle(float nozzleX, float nozzleY, float backAngle) {
        if (!com.gdx.utils.ParticleBudget.isInSpawnBounds(nozzleX, nozzleY)) {
            return;
        }
        if (!com.gdx.utils.ParticleBudget.canSpawn(nozzleX, nozzleY)) {
            return;
        }
        float spreadAngle = backAngle + (MathUtils.random() - 0.5f) * 0.22f;
        float flameSpeed = 35 + MathUtils.random() * 20;
        FlameParticle particle = ParticlePool.obtainFlameParticle();
        particle.initFlameWithVelocity(nozzleX, nozzleY,
                dx + MathUtils.cos(spreadAngle) * flameSpeed,
                dy + MathUtils.sin(spreadAngle) * flameSpeed);
        flameParticles.add(particle);
        com.gdx.utils.ParticleBudget.add(1);
    }

    private void spawnSparkParticle(float nozzleX, float nozzleY, float backAngle) {
        if (!com.gdx.utils.ParticleBudget.isInSpawnBounds(nozzleX, nozzleY)) {
            return;
        }
        if (!com.gdx.utils.ParticleBudget.canSpawn(nozzleX, nozzleY)) {
            return;
        }
        float spreadAngle = backAngle + (MathUtils.random() - 0.5f) * 0.44f; // ±12.6°
        float sparkSpeed = 100 + MathUtils.random() * 80; // 100-180 ед./сек
        SparkParticle particle = ParticlePool.obtainSparkParticle();
        particle.init(nozzleX, nozzleY, spreadAngle, sparkSpeed);
        sparkParticles.add(particle);
        com.gdx.utils.ParticleBudget.add(1);
    }

    public void drawHitLines(ShapeRenderer shapeRenderer, Camera camera) {
        if (!hit) return;
        shapeRenderer.setColor(1, 0.3f, 0.1f, 1);
        for (Line2D.Float line : hitLines) {
            shapeRenderer.line(camera.worldToScreenX(line.x1), camera.worldToScreenY(line.y1),
                    camera.worldToScreenX(line.x2), camera.worldToScreenY(line.y2));
        }
    }

    public void drawShip(SpriteBatch spriteBatch, Camera camera) {
        if (hit || shipTexture == null) return;
            float screenX = camera.worldToScreenX(x);
            float screenY = camera.worldToScreenY(y);
            float textureSize = 32 * camera.getCurrentZoom();
            if(isInvulnerable) {
                float alpha = 0.5f + MathUtils.sin(invulnerabilityTimer * 20f) * 0.5f;
                spriteBatch.setColor(1, 1, 1, alpha);
            } else {
                spriteBatch.setColor(1, 1, 1, 1);
            }
            spriteBatch.draw(
                shipTexture,
                screenX - textureSize/2,
                screenY - textureSize/2,
                textureSize/2, textureSize/2,
                textureSize, textureSize,
                1, 1,
                (radians * MathUtils.radiansToDegrees) + 270,
                0, 0,
                shipTexture.getWidth(), shipTexture.getHeight(),
                false, false
            );

            if(up) {
                spriteBatch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                float flameIntensity = 0.3f + MathUtils.sin(engineGlowTimer * 6f) * 0.1f;
                spriteBatch.setColor(1.0f, 0.4f, 0.1f, flameIntensity);
                float glowSize = textureSize * 1.2f;
                spriteBatch.draw(
                    shipTexture,
                    screenX - glowSize/2,
                    screenY - glowSize/2,
                    glowSize/2, glowSize/2,
                    glowSize, glowSize,
                    1, 1,
                    (radians * MathUtils.radiansToDegrees) + 270,
                    0, 0,
                    shipTexture.getWidth(), shipTexture.getHeight(),
                    false, false
                );
                spriteBatch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            }
            spriteBatch.setColor(1, 1, 1, 1);
    }

    public void drawEngineEffects(Camera camera) {
        if (hit) return;
        if (exhaustTrailRenderer != null) {
            exhaustTrailRenderer.render(camera);
        }
        boolean engineOn = plumeThrottle > 0.015f && engineDensity > 0f;
        if (engineOn && enginePlumeRenderer != null) {
            enginePlumeRenderer.advanceTime();
            float intensity = MathUtils.clamp(0.72f + engineDensity * 0.28f,
                    0.72f, 1.2f) * plumeThrottle;
            float length = 54f * (0.62f + 0.38f * plumeThrottle);
            float centerX = (engineX[0] + engineX[2]) * 0.5f;
            float centerY = (engineY[0] + engineY[2]) * 0.5f;
            enginePlumeRenderer.renderFlame(centerX, centerY, radians,
                    intensity * 0.06f, 74f, length * 1.02f, plumeBend, camera);
            enginePlumeRenderer.renderFlame(centerX, centerY, radians,
                    intensity * 0.24f, 50f, length * 1.05f, plumeBend, camera);
            enginePlumeRenderer.renderFlame(centerX, centerY, radians,
                    intensity, 32f, length, plumeBend, camera);
        }
    }

    public void drawEngineParticles(SpriteBatch spriteBatch, Camera camera) {
        if (hit) return;
        spriteBatch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        for(int i = flameParticles.size - 1; i >= 0; i--) {
            FlameParticle particle = flameParticles.get(i);
            if (camera.isInRenderBounds(particle.getX(), particle.getY(), particle.getCullRadius())) {
                particle.draw(spriteBatch, camera);
            }
        }
        spriteBatch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for(int i = sparkParticles.size - 1; i >= 0; i--) {
            SparkParticle particle = sparkParticles.get(i);
            if (camera.isInRenderBounds(particle.getX(), particle.getY(), particle.getCullRadius())) {
                particle.draw(spriteBatch, camera);
            }
        }
        spriteBatch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    public void drawShield(ShapeRenderer shapeRenderer, Camera camera) {
        if (hit) return;
        for(int i = shieldParticles.size - 1; i >= 0; i--) {
            ShieldParticle particle = shieldParticles.get(i);
            if (camera.isInRenderBounds(particle.getX(), particle.getY(), particle.getCullRadius())) {
                particle.draw(shapeRenderer, camera);
            }
        }
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
            for (FlameParticle particle : flameParticles) {
                ParticlePool.freeFlameParticle(particle);
            }
            com.gdx.utils.ParticleBudget.release(flameParticles.size);
            com.gdx.utils.ParticleBudget.unregister(flameParticles);
            flameParticles.clear();
        }

        // Очищаем синие частицы щита
        if (shieldParticles != null) {
            for (ShieldParticle particle : shieldParticles) {
                ParticlePool.freeShieldParticle(particle);
            }
            com.gdx.utils.ParticleBudget.release(shieldParticles.size);
            com.gdx.utils.ParticleBudget.unregister(shieldParticles);
            shieldParticles.clear();
        }

        // Очищаем искры
        if (sparkParticles != null) {
            for (SparkParticle particle : sparkParticles) {
                ParticlePool.freeSparkParticle(particle);
            }
            com.gdx.utils.ParticleBudget.release(sparkParticles.size);
            com.gdx.utils.ParticleBudget.unregister(sparkParticles);
            sparkParticles.clear();
        }

        // Освобождаем текстуру
        if (shipTexture != null) {
            shipTexture.dispose();
        }
        if (enginePlumeRenderer != null) {
            enginePlumeRenderer.dispose();
            enginePlumeRenderer = null;
        }
        if (exhaustTrailRenderer != null) {
            exhaustTrailRenderer.dispose();
            exhaustTrailRenderer = null;
        }
    }

    public Array<FlameParticle> getFlameParticles() {
        return flameParticles;
    }

    public Array<ShieldParticle> getShieldParticles() {
        return shieldParticles;
    }

    public Array<SparkParticle> getSparkParticles() {
        return sparkParticles;
    }

    public int getExhaustTrailSampleCount() {
        return exhaustTrailRenderer != null ? exhaustTrailRenderer.getSampleCount() : 0;
    }
}
