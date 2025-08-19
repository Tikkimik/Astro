package com.gdx.entities;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.gdx.managers.Camera;

import java.util.ArrayList;

public class OrkAsteroid extends SpaceObject {
    
    private int type;
    public static final int SMALL = 0;
    public static final int MEDIUM = 1;
    public static final int LARGE = 2;
    
    private int numPoints;
    private float[] dists;
    private boolean remove = false;
    
    // Система стрельбы
    private float shootTimer = 0f;
    private final float shootInterval = 2f; // Интервал между выстрелами
    private final int burstSize = 3; // Количество выстрелов в очереди
    private int currentBurst = 0;
    private float burstTimer = 0f;
    private final float burstInterval = 0.2f; // Интервал между выстрелами в очереди
    
    // Цель для стрельбы
    private Player target;
    
    // Список пуль орков
    private ArrayList<OrkBullet> orkBullets;
    
    public OrkAsteroid(float x, float y, int type, Player target, ArrayList<OrkBullet> orkBullets) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.target = target;
        this.orkBullets = orkBullets;
        
        // Настройка размера и скорости в зависимости от типа
        if(type == SMALL){
            numPoints = 8;
            width = height = 15;
            speed = MathUtils.random(40, 60);
        } else if(type == MEDIUM){
            numPoints = 10;
            width = height = 25;
            speed = MathUtils.random(30, 40);
        } else if(type == LARGE){
            numPoints = 12;
            width = height = 45;
            speed = MathUtils.random(15, 25);
        }
        
        rotationSpeed = MathUtils.random(-0.5f, 0.5f);
        radians = MathUtils.random(2 * MathUtils.PI);
        dx = MathUtils.cos(radians) * speed;
        dy = MathUtils.sin(radians) * speed;
        
        shapeX = new float[numPoints];
        shapeY = new float[numPoints];
        dists = new float[numPoints];
        
        int radius = width / 2;
        for(int i = 0; i < numPoints; i++){
            dists[i] = MathUtils.random(radius / 2, radius);
        }
        
        setShape();
    }
    
    public void setShape(){
        float angle = 0;
        
        for(int i = 0; i < numPoints; i++){
            shapeX[i] = x + MathUtils.cos(angle + radians) * dists[i];
            shapeY[i] = y + MathUtils.sin(angle + radians) * dists[i];
            angle += 2 * MathUtils.PI / numPoints;
        }
    }
    
    public int getType(){
        return type;
    }
    
    public boolean shouldRemove() {
        return remove;
    }
    
    public void update(float dt){
        x += dx * dt;
        y += dy * dt;
        
        radians += rotationSpeed * dt;
        setShape();
        
        wrap();
        
        // Обновляем таймеры стрельбы
        shootTimer += dt;
        burstTimer += dt;
        
        // Проверяем, нужно ли начать новую очередь выстрелов
        if (shootTimer >= shootInterval && currentBurst == 0) {
            currentBurst = burstSize;
            burstTimer = 0f;
        }
        
        // Стреляем в очереди
        if (currentBurst > 0 && burstTimer >= burstInterval) {
            shoot();
            currentBurst--;
            burstTimer = 0f;
            
            // Если очередь закончилась, сбрасываем основной таймер
            if (currentBurst == 0) {
                shootTimer = 0f;
            }
        }
    }
    
    private void shoot() {
        if (target == null) return;
        
        // Вычисляем направление к цели
        float targetDx = target.getX() - x;
        float targetDy = target.getY() - y;
        float distance = (float) Math.sqrt(targetDx * targetDx + targetDy * targetDy);
        
        if (distance > 0) {
            // Добавляем небольшое отклонение для реалистичности
            float accuracy = 0.3f; // Точность стрельбы (0 = идеально, 1 = очень неточно)
            float angle = (float) Math.atan2(targetDy, targetDx);
            angle += (MathUtils.random() - 0.5f) * accuracy;
            
            // Создаем пулю орка
            orkBullets.add(new OrkBullet(x, y, angle));
        }
    }
    
    public void draw(ShapeRenderer shapeRenderer, Camera camera) {
        // Рисуем астероид
        shapeRenderer.setColor(0.3f, 0.2f, 0.1f, 1); // Темно-коричневый цвет
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        for(int i = 0; i < shapeX.length; i++) {
            float screenX = camera.worldToScreenX(shapeX[i]);
            float screenY = camera.worldToScreenY(shapeY[i]);
            shapeRenderer.circle(screenX, screenY, 2);
        }
        shapeRenderer.end();
        
        // Рисуем контур астероида
        shapeRenderer.setColor(0.6f, 0.4f, 0.2f, 1); // Светло-коричневый контур
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        
        for(int i = 0, j = shapeX.length - 1; i < shapeX.length; j = i++){
            float screenX1 = camera.worldToScreenX(shapeX[i]);
            float screenY1 = camera.worldToScreenY(shapeY[i]);
            float screenX2 = camera.worldToScreenX(shapeX[j]);
            float screenY2 = camera.worldToScreenY(shapeY[j]);
            shapeRenderer.line(screenX1, screenY1, screenX2, screenY2);
        }
        shapeRenderer.end();
        
        // Рисуем "орков" на астероиде (маленькие зеленые точки)
        shapeRenderer.setColor(0.2f, 0.8f, 0.2f, 1); // Зеленый цвет орков
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        for(int i = 0; i < 3; i++) {
            float orkX = x + MathUtils.cos(radians + i * MathUtils.PI2 / 3) * (width / 3);
            float orkY = y + MathUtils.sin(radians + i * MathUtils.PI2 / 3) * (width / 3);
            float screenOrkX = camera.worldToScreenX(orkX);
            float screenOrkY = camera.worldToScreenY(orkY);
            shapeRenderer.circle(screenOrkX, screenOrkY, 1.5f);
        }
        shapeRenderer.end();
    }
    
    // Проверяем столкновение с другим объектом
    public boolean intersects(SpaceObject other) {
        float dx = x - other.getX();
        float dy = y - other.getY();
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        return distance < width / 2 + other.getWidth() / 2;
    }
    
    public int getWidth() {
        return width;
    }
}
