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
    }
    
    private void updateCamera() {
        camera.position.set(cameraX, cameraY, 0);
        camera.zoom = currentZoom;
        camera.update();
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
}
