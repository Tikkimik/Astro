package com.gdx.utils;

/**
 * Единый счётчик активных частиц с глобальным лимитом.
 *
 * Настройка «Макс. частиц» (GameSettings.getMaxParticles) управляет суммарным
 * числом частиц ВСЕХ источников: обычные частицы и оптимизированные частицы
 * взрывов (PlayState), пламя и щит игрока, пламя вражеских кораблей, следы
 * ракет. Каждый источник «резервирует» слоты через canSpawn/add при создании
 * и освобождает их через release при удалении частицы.
 */
public final class ParticleBudget {

    private static int active = 0;

    private ParticleBudget() {
    }

    /**
     * Хватит ли бюджета ещё на {@code n} частиц.
     */
    public static boolean canSpawn(int n) {
        return active + n <= GameSettings.getMaxParticles();
    }

    /**
     * Зарезервировать {@code n} слотов под создаваемые частицы.
     */
    public static void add(int n) {
        active += n;
    }

    /**
     * Освободить {@code n} слотов при удалении частиц.
     */
    public static void release(int n) {
        active = Math.max(0, active - n);
    }

    /**
     * Число частиц, считающихся сейчас активными.
     */
    public static int getActive() {
        return active;
    }

    /**
     * Полный сброс счётчика (очистка мира).
     */
    public static void reset() {
        active = 0;
    }
}
