package com.gdx.entities;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.gdx.managers.Camera;
import com.gdx.managers.GameObjectManager;

public class OrkAsteroid extends Enemy {
    
    private int type;
    public static final int SMALL = 0;
    public static final int MEDIUM = 1;
    public static final int LARGE = 2;
    
    private int numPoints;
    private float[] dists;
    
    // Система стрельбы
    private float shootTimer = 0f;
    private final float shootInterval = 2f; // Интервал между выстрелами
    private final int burstSize = 3; // Количество выстрелов в очереди
    private int currentBurst = 0;
    private float burstTimer = 0f;
    private final float burstInterval = 0.2f; // Интервал между выстрелами в очереди
    
    // Цель для стрельбы
    private Player target;
    
    // Список пуль орков (используем Array вместо ArrayList)
    private Array<OrkBullet> orkBullets;
    private GameObjectManager gameObjectManager;
    
    public OrkAsteroid(float x, float y, int type, Player target, Array<OrkBullet> orkBullets) {
        super(x, y, 2); // Здоровье = 2 для орк-астероидов
        this.type = type;
        this.target = target;
        this.orkBullets = orkBullets != null ? orkBullets : new Array<>();
        
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
    
    public void setGameObjectManager(GameObjectManager gameObjectManager) {
        this.gameObjectManager = gameObjectManager;
    }

    public int getType(){
        return type;
    }
    
    @Override
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
        
        // Вычисляем направление к цели (оптимизировано - без квадратного корня)
        float targetDx = target.getX() - x;
        float targetDy = target.getY() - y;
        float distanceSq = targetDx * targetDx + targetDy * targetDy;
        
        if (distanceSq > 100) { // Минимальное расстояние для стрельбы (10² = 100)
            // Добавляем небольшое отклонение для реалистичности
            float accuracy = 0.3f; // Точность стрельбы (0 = идеально, 1 = очень неточно)
            float angle = (float) Math.atan2(targetDy, targetDx);
            angle += (MathUtils.random() - 0.5f) * accuracy;
            
            // Создаем пулю орка
            OrkBullet bullet = new OrkBullet(x, y, angle);
            if (gameObjectManager != null) {
                gameObjectManager.addOrkBullet(bullet);
            }
            orkBullets.add(bullet);
        }
    }
    
    @Override
    public void draw(ShapeRenderer shapeRenderer, Camera camera) {
        // Рисуем орк-астероид как красный многоугольник
        shapeRenderer.setColor(0.8f, 0.2f, 0.2f, 1); // Красный цвет
        
        float[] screenX = new float[shapeX.length];
        float[] screenY = new float[shapeY.length];
        
        for (int i = 0; i < shapeX.length; i++) {
            screenX[i] = camera.worldToScreenX(shapeX[i]);
            screenY[i] = camera.worldToScreenY(shapeY[i]);
        }
        
        // Рисуем многоугольник
        for (int i = 0; i < screenX.length; i++) {
            int next = (i + 1) % screenX.length;
            shapeRenderer.line(screenX[i], screenY[i], screenX[next], screenY[next]);
        }
    }
    
    public void drawFilled(ShapeRenderer shapeRenderer, Camera camera) {
        // Рисуем астероид
        shapeRenderer.setColor(0.3f, 0.2f, 0.1f, 1); // Темно-коричневый цвет
        
        for(int i = 0; i < shapeX.length; i++) {
            float screenX = camera.worldToScreenX(shapeX[i]);
            float screenY = camera.worldToScreenY(shapeY[i]);
            shapeRenderer.circle(screenX, screenY, 2);
        }
        
        // Рисуем "орков" на астероиде (маленькие зеленые точки)
        shapeRenderer.setColor(0.2f, 0.8f, 0.2f, 1); // Зеленый цвет орков
        
        for(int i = 0; i < 3; i++) {
            float orkX = x + MathUtils.cos(radians + i * MathUtils.PI2 / 3) * (width / 3);
            float orkY = y + MathUtils.sin(radians + i * MathUtils.PI2 / 3) * (width / 3);
            float screenOrkX = camera.worldToScreenX(orkX);
            float screenOrkY = camera.worldToScreenY(orkY);
            shapeRenderer.circle(screenOrkX, screenOrkY, 1.5f);
        }
    }
    
    // Проверяем столкновение с другим объектом
    public boolean intersects(SpaceObject other) {
        float dx = x - other.getX();
        float dy = y - other.getY();
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        return distance < width / 2 + other.getWidth() / 2;
    }
    
    @Override
    public int getWidth() {
        return width;
    }
    
    @Override
    public int getHeight() {
        return height;
    }
    
    @Override
    public String getEnemyType() {
        return "OrkAsteroid";
    }
}
