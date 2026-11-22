package com.gdx.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.gdx.managers.Camera;
import com.gdx.utils.ParticleTextures;

public class FlameParticle extends Particle {

    // Режимы частицы двигателя
    public static final int MODE_FLAME = 0;  // короткое яркое пламя у сопла
    public static final int MODE_STREAM = 1; // основная направленная струя

    private float life;
    private float maxLife;
    private float size;
    private float maxSize;
    private float speed;
    private float angle;

    // Вытянутая форма: частица рисуется не кругом, а штрихом/языком пламени
    // вдоль вектора скорости. width — поперёк, length — вдоль движения.
    private float width;
    private float length;
    private float maxWidth;
    private float maxLength;

    private int mode = MODE_STREAM;
    private float pulsePhase; // фаза синусоидальной пульсации (пламя у сопла)

    public FlameParticle(float x, float y, float angle, float speed) {
        super(x, y);
        init(x, y, angle, speed);
    }

    // Метод инициализации для пула объектов (по умолчанию — струя)
    public void init(float x, float y, float angle, float speed) {
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.speed = speed;
        this.mode = MODE_STREAM;
        this.pulsePhase = MathUtils.random() * MathUtils.PI2;
        // Остаточный газ живёт достаточно долго, чтобы после отпускания тяги
        // читалась короткая история траектории.
        this.maxLife = 0.40f + MathUtils.random() * 0.22f;
        this.life = maxLife;
        this.maxSize = 5f + MathUtils.random() * 3f;
        this.size = maxSize;

        // Штрих струи: узкий и короткий, к хвосту сужается до ~30%.
        // Ширина в начале 2.5-4, длина 6-10 (в мировых единицах).
        this.maxWidth = maxSize;
        this.maxLength = maxSize;
        this.width = maxWidth;
        this.length = maxLength;

        // Начальная скорость частицы
        this.dx = MathUtils.cos(angle) * speed;
        this.dy = MathUtils.sin(angle) * speed;
    }

    // Метод инициализации для пула объектов (переопределение базового)
    public void init(float x, float y) {
        init(x, y, MathUtils.random(2 * MathUtils.PI), 50 + MathUtils.random(50));
    }

    /**
     * Инициализация с готовым вектором скорости. Используется для струи,
     * чтобы добавить к основному движению назад боковую сходимость к центру.
     */
    public void initWithVelocity(float x, float y, float vx, float vy) {
        init(x, y, MathUtils.atan2(vy, vx), (float) Math.sqrt(vx * vx + vy * vy));
        this.dx = vx;
        this.dy = vy;
    }

    /**
     * Пламя у сопла: короткое, яркое, почти не отклоняется в стороны,
     * слегка пульсирует (синусоида, ±10-15%, без случайных рывков).
     */
    public void initFlame(float x, float y, float angle, float speed) {
        init(x, y, angle, speed);
        this.mode = MODE_FLAME;
        this.maxLife = 0.20f + MathUtils.random() * 0.12f;
        this.life = maxLife;
        this.maxSize = 3.5f + MathUtils.random() * 2f;
        this.size = maxSize;
        this.maxWidth = maxSize;
        this.maxLength = maxSize;
        this.width = maxWidth;
        this.length = maxLength;
        this.dx = MathUtils.cos(angle) * speed;
        this.dy = MathUtils.sin(angle) * speed;
    }

    /** Инициализация короткого язычка с учётом скорости корабля. */
    public void initFlameWithVelocity(float x, float y, float vx, float vy) {
        initFlame(x, y, MathUtils.atan2(vy, vx), (float) Math.sqrt(vx * vx + vy * vy));
        this.dx = vx;
        this.dy = vy;
    }

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);

        // Уменьшаем жизнь частицы
        life -= deltaTime;

        float lifeRatio = MathUtils.clamp(life / maxLife, 0f, 1f);

        // То же плавное торможение, что у RocketParticle.
        dx *= 0.995f;
        dy *= 0.995f;
        size = maxSize;
        width = maxWidth;
        length = maxLength;
    }

    @Override
    public void draw(ShapeRenderer shapeRenderer, Camera camera) {
        if (life <= 0) return;

        // Цвет частицы зависит от оставшейся жизни
        float lifeRatio = life / maxLife;

        // Красивые цвета огня - от ярко-оранжевого к красному
        float r = 1.0f;
        float g = 0.2f + lifeRatio * 0.6f; // От красного к оранжевому
        float b = lifeRatio * 0.1f; // Немного голубого для реалистичности

        shapeRenderer.setColor(r, g, b, lifeRatio);

        float screenX = camera.worldToScreenX(x);
        float screenY = camera.worldToScreenY(y);

        // Масштабируем размер частицы в зависимости от зума камеры
        float scaledSize = size * camera.getCurrentZoom();

        // Рисуем красивый огненный след - несколько кругов разного размера
        shapeRenderer.circle(screenX, screenY, scaledSize);

        // Внутренний яркий центр
        shapeRenderer.setColor(1.0f, 0.8f, 0.3f, lifeRatio * 0.7f);
        shapeRenderer.circle(screenX, screenY, scaledSize * 0.6f);

        // Самый яркий центр
        shapeRenderer.setColor(1.0f, 1.0f, 0.5f, lifeRatio * 0.5f);
        shapeRenderer.circle(screenX, screenY, scaledSize * 0.3f);
    }

    @Override
    public boolean shouldRemove() {
        return life <= 0;
    }

    /**
     * Рисование через SpriteBatch: вытянутый мягкий «язык пламени» (штрих)
     * вдоль вектора скорости частицы, а не круг. Аддитивное смешивание
     * включается вызывающим кодом. Цвет — температурный градиент по времени
     * жизни (белый → жёлтый → оранжевый → тёмный красно-оранжевый), альфа
     * убывает к хвосту, ширина/длина уменьшаются.
     */
    public void draw(SpriteBatch batch, Camera camera) {
        if (life <= 0) return;

        float lifeRatio = MathUtils.clamp(life / maxLife, 0f, 1f);

        // У сопла газ тёплый, затем быстро остывает до нейтрального
        // серо-голубого. Обычное смешивание не превращает старые частицы в
        // зелёные светящиеся точки.
        float r = 0.34f + 0.66f * lifeRatio;
        float g = 0.40f + 0.22f * lifeRatio;
        float b = 0.48f - 0.24f * lifeRatio;
        float born = MathUtils.clamp((1f - lifeRatio) * 10f, 0f, 1f);
        float alpha = (mode == MODE_FLAME ? 0.52f : 0.42f)
                * born * (float) Math.pow(lifeRatio, 1.25f);

        float screenX = camera.worldToScreenX(x);
        float screenY = camera.worldToScreenY(y);
        float age = 1f - lifeRatio;
        float particleSize = size * (0.78f + age * 0.55f) * camera.getCurrentZoom();

        Texture texture = ParticleTextures.getInstance().getParticleTexture("smoke");
        batch.setColor(r, g, b, alpha);
        batch.draw(texture,
                screenX - particleSize / 2f, screenY - particleSize / 2f,
                particleSize, particleSize);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    @Override
    public float getCullRadius() {
        return maxSize * 0.75f + 1f;
    }
}
