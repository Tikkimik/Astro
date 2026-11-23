package com.gdx.utils;

import com.badlogic.gdx.utils.Pool;
import com.gdx.entities.Particle;
import com.gdx.entities.ExplosionParticle;
import com.gdx.entities.AutoRocket;
import com.gdx.entities.Targetable;
import com.gdx.entities.OptimizedParticle;
import com.gdx.entities.SmokeParticle;

public class ObjectPools {

    // Пулы для частиц
    public static final Pool<Particle> particlePool = new Pool<Particle>(200) {
        @Override
        protected Particle newObject() {
            return new Particle(0, 0);
        }
    };

    // Пулы для оптимизированных частиц
    public static final Pool<OptimizedParticle> optimizedParticlePool = new Pool<OptimizedParticle>(300) {
        @Override
        protected OptimizedParticle newObject() {
            return new OptimizedParticle(0, 0, "simple");
        }
    };

    public static final Pool<ExplosionParticle> explosionParticlePool = new Pool<ExplosionParticle>(100) {
        @Override
        protected ExplosionParticle newObject() {
            return new ExplosionParticle(0, 0);
        }
    };

    // Пул дымовых частиц (выхлоп вражеских кораблей)
    public static final Pool<SmokeParticle> smokeParticlePool = new Pool<SmokeParticle>(150) {
        @Override
        protected SmokeParticle newObject() {
            return new SmokeParticle(0, 0, 0, 0);
        }
    };

    public static final Pool<AutoRocket> autoRocketPool = new Pool<AutoRocket>(20) {
        @Override
        protected AutoRocket newObject() {
            return new AutoRocket(0, 0, 0, null);
        }
    };

    // Методы для получения объектов из пулов
    public static Particle obtainParticle(float x, float y) {
        Particle particle = particlePool.obtain();
        particle.init(x, y);
        return particle;
    }

    public static ExplosionParticle obtainExplosionParticle(float x, float y) {
        ExplosionParticle particle = explosionParticlePool.obtain();
        particle.init(x, y);
        return particle;
    }

    public static AutoRocket obtainAutoRocket(float x, float y, float direction, Targetable target) {
        AutoRocket rocket = autoRocketPool.obtain();
        rocket.init(x, y, direction, target);
        return rocket;
    }

    public static OptimizedParticle obtainOptimizedParticle(float x, float y, String type) {
        OptimizedParticle particle = optimizedParticlePool.obtain();
        particle.init(x, y, type);
        return particle;
    }

    public static SmokeParticle obtainSmokeParticle(float x, float y, float angle, float speed) {
        SmokeParticle particle = smokeParticlePool.obtain();
        particle.init(x, y, angle, speed);
        return particle;
    }

    // Методы для возврата объектов в пулы
    public static void freeParticle(Particle particle) {
        if (particle instanceof ExplosionParticle explosionParticle) {
            explosionParticlePool.free(explosionParticle);
        } else {
            particlePool.free(particle);
        }
    }

    public static void freeExplosionParticle(ExplosionParticle particle) {
        explosionParticlePool.free(particle);
    }

    public static void freeAutoRocket(AutoRocket rocket) {
        autoRocketPool.free(rocket);
    }

    public static void freeOptimizedParticle(OptimizedParticle particle) {
        optimizedParticlePool.free(particle);
    }

    public static void freeSmokeParticle(SmokeParticle particle) {
        smokeParticlePool.free(particle);
    }

    public static void clearAllPools() {
        particlePool.clear();
        optimizedParticlePool.clear();
        explosionParticlePool.clear();
        smokeParticlePool.clear();
        autoRocketPool.clear();
    }
}
