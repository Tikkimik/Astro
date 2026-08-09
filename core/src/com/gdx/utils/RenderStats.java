package com.gdx.utils;

/**
 * Счётчики одного render-кадра для отладки отсечения (culling).
 *
 * Сбрасываются в начале каждого кадра отрисовки и заполняются непосредственно
 * в местах вызова draw: active — живые объекты/частицы в коллекциях,
 * visible — пересекающие расширенную область камеры, drawn — реально переданные
 * в renderer, culled — пропущенные из-за нахождения вне камеры.
 *
 * Инварианты для каждой категории: active = visible + culled, drawn &lt;= visible.
 */
public final class RenderStats {

    public int objectsActive, objectsVisible, objectsDrawn, objectsCulled;
    public int projectilesActive, projectilesVisible, projectilesDrawn, projectilesCulled;
    public int obstaclesActive, obstaclesVisible, obstaclesDrawn, obstaclesCulled;
    public int enemiesActive, enemiesVisible, enemiesDrawn, enemiesCulled;
    public int particlesActive, particlesVisible, particlesDrawn, particlesCulled;

    public void beginFrame() {
        objectsActive = 0; objectsVisible = 0; objectsDrawn = 0; objectsCulled = 0;
        projectilesActive = 0; projectilesVisible = 0; projectilesDrawn = 0; projectilesCulled = 0;
        obstaclesActive = 0; obstaclesVisible = 0; obstaclesDrawn = 0; obstaclesCulled = 0;
        enemiesActive = 0; enemiesVisible = 0; enemiesDrawn = 0; enemiesCulled = 0;
        particlesActive = 0; particlesVisible = 0; particlesDrawn = 0; particlesCulled = 0;
    }
}
