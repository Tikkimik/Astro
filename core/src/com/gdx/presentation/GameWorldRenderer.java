package com.gdx.presentation;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.gdx.entities.FlameParticle;
import com.gdx.entities.OptimizedParticle;
import com.gdx.entities.Particle;
import com.gdx.entities.Player;
import com.gdx.entities.RocketExplosionEffect;
import com.gdx.entities.ShieldParticle;
import com.gdx.entities.SparkParticle;
import com.gdx.managers.Camera;
import com.gdx.managers.GameObjectManager;
import com.gdx.utils.ParticleBudget;
import com.gdx.utils.RenderStats;

public final class GameWorldRenderer {

    private final GamePresentation presentation;
    private final Player player;
    private final GameObjectManager objects;
    private final RenderStats stats = new RenderStats();

    public GameWorldRenderer(GamePresentation presentation, Player player, GameObjectManager objects) {
        this.presentation = presentation;
        this.player = player;
        this.objects = objects;
    }

    public void draw(ShapeRenderer shapes, SpriteBatch batch) {
        Camera camera = presentation.camera();
        stats.beginFrame();

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        presentation.background().draw(shapes, camera);
        objects.draw(shapes, camera, batch, stats);
        for (RocketExplosionEffect effect : presentation.rocketExplosionEffects()) {
            effect.drawFlash(shapes, camera);
        }
        shapes.end();

        drawPlayerTextures(batch, camera);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        if (isPlayerVisible(camera)) player.drawShield(shapes, camera);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        if (isPlayerVisible(camera)) player.drawHitLines(shapes, camera);
        for (RocketExplosionEffect effect : presentation.rocketExplosionEffects()) {
            effect.drawRing(shapes, camera);
        }
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (Particle particle : presentation.particles()) {
            stats.particlesActive++;
            if (camera.isInRenderBounds(particle.getX(), particle.getY(), particle.getCullRadius())) {
                stats.particlesVisible++;
                particle.draw(shapes, camera);
                stats.particlesDrawn++;
            } else {
                stats.particlesCulled++;
            }
        }
        shapes.end();

        if (presentation.optimizedParticles().size > 0) {
            batch.begin();
            for (OptimizedParticle particle : presentation.optimizedParticles()) {
                stats.particlesActive++;
                if (camera.isInRenderBounds(particle.getX(), particle.getY(), particle.getCullRadius())) {
                    stats.particlesVisible++;
                    particle.draw(batch, camera);
                    stats.particlesDrawn++;
                } else {
                    stats.particlesCulled++;
                }
            }
            batch.end();
        }

        objects.drawRocketParticles(batch, camera, stats);
        objects.drawEnemySmoke(batch, camera, stats);
        ParticleBudget.setVisible(stats.particlesVisible);
    }

    private void drawPlayerTextures(SpriteBatch batch, Camera camera) {
        int flameCount = player.getFlameParticles().size;
        int shieldCount = player.getShieldParticles().size;
        int sparkCount = player.getSparkParticles().size;
        int exhaustCount = player.getExhaustTrailSampleCount();
        boolean visible = isPlayerVisible(camera);

        stats.particlesActive += flameCount + shieldCount + sparkCount + exhaustCount;
        if (visible) {
            batch.begin();
            player.drawShip(batch, camera);
            batch.end();
            player.drawEngineEffects(camera);
            batch.begin();
            player.drawEngineParticles(batch, camera);
            batch.end();
            stats.particlesVisible += exhaustCount;
            stats.particlesDrawn += exhaustCount;
            countPlayerParticles(camera, player.getFlameParticles(), player.getShieldParticles(),
                    player.getSparkParticles());
        } else {
            stats.particlesCulled += flameCount + shieldCount + sparkCount + exhaustCount;
        }

        stats.objectsActive = objects.getActiveObjects();
        stats.objectsVisible = stats.projectilesVisible + stats.obstaclesVisible
                + stats.enemiesVisible + (visible ? 1 : 0);
        stats.objectsDrawn = stats.projectilesDrawn + stats.obstaclesDrawn
                + stats.enemiesDrawn + (visible ? 1 : 0);
        stats.objectsCulled = stats.objectsActive - stats.objectsVisible;
    }

    private boolean isPlayerVisible(Camera camera) {
        return camera.isInRenderBounds(player.getX(), player.getY(),
                Math.max(1f, player.getWidth() / 2f));
    }

    private void countPlayerParticles(Camera camera,
                                      Iterable<FlameParticle> flames,
                                      Iterable<ShieldParticle> shields,
                                      Iterable<SparkParticle> sparks) {
        for (FlameParticle particle : flames) {
            countParticle(camera, particle.getX(), particle.getY(), particle.getCullRadius());
        }
        for (ShieldParticle particle : shields) {
            countParticle(camera, particle.getX(), particle.getY(), particle.getCullRadius());
        }
        for (SparkParticle particle : sparks) {
            countParticle(camera, particle.getX(), particle.getY(), particle.getCullRadius());
        }
    }

    private void countParticle(Camera camera, float x, float y, float radius) {
        if (camera.isInRenderBounds(x, y, radius)) {
            stats.particlesVisible++;
            stats.particlesDrawn++;
        } else {
            stats.particlesCulled++;
        }
    }

    public RenderStats stats() {
        return stats;
    }
}
