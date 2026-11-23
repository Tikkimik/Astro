package com.gdx.presentation;

import com.badlogic.gdx.utils.Array;
import com.gdx.entities.Background;
import com.gdx.entities.OptimizedParticle;
import com.gdx.entities.Particle;
import com.gdx.entities.Player;
import com.gdx.entities.RocketExplosionEffect;
import com.gdx.managers.Camera;
import com.gdx.utils.CameraBounds;
import com.gdx.utils.ObjectPools;
import com.gdx.utils.ParticleBudget;

public final class GamePresentation {

    public record ViewTimings(long cameraNanos, long backgroundNanos) {}

    private final Camera camera = new Camera();
    private final Background background = new Background(camera);
    private final CameraBounds cameraBounds = new CameraBounds();
    private final Array<Particle> particles = new Array<>();
    private final Array<OptimizedParticle> optimizedParticles = new Array<>();
    private final Array<RocketExplosionEffect> rocketExplosionEffects = new Array<>();

    public GamePresentation() {
        ParticleBudget.register(particles);
        ParticleBudget.register(optimizedParticles);
    }

    public ViewTimings updateView(float delta, Player player) {
        long started = System.nanoTime();
        camera.followTarget(player.getX(), player.getY());
        camera.update(delta);
        cameraBounds.set(camera.getRenderLeft(), camera.getRenderRight(),
                camera.getRenderBottom(), camera.getRenderTop(), camera.getSpawnLeft(),
                camera.getSpawnRight(), camera.getSpawnBottom(), camera.getSpawnTop());
        ParticleBudget.updateBounds(cameraBounds);
        long cameraNanos = System.nanoTime() - started;

        started = System.nanoTime();
        background.update(delta);
        long backgroundNanos = System.nanoTime() - started;

        return new ViewTimings(cameraNanos, backgroundNanos);
    }

    public long updateEffects(float delta) {
        long started = System.nanoTime();
        for (int i = 0; i < particles.size; i++) {
            Particle particle = particles.get(i);
            particle.update(delta);
            if (particle.shouldRemove()) {
                ObjectPools.freeParticle(particle);
                particles.removeIndex(i);
                ParticleBudget.release(1);
                i--;
            }
        }
        for (int i = 0; i < optimizedParticles.size; i++) {
            OptimizedParticle particle = optimizedParticles.get(i);
            particle.update(delta);
            if (particle.shouldRemove()) {
                ObjectPools.freeOptimizedParticle(particle);
                optimizedParticles.removeIndex(i);
                ParticleBudget.release(1);
                i--;
            }
        }
        for (int i = rocketExplosionEffects.size - 1; i >= 0; i--) {
            RocketExplosionEffect effect = rocketExplosionEffects.get(i);
            effect.update(delta);
            if (effect.isComplete()) rocketExplosionEffects.removeIndex(i);
        }
        return System.nanoTime() - started;
    }

    public void dispose() {
        for (Particle particle : particles) {
            ObjectPools.freeParticle(particle);
        }
        for (OptimizedParticle particle : optimizedParticles) {
            ObjectPools.freeOptimizedParticle(particle);
        }
        ParticleBudget.release(optimizedParticles.size + particles.size);
        ParticleBudget.unregister(optimizedParticles);
        ParticleBudget.unregister(particles);
        optimizedParticles.clear();
        particles.clear();
        rocketExplosionEffects.clear();
    }

    public Camera camera() { return camera; }
    public Background background() { return background; }
    public Array<Particle> particles() { return particles; }
    public Array<OptimizedParticle> optimizedParticles() { return optimizedParticles; }
    public Array<RocketExplosionEffect> rocketExplosionEffects() { return rocketExplosionEffects; }
}
