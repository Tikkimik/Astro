package com.gdx.managers;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.gdx.game.MyGdxGame;

public class Camera {
    
    private OrthographicCamera camera;
    private float targetX, targetY;
    private float cameraX, cameraY;
    private float smoothness = 0.1f; // Плавность следования камеры
    
    // Система зума
    private float currentZoom = 1.0f;
    private float targetZoom = 1.0f;
    private float zoomSmoothness = 0.05f;
    private float minZoom = 0.5f; // Максимальное приближение
    private float maxZoom = 2.0f; // Максимальное отдаление
    
    // Границы мира (больше чем экран)
    private float worldWidth;
    private float worldHeight;

    // Видимая область камеры в мировых координатах (пересчитывается в update).
    // renderBounds — расширенная область отрисовки (culling): объект, центр которого
    // внутри неё, гарантированно виден или вот-вот появится на экране (margin 20%).
    // spawnBounds — ещё более широкая область появления декоративных частиц (margin 40%):
    // эмиттер внутри неё успевает просуществовать до появления объекта на экране.
    private float renderLeft, renderRight, renderBottom, renderTop;
    private float spawnLeft, spawnRight, spawnBottom, spawnTop;

    private static final float RENDER_MARGIN = 0.2f; // 10% от полной ширины экрана в каждую сторону
    private static final float SPAWN_MARGIN = 0.8f;  // 40% от полной ширины экрана в каждую сторону
    
    public Camera() {
        camera = new OrthographicCamera(MyGdxGame.WIDTH, MyGdxGame.HEIGHT);
        cameraX = MyGdxGame.WIDTH / 2f;
        cameraY = MyGdxGame.HEIGHT / 2f;
        targetX = cameraX;
        targetY = cameraY;
        
        // Размер мира в 4 раза больше экрана
        worldWidth = MyGdxGame.WIDTH * 4;
        worldHeight = MyGdxGame.HEIGHT * 4;

        updateCamera();
        computeBounds();
    }
    
    public void followTarget(float targetX, float targetY) {
        this.targetX = targetX;
        this.targetY = targetY;
    }
    
    public void update(float dt) {
        // Плавное следование за целью
        cameraX += (targetX - cameraX) * smoothness;
        cameraY += (targetY - cameraY) * smoothness;
        
        // Плавное изменение зума
        currentZoom += (targetZoom - currentZoom) * zoomSmoothness;
        
        // Убираем ограничения камеры для бесконечного мира
        // Камера может следовать за игроком в любом направлении

        updateCamera();
        computeBounds();
    }
    
    private void updateCamera() {
        camera.position.set(cameraX, cameraY, 0);
        camera.zoom = currentZoom;
        camera.update();
    }

    private void computeBounds() {
        // Фактическая видимая область: worldToScreenX = (wx - cameraX) * zoom + WIDTH/2,
        // поэтому на экране помещается (WIDTH/2)/zoom мировых единиц от центра камеры.
        float halfWidth = (MyGdxGame.WIDTH * 0.5f) / currentZoom;
        float halfHeight = (MyGdxGame.HEIGHT * 0.5f) / currentZoom;

        float renderExtX = halfWidth * (1f + RENDER_MARGIN);
        float renderExtY = halfHeight * (1f + RENDER_MARGIN);
        renderLeft = cameraX - renderExtX;
        renderRight = cameraX + renderExtX;
        renderBottom = cameraY - renderExtY;
        renderTop = cameraY + renderExtY;

        float spawnExtX = halfWidth * (1f + SPAWN_MARGIN);
        float spawnExtY = halfHeight * (1f + SPAWN_MARGIN);
        spawnLeft = cameraX - spawnExtX;
        spawnRight = cameraX + spawnExtX;
        spawnBottom = cameraY - spawnExtY;
        spawnTop = cameraY + spawnExtY;
    }
    
    public OrthographicCamera getCamera() {
        return camera;
    }
    
    public float getWorldWidth() {
        return worldWidth;
    }
    
    public float getWorldHeight() {
        return worldHeight;
    }
    
    public float getCameraX() {
        return cameraX;
    }
    
    public float getCameraY() {
        return cameraY;
    }
    
    // Методы для управления зумом
    public void setTargetZoom(float zoom) {
        targetZoom = MathUtils.clamp(zoom, minZoom, maxZoom);
    }
    
    public void zoomIn() {
        setTargetZoom(targetZoom * 0.8f);
    }
    
    public void zoomOut() {
        setTargetZoom(targetZoom * 1.2f);
    }
    
    public void resetZoom() {
        setTargetZoom(1.0f);
    }
    
    public float getCurrentZoom() {
        return currentZoom;
    }
    
    public float getTargetZoom() {
        return targetZoom;
    }
    
    // Преобразование мировых координат в экранные с учетом зума
    public float worldToScreenX(float worldX) {
        return (worldX - cameraX) * currentZoom + MyGdxGame.WIDTH / 2f;
    }
    
    public float worldToScreenY(float worldY) {
        return (worldY - cameraY) * currentZoom + MyGdxGame.HEIGHT / 2f;
    }
    
    // Проверка, находится ли объект в видимой области с учетом зума
    public boolean isInView(float x, float y, float radius) {
        float screenX = worldToScreenX(x);
        float screenY = worldToScreenY(y);
        float scaledRadius = radius * currentZoom;
        
        return screenX + scaledRadius >= 0 && 
               screenX - scaledRadius <= MyGdxGame.WIDTH && 
               screenY + scaledRadius >= 0 && 
               screenY - scaledRadius <= MyGdxGame.HEIGHT;
    }

    // === Границы камеры для culling ===

    /**
     * Пересекает ли круг с центром (x, y) и радиусом radius расширенную
     * область отрисовки камеры (render bounds). Центр может быть за границей,
     * но сама фигура — всё ещё видима: проверяется по полуосям, без аллокаций.
     */
    public boolean isInRenderBounds(float x, float y, float radius) {
        return x + radius >= renderLeft && x - radius <= renderRight
            && y + radius >= renderBottom && y - radius <= renderTop;
    }

    public boolean isInRenderBounds(float x, float y) {
        return isInRenderBounds(x, y, 0f);
    }

    /**
     * Находится ли точка внутри расширенной области появления частиц (spawn bounds).
     */
    public boolean isInSpawnBounds(float x, float y) {
        return x >= spawnLeft && x <= spawnRight
            && y >= spawnBottom && y <= spawnTop;
    }

    public float getRenderLeft() { return renderLeft; }
    public float getRenderRight() { return renderRight; }
    public float getRenderBottom() { return renderBottom; }
    public float getRenderTop() { return renderTop; }

    public float getSpawnLeft() { return spawnLeft; }
    public float getSpawnRight() { return spawnRight; }
    public float getSpawnBottom() { return spawnBottom; }
    public float getSpawnTop() { return spawnTop; }
}
