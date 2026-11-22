package com.gdx.entities;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.gdx.managers.Camera;

public final class RocketExplosionEffect {

    private static final float DURATION = 0.35f;

    private final float x;
    private final float y;
    private final float radius;
    private float elapsed;

    public RocketExplosionEffect(float x, float y, float radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    public void update(float dt) {
        elapsed += dt;
    }

    public boolean isComplete() {
        return elapsed >= DURATION;
    }

    public void drawFlash(ShapeRenderer renderer, Camera camera) {
        float progress = MathUtils.clamp(elapsed / DURATION, 0f, 1f);
        float alpha = (1f - progress) * 0.7f;
        float flashRadius = (8f + 14f * progress) * camera.getCurrentZoom();
        renderer.setColor(1f, 0.65f, 0.12f, alpha);
        renderer.circle(camera.worldToScreenX(x), camera.worldToScreenY(y), flashRadius);
    }

    public void drawRing(ShapeRenderer renderer, Camera camera) {
        float progress = MathUtils.clamp(elapsed / DURATION, 0f, 1f);
        float expanded = Interpolation.circleOut.apply(progress);
        float ringRadius = radius * expanded * camera.getCurrentZoom();
        float alpha = 1f - progress;
        float screenX = camera.worldToScreenX(x);
        float screenY = camera.worldToScreenY(y);
        renderer.setColor(1f, 0.35f, 0.04f, alpha);
        renderer.circle(screenX, screenY, ringRadius);
        renderer.setColor(1f, 0.82f, 0.25f, alpha * 0.65f);
        renderer.circle(screenX, screenY, Math.max(0f, ringRadius - 2f));
    }
}
