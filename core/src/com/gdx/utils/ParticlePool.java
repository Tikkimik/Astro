package com.gdx.utils;

import com.badlogic.gdx.utils.Array;
import com.gdx.entities.FlameParticle;
import com.gdx.entities.ShieldParticle;
import com.gdx.entities.ExplosionParticle;
import com.gdx.entities.RocketParticle;
import com.gdx.entities.EnemyFlameParticle;
import com.gdx.game.MyGdxGame;

/**
 * Пул для переиспользования частиц и избежания создания новых объектов
 */
public class ParticlePool {
    
    // Размеры пулов для разных типов частиц
    private static final int FLAME_POOL_SIZE = 200;
    private static final int SHIELD_POOL_SIZE = 100;
    private static final int EXPLOSION_POOL_SIZE = 150;
    private static final int ROCKET_POOL_SIZE = 50;
    private static final int ENEMY_FLAME_POOL_SIZE = 150;
    
    // Пул для частиц огня
    private static final Array<FlameParticle> flamePool = new Array<>(FLAME_POOL_SIZE);
    private static final Array<ShieldParticle> shieldPool = new Array<>(SHIELD_POOL_SIZE);
    private static final Array<ExplosionParticle> explosionPool = new Array<>(EXPLOSION_POOL_SIZE);
    private static final Array<RocketParticle> rocketPool = new Array<>(ROCKET_POOL_SIZE);
    private static final Array<EnemyFlameParticle> enemyFlamePool = new Array<>(ENEMY_FLAME_POOL_SIZE);
    
    // Инициализируем пулы при первом использовании
    private static boolean poolsInitialized = false;
    
    static {
        initializePools();
    }
    
    /**
     * Инициализируем пулы частицами
     */
    private static void initializePools() {
        if (poolsInitialized) return;
        
        // Заполняем пул частиц огня
        for (int i = 0; i < FLAME_POOL_SIZE; i++) {
            flamePool.add(new FlameParticle(0, 0, 0, 0));
        }
        
        // Заполняем пул частиц щита
        for (int i = 0; i < SHIELD_POOL_SIZE; i++) {
            shieldPool.add(new ShieldParticle(0, 0, 0, 0));
        }
        
        // Заполняем пул частиц взрыва
        for (int i = 0; i < EXPLOSION_POOL_SIZE; i++) {
            explosionPool.add(new ExplosionParticle(0, 0));
        }
        
        // Заполняем пул частиц ракет
        for (int i = 0; i < ROCKET_POOL_SIZE; i++) {
            rocketPool.add(new RocketParticle(0, 0, 0));
        }
        
        // Заполняем пул частиц пламени вражеских кораблей
        for (int i = 0; i < ENEMY_FLAME_POOL_SIZE; i++) {
            enemyFlamePool.add(new EnemyFlameParticle(0, 0, 0, 0));
        }
        
        poolsInitialized = true;
        if (MyGdxGame.DEBUG_MODE) {
            System.out.println("ParticlePool: Пул частиц инициализирован");
        }
    }
    
    /**
     * Получить частицу огня из пула
     */
    public static FlameParticle obtainFlameParticle() {
        if (flamePool.size > 0) {
            PerformanceMonitor.incrementParticlesReused();
            return flamePool.pop();
        }
        // Если пул пуст, создаем новую частицу
        PerformanceMonitor.incrementParticlesCreated();
        return new FlameParticle(0, 0, 0, 0);
    }
    
    /**
     * Получить частицу щита из пула
     */
    public static ShieldParticle obtainShieldParticle() {
        if (shieldPool.size > 0) {
            return shieldPool.pop();
        }
        return new ShieldParticle(0, 0, 0, 0);
    }
    
    /**
     * Получить частицу взрыва из пула
     */
    public static ExplosionParticle obtainExplosionParticle() {
        if (explosionPool.size > 0) {
            return explosionPool.pop();
        }
        return new ExplosionParticle(0, 0);
    }
    
    /**
     * Получить частицу ракеты из пула
     */
    public static RocketParticle obtainRocketParticle() {
        if (rocketPool.size > 0) {
            return rocketPool.pop();
        }
        return new RocketParticle(0, 0, 0);
    }
    
    /**
     * Получить частицу пламени вражеского корабля из пула
     */
    public static EnemyFlameParticle obtainEnemyFlameParticle() {
        if (enemyFlamePool.size > 0) {
            return enemyFlamePool.pop();
        }
        return new EnemyFlameParticle(0, 0, 0, 0);
    }
    
    /**
     * Вернуть частицу огня в пул
     */
    public static void freeFlameParticle(FlameParticle particle) {
        if (particle != null && flamePool.size < FLAME_POOL_SIZE) {
            particle.init(0, 0, 0, 0);
            flamePool.add(particle);
        }
    }
    
    /**
     * Вернуть частицу щита в пул
     */
    public static void freeShieldParticle(ShieldParticle particle) {
        if (particle != null && shieldPool.size < SHIELD_POOL_SIZE) {
            particle.init(0, 0, 0, 0);
            shieldPool.add(particle);
        }
    }
    
    /**
     * Вернуть частицу взрыва в пул
     */
    public static void freeExplosionParticle(ExplosionParticle particle) {
        if (particle != null && explosionPool.size < EXPLOSION_POOL_SIZE) {
            particle.init(0, 0);
            explosionPool.add(particle);
        }
    }
    
    /**
     * Вернуть частицу ракеты в пул
     */
    public static void freeRocketParticle(RocketParticle particle) {
        if (particle != null && rocketPool.size < ROCKET_POOL_SIZE) {
            particle.init(0, 0, 0);
            rocketPool.add(particle);
        }
    }
    
    /**
     * Вернуть частицу пламени вражеского корабля в пул
     */
    public static void freeEnemyFlameParticle(EnemyFlameParticle particle) {
        if (particle != null && enemyFlamePool.size < ENEMY_FLAME_POOL_SIZE) {
            particle.init(0, 0, 0, 0);
            enemyFlamePool.add(particle);
        }
    }
    
    /**
     * Получить статистику пулов
     */
    public static String getPoolStats() {
        return String.format("Flame: %d/%d | Shield: %d/%d | Explosion: %d/%d | Rocket: %d/%d | EnemyFlame: %d/%d",
            flamePool.size, FLAME_POOL_SIZE,
            shieldPool.size, SHIELD_POOL_SIZE,
            explosionPool.size, EXPLOSION_POOL_SIZE,
            rocketPool.size, ROCKET_POOL_SIZE,
            enemyFlamePool.size, ENEMY_FLAME_POOL_SIZE);
    }
    
    /**
     * Очистить все пулы (вызывать только при завершении игры)
     */
    public static void clearAllPools() {
        flamePool.clear();
        shieldPool.clear();
        explosionPool.clear();
        rocketPool.clear();
        enemyFlamePool.clear();
        poolsInitialized = false;
    }
}
