package com.gdx.utils;

/**
 * Честные счётчики производительности главного цикла.
 *
 * Оба показателя измеряются в одном скользящем окне реального времени:
 *   renderFps     — сколько кадров реально отрисовано за окно;
 *   simulationUps — сколько фиксированных тиков игровой логики реально выполнено за окно.
 *
 * Счётчик тиков увеличивается в том же месте, где выполняется фиксированное
 * обновление игровой логики (главный цикл в MyGdxGame.render), а не из настроек.
 * Если за кадр выполняется два тика, счётчик увеличивается на два; если ни одного —
 * не увеличивается вовсе. Внутри каждого тика новые объекты не создаются.
 *
 * Пауза: во время паузы игровая логика не выполняется, поэтому simulationUps
 * естественно опускается до 0, а окно измерения продолжает обновляться по
 * реальному времени — после снятия паузы ложного огромного значения нет.
 */
public class PerformanceMetrics {

    /** Длительность окна измерения в секундах. */
    private static final float WINDOW_SECONDS = 1.0f;

    // Накопители внутри текущего окна
    private static int renderFrameCount = 0;
    private static int simulationTickCount = 0;

    // Момент начала окна в наносекундах (0 = окно ещё не начато)
    private static long windowStartNanos = 0L;

    // Вычисленные значения за завершённое окно
    private static float renderFps = 0f;
    private static float simulationUps = 0f;

    // Сколько фиксированных тиков было выполнено в последнем кадре рендера
    private static int lastTicksPerFrame = 0;

    private PerformanceMetrics() {
    }

    /**
     * Зафиксировать один отрисованный кадр рендера и количество выполненных
     * в нём фиксированных тиков игровой логики. Вызывается один раз за кадр.
     */
    public static void onRenderFrame(int simulationTicksThisFrame) {
        long now = System.nanoTime();
        if (windowStartNanos == 0L) {
            // Первый кадр: только запускаем окно, чтобы не делить на нулевое время
            windowStartNanos = now;
            renderFrameCount = 0;
            simulationTickCount = 0;
            lastTicksPerFrame = simulationTicksThisFrame;
            return;
        }

        renderFrameCount++;
        simulationTickCount += simulationTicksThisFrame;
        lastTicksPerFrame = simulationTicksThisFrame;

        float elapsedSec = (now - windowStartNanos) / 1_000_000_000.0f;
        if (elapsedSec >= WINDOW_SECONDS) {
            renderFps = renderFrameCount / elapsedSec;
            simulationUps = simulationTickCount / elapsedSec;
            renderFrameCount = 0;
            simulationTickCount = 0;
            windowStartNanos = now;
        }
    }

    /** Фактически отрисованных кадров в секунду за завершённое окно. */
    public static float getRenderFps() {
        return renderFps;
    }

    /** Фактически выполненных фиксированных тиков в секунду за завершённое окно. */
    public static float getSimulationUps() {
        return simulationUps;
    }

    /** Тиков игровой логики, выполненных в последнем кадре рендера. */
    public static int getTicksPerFrame() {
        return lastTicksPerFrame;
    }
}
