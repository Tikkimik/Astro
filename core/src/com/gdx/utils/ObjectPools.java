package com.gdx.utils;

import com.badlogic.gdx.utils.Pool;
import com.gdx.entities.Bullet;
import com.gdx.entities.Particle;
import com.gdx.entities.ExplosionParticle;
import com.gdx.entities.FlameParticle;
import com.gdx.entities.ShieldParticle;
import com.gdx.entities.RocketParticle;
import com.gdx.entities.OrkBullet;
import com.gdx.entities.AutoRocket;
import com.gdx.entities.Enemy;
import com.gdx.entities.OptimizedParticle;

public class ObjectPools {
    
    // Пулы для пуль
    public static final Pool<Bullet> bulletPool = new Pool<Bullet>(100) {
        @Override
        protected Bullet newObject() {
            return new Bullet(0, 0, 0);
        }
    };
    
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
    
    public static final Pool<FlameParticle> flameParticlePool = new Pool<FlameParticle>(50) {
        @Override
        protected FlameParticle newObject() {
            return new FlameParticle(0, 0, 0, 50);
        }
    };
    
    public static final Pool<ShieldParticle> shieldParticlePool = new Pool<ShieldParticle>(30) {
        @Override
        protected ShieldParticle newObject() {
            return new ShieldParticle(0, 0, 0, 20);
        }
    };
    
    public static final Pool<RocketParticle> rocketParticlePool = new Pool<RocketParticle>(80) {
        @Override
        protected RocketParticle newObject() {
            return new RocketParticle(0, 0, 0);
        }
    };
    
    // Пулы для вражеских объектов
    public static final Pool<OrkBullet> orkBulletPool = new Pool<OrkBullet>(50) {
        @Override
        protected OrkBullet newObject() {
            return new OrkBullet(0, 0, 0);
        }
    };
    
    public static final Pool<AutoRocket> autoRocketPool = new Pool<AutoRocket>(20) {
        @Override
        protected AutoRocket newObject() {
            return new AutoRocket(0, 0, 0, null);
        }
    };
    
    // Методы для получения объектов из пулов
    public static Bullet obtainBullet(float x, float y, float radians) {
        Bullet bullet = bulletPool.obtain();
        bullet.init(x, y, radians);
        return bullet;
    }
    
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
    
    public static FlameParticle obtainFlameParticle(float x, float y) {
        FlameParticle particle = flameParticlePool.obtain();
        particle.init(x, y);
        return particle;
    }
    
    public static ShieldParticle obtainShieldParticle(float x, float y) {
        ShieldParticle particle = shieldParticlePool.obtain();
        particle.init(x, y);
        return particle;
    }
    
    public static RocketParticle obtainRocketParticle(float x, float y, float angle) {
        RocketParticle particle = rocketParticlePool.obtain();
        particle.init(x, y);
        return particle;
    }
    
    public static OrkBullet obtainOrkBullet(float x, float y, float angle) {
        OrkBullet bullet = orkBulletPool.obtain();
        bullet.init(x, y, angle);
        return bullet;
    }
    
    public static AutoRocket obtainAutoRocket(float x, float y, float direction, Enemy target) {
        AutoRocket rocket = autoRocketPool.obtain();
        rocket.init(x, y, direction, target);
        return rocket;
    }
    
    public static OptimizedParticle obtainOptimizedParticle(float x, float y, String type) {
        OptimizedParticle particle = optimizedParticlePool.obtain();
        particle.init(x, y, type);
        return particle;
    }
    
    // Методы для возврата объектов в пулы
    public static void freeBullet(Bullet bullet) {
        bulletPool.free(bullet);
    }
    
    public static void freeParticle(Particle particle) {
        particlePool.free(particle);
    }
    
    public static void freeExplosionParticle(ExplosionParticle particle) {
        explosionParticlePool.free(particle);
    }
    
    public static void freeFlameParticle(FlameParticle particle) {
        flameParticlePool.free(particle);
    }
    
    public static void freeShieldParticle(ShieldParticle particle) {
        shieldParticlePool.free(particle);
    }
    
    public static void freeRocketParticle(RocketParticle particle) {
        rocketParticlePool.free(particle);
    }
    
    public static void freeOrkBullet(OrkBullet bullet) {
        orkBulletPool.free(bullet);
    }
    
    public static void freeAutoRocket(AutoRocket rocket) {
        autoRocketPool.free(rocket);
    }
    
    public static void freeOptimizedParticle(OptimizedParticle particle) {
        optimizedParticlePool.free(particle);
    }
}
