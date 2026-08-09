package com.gdx.utils;

/**
 * Переиспользуемый контейнер границ камеры в мировых координатах.
 * Заполняется один раз за кадр из Camera и передаётся в ParticleBudget,
 * чтобы не дёргать геттеры камеры при каждом спавне частицы.
 */
public final class CameraBounds {

    public float renderLeft, renderRight, renderBottom, renderTop;
    public float spawnLeft, spawnRight, spawnBottom, spawnTop;

    public void set(float rl, float rr, float rb, float rt,
                    float sl, float sr, float sb, float st) {
        renderLeft = rl;
        renderRight = rr;
        renderBottom = rb;
        renderTop = rt;
        spawnLeft = sl;
        spawnRight = sr;
        spawnBottom = sb;
        spawnTop = st;
    }
}
