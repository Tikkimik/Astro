package com.gdx.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.gdx.managers.Camera;
import com.gdx.managers.GameObjectManager;
import com.gdx.utils.ParticlePool;


public class AutoRocket extends SpaceObject implements GameObject.Updatable, GameObject.Drawable {

    private float initialSpeed = 33f;
    private float maxSpeed = 100f;
    private float currentSpeed = 33f;
    private float acceleration = 133f;
    private float lifeTime = 8f; // Увеличиваем время жизни
    private float lifeTimer = 0f;
    private boolean remove = false;

    // Фазы полета
    private float launchPhase = 0.5f; // Время начальной фазы (0.5 секунды)
    private float launchTimer = 0f;
    private boolean isLaunched = false;

    private Targetable target;

    private float lostTargetLifeTimer = 0f;
    private static final float LOST_TARGET_LIFETIME = 3f;

    public static final float DETECTION_RANGE = 1200f;

    // Урон ракеты
    private float damage = 2f;

    public static final float EXPLOSION_RADIUS = 60f;
    public static final int SPLASH_DAMAGE = 1;

    private GameObjectManager manager;

    // След-лента (ribbon) из прошлых позиций ракеты
    private static final int TRAIL_LENGTH = 42;
    private static final float TRAIL_WIDTH = 2.2f; // мировая ширина у головы
    private final Vector2[] trailHistory = new Vector2[TRAIL_LENGTH];
    private int trailSize;
    private int trailWriteIndex;
    private float trailParticleAccumulator;
    private static final float TRAIL_PARTICLES_PER_SECOND = 28f;

    {
        for (int i = 0; i < trailHistory.length; i++) {
            trailHistory[i] = new Vector2();
        }
    }

    public void setGameObjectManager(GameObjectManager manager) {
        this.manager = manager;
    }

    public AutoRocket(float x, float y, float rocketDirection, Targetable target) {
        init(x, y, rocketDirection, target);
    }

    public void init(float x, float y, float rocketDirection, Targetable target) {
        this.x = x;
        this.y = y;
        this.target = target;
        this.manager = null;

        this.radians = rocketDirection;
        this.currentSpeed = initialSpeed;
        this.dx = MathUtils.cos(radians) * currentSpeed;
        this.dy = MathUtils.sin(radians) * currentSpeed;

        lifeTimer = 0f;
        launchTimer = 0f;
        isLaunched = false;
        remove = false;
        lostTargetLifeTimer = 0f;
        damage = 2f;

        width = height = 3;

        trailSize = 1;
        trailWriteIndex = 1;
        trailParticleAccumulator = 0f;
        trailHistory[0].set(x, y);
    }

    public void update(float dt) {
        // Обновляем таймеры
        lifeTimer += dt;
        launchTimer += dt;

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
            }

            updateTarget();
            if (target == null) {
                lostTargetLifeTimer += dt;
                if (lostTargetLifeTimer >= LOST_TARGET_LIFETIME) {
                    remove = true;
                    return;
                }
            } else {
                lostTargetLifeTimer = 0f;
            }

            // Ускоряем ракету
            currentSpeed += acceleration * dt;
            if (currentSpeed > maxSpeed) {
                currentSpeed = maxSpeed;
            }

            // Сначала приводим вектор к текущей скорости, затем плавно доворачиваем.
            // Раньше currentSpeed дополнительно умножал уже ускоренный dx/dy, из-за
            // чего фактическая скорость росла квадратично и ракета улетала за экран.
            float currentAngle = MathUtils.atan2(dy, dx);
            dx = MathUtils.cos(currentAngle) * currentSpeed;
            dy = MathUtils.sin(currentAngle) * currentSpeed;
            recalculateTrajectory(dt);

            x += dx * dt;
            y += dy * dt;
        }

        // Проверяем границы мира
        wrap();

        // Записываем позицию в историю для след-ленты
        trailHistory[trailWriteIndex].set(x, y);
        trailWriteIndex = (trailWriteIndex + 1) % TRAIL_LENGTH;
        if (trailSize < TRAIL_LENGTH) trailSize++;

        // Создаем частицы следа, которые живут независимо от ракеты
        if (manager != null) {
            trailParticleAccumulator += TRAIL_PARTICLES_PER_SECOND * dt;
            while (trailParticleAccumulator >= 1f) {
                float trailAngle = (float) Math.atan2(dy, dx) + MathUtils.PI;
                spawnTrailParticle(trailAngle);
                trailParticleAccumulator -= 1f;
            }
        }
    }

    private void spawnTrailParticle(float trailAngle) {
        RocketParticle particle = ParticlePool.obtainRocketParticle();
        particle.init(x, y, trailAngle);
        manager.addRocketParticle(particle);
    }

    // Пересчет траектории для самонаведения
    private void recalculateTrajectory(float dt) {
        if (target == null) return;

        float targetDx = target.getX() - x;
        float targetDy = target.getY() - y;
        float distance = (float) Math.sqrt(targetDx * targetDx + targetDy * targetDy);

        if (distance > DETECTION_RANGE) {
            target = null;
            return;
        }

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
            float maxTurn = turnSpeed * dt; // Максимальный поворот за текущий тик

            if (Math.abs(angleDiff) > maxTurn) {
                angleDiff = Math.signum(angleDiff) * maxTurn;
            }

            // Обновляем направление
            float newAngle = currentAngle + angleDiff;
            dx = MathUtils.cos(newAngle) * currentSpeed;
            dy = MathUtils.sin(newAngle) * currentSpeed;
        }
    }

    private void updateTarget() {
        if (target != null && !target.shouldRemove()) {
            float targetDx = target.getX() - x;
            float targetDy = target.getY() - y;
            if (targetDx * targetDx + targetDy * targetDy <= DETECTION_RANGE * DETECTION_RANGE) return;
        }
        target = manager == null ? null : manager.findNearestTarget(x, y, DETECTION_RANGE);
    }

    public Targetable getTarget() {
        return target;
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

        // Нос ракеты
        float noseX = screenX + MathUtils.cos(direction) * rocketSize;
        float noseY = screenY + MathUtils.sin(direction) * rocketSize;

        // Боковые точки
        float leftX = screenX + MathUtils.cos(direction + 2.5f) * (rocketSize * 0.72f);
        float leftY = screenY + MathUtils.sin(direction + 2.5f) * (rocketSize * 0.72f);

        float rightX = screenX + MathUtils.cos(direction - 2.5f) * (rocketSize * 0.72f);
        float rightY = screenY + MathUtils.sin(direction - 2.5f) * (rocketSize * 0.72f);

        // Цвет зависит от фазы полета
        if (isLaunched) {
            // В фазе самонаведения - ярко-оранжевый
            shapeRenderer.setColor(1f, 0.28f, 0.02f, 1f);
        } else {
            // В начальной фазе - желтый
            shapeRenderer.setColor(1, 1, 0, 1);
        }

        shapeRenderer.triangle(noseX, noseY, leftX, leftY, rightX, rightY);

        // Яркое ядро делает маленькую ракету различимой на фоне следа.
        shapeRenderer.setColor(1f, 0.9f, 0.35f, 1f);
        shapeRenderer.circle(screenX, screenY, rocketSize * 0.28f);

        // В фазе самонаведения добавляем след-ленту из прошлых позиций
        if (isLaunched) {
            drawTrail(shapeRenderer, camera);
        }

    }

    // Рисуем след ракеты как ленту (ribbon) из прошлых позиций:
    // аддитивное свечение, ширина и прозрачность сужаются к хвосту.
    private void drawTrail(ShapeRenderer shapeRenderer, Camera camera) {
        if (trailSize < 2) {
            return;
        }

        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        float zoom = camera.getCurrentZoom();
        int last = trailSize - 1;
        int oldest = (trailWriteIndex - trailSize + TRAIL_LENGTH) % TRAIL_LENGTH;

        for (int i = last; i >= 1; i--) {
            Vector2 newer = trailHistory[(oldest + i) % TRAIL_LENGTH];
            Vector2 older = trailHistory[(oldest + i - 1) % TRAIL_LENGTH];

            // Доля от хвоста (0) к голове (1)
            float f = i / (float) last;

            float width = TRAIL_WIDTH * f * zoom;
            if (width < 0.3f) {
                continue;
            }

            float ax = camera.worldToScreenX(older.x);
            float ay = camera.worldToScreenY(older.y);
            float bx = camera.worldToScreenX(newer.x);
            float by = camera.worldToScreenY(newer.y);

            // Яркое бело-оранжевое ядро у головы, прозрачное к хвосту
            shapeRenderer.setColor(1f, 0.45f + 0.4f * f, 0.02f, 0.38f * f);
            shapeRenderer.rectLine(ax, ay, bx, by, width);
        }

        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    public boolean intersects(SpaceObject other) {
        float dx = x - other.getX();
        float dy = y - other.getY();
        float distSq = dx * dx + dy * dy;
        float radiusSum = width / 2f + other.getWidth() / 2f;
        return distSq < radiusSum * radiusSum;
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
