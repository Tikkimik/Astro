package com.gdx.entities;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.gdx.game.MyGdxGame;
import com.gdx.managers.Camera;

import com.gdx.utils.Line2D;
import com.gdx.utils.Point2D;
import java.util.ArrayList;

public class Player extends SpaceObject{

    private final int MAX_BULLETS = 4;
    private ArrayList<Bullet> bullets;

    private final float[] flameX;
    private final float[] flameY;
    
    // Детали корабля для красивого рендеринга
    private final float[] engineX;
    private final float[] engineY;
    private final float[] cockpitX;
    private final float[] cockpitY;
    private final float[] wingX;
    private final float[] wingY;
    private final float[] weaponX;
    private final float[] weaponY;
    private final float[] antennaX;
    private final float[] antennaY;

    protected boolean left;
    protected boolean right;
    protected boolean up; //speedBoost

    private final float maxSpeed;
    private final float acceleration; //скорость разгона игрока
    private final float deceleration; //скорость замедления игрока

    private float acceleratingTimer;
    private float engineGlowTimer; // Таймер для мерцания двигателей
    private ArrayList<FlameParticle> flameParticles; // Частицы огня

    private boolean hit;
    private boolean dead;

    private float hitTimer;
    private float hitTime;

    private Line2D.Float[] hitLines;
    private Point2D.Float[] hitLinesVector;
    
    private Texture shipTexture;

    public Player(ArrayList<Bullet> bullets){

        this.bullets = bullets;

        x = MyGdxGame.WIDTH / 2;
        y = MyGdxGame.WIDTH / 2;

        maxSpeed = 300;
        acceleration = 200;
        deceleration = 10;

        shapeX = new float[4];
        shapeY = new float[4];
        flameX = new float[3];
        flameY = new float[3];
        
        // Инициализируем массивы для деталей корабля
        engineX = new float[4];
        engineY = new float[4];
        cockpitX = new float[3];
        cockpitY = new float[3];
        wingX = new float[6];
        wingY = new float[6];
        weaponX = new float[4];
        weaponY = new float[4];
        antennaX = new float[2];
        antennaY = new float[2];

        radians = MathUtils.HALF_PI;
        rotationSpeed = 3;

        hit = false;
        hitTimer = 0;
        hitTime = 2;
        
        // Инициализируем список частиц огня
        flameParticles = new ArrayList<>();
        
        // Создаем текстуру корабля
        createShipTexture();
    }

    private void setShape(){
        // Основной корпус корабля
        shapeX[0] = x + MathUtils.cos(radians) * 10;
        shapeY[0] = y + MathUtils.sin(radians) * 10;

        shapeX[1] = x + MathUtils.cos(radians - 4 * MathUtils.PI / 5) * 8;
        shapeY[1] = y + MathUtils.sin(radians - 4 * MathUtils.PI / 5) * 8;

        shapeX[2] = x + MathUtils.cos(radians + MathUtils.PI) * 6;
        shapeY[2] = y + MathUtils.sin(radians + MathUtils.PI) * 6;

        shapeX[3] = x + MathUtils.cos(radians + 4 * MathUtils.PI / 5) * 8;
        shapeY[3] = y + MathUtils.sin(radians + 4 * MathUtils.PI / 5) * 8;
        
        // Обновляем детали корабля
        setEngineDetails();
        setCockpitDetails();
        setWingDetails();
        setWeaponDetails();
        setAntennaDetails();
    }
    
    private void setEngineDetails() {
        // Двигатели по бокам
        engineX[0] = x + MathUtils.cos(radians - 3 * MathUtils.PI / 4) * 6;
        engineY[0] = y + MathUtils.sin(radians - 3 * MathUtils.PI / 4) * 6;
        
        engineX[1] = x + MathUtils.cos(radians - 3 * MathUtils.PI / 4) * 4;
        engineY[1] = y + MathUtils.sin(radians - 3 * MathUtils.PI / 4) * 4;
        
        engineX[2] = x + MathUtils.cos(radians + 3 * MathUtils.PI / 4) * 6;
        engineY[2] = y + MathUtils.sin(radians + 3 * MathUtils.PI / 4) * 6;
        
        engineX[3] = x + MathUtils.cos(radians + 3 * MathUtils.PI / 4) * 4;
        engineY[3] = y + MathUtils.sin(radians + 3 * MathUtils.PI / 4) * 4;
    }
    
    private void setCockpitDetails() {
        // Кабина пилота
        cockpitX[0] = x + MathUtils.cos(radians) * 3;
        cockpitY[0] = y + MathUtils.sin(radians) * 3;
        
        cockpitX[1] = x + MathUtils.cos(radians - MathUtils.PI / 6) * 2;
        cockpitY[1] = y + MathUtils.sin(radians - MathUtils.PI / 6) * 2;
        
        cockpitX[2] = x + MathUtils.cos(radians + MathUtils.PI / 6) * 2;
        cockpitY[2] = y + MathUtils.sin(radians + MathUtils.PI / 6) * 2;
    }
    
    private void setWingDetails() {
        // Крылья корабля
        wingX[0] = x + MathUtils.cos(radians - 2 * MathUtils.PI / 3) * 12;
        wingY[0] = y + MathUtils.sin(radians - 2 * MathUtils.PI / 3) * 12;
        
        wingX[1] = x + MathUtils.cos(radians - 2 * MathUtils.PI / 3) * 8;
        wingY[1] = y + MathUtils.sin(radians - 2 * MathUtils.PI / 3) * 8;
        
        wingX[2] = x + MathUtils.cos(radians - MathUtils.PI / 2) * 10;
        wingY[2] = y + MathUtils.sin(radians - MathUtils.PI / 2) * 10;
        
        wingX[3] = x + MathUtils.cos(radians + 2 * MathUtils.PI / 3) * 12;
        wingY[3] = y + MathUtils.sin(radians + 2 * MathUtils.PI / 3) * 12;
        
        wingX[4] = x + MathUtils.cos(radians + 2 * MathUtils.PI / 3) * 8;
        wingY[4] = y + MathUtils.sin(radians + 2 * MathUtils.PI / 3) * 8;
        
        wingX[5] = x + MathUtils.cos(radians + MathUtils.PI / 2) * 10;
        wingY[5] = y + MathUtils.sin(radians + MathUtils.PI / 2) * 10;
    }
    
    private void setWeaponDetails() {
        // Орудия по бокам
        weaponX[0] = x + MathUtils.cos(radians - MathUtils.PI / 3) * 9;
        weaponY[0] = y + MathUtils.sin(radians - MathUtils.PI / 3) * 9;
        
        weaponX[1] = x + MathUtils.cos(radians - MathUtils.PI / 3) * 7;
        weaponY[1] = y + MathUtils.sin(radians - MathUtils.PI / 3) * 7;
        
        weaponX[2] = x + MathUtils.cos(radians + MathUtils.PI / 3) * 9;
        weaponY[2] = y + MathUtils.sin(radians + MathUtils.PI / 3) * 9;
        
        weaponX[3] = x + MathUtils.cos(radians + MathUtils.PI / 3) * 7;
        weaponY[3] = y + MathUtils.sin(radians + MathUtils.PI / 3) * 7;
    }
    
    private void setAntennaDetails() {
        // Антенны на носу корабля
        antennaX[0] = x + MathUtils.cos(radians) * 12;
        antennaY[0] = y + MathUtils.sin(radians) * 12;
        
        antennaX[1] = x + MathUtils.cos(radians) * 14;
        antennaY[1] = y + MathUtils.sin(radians) * 14;
    }
    
    private void createShipTexture() {
        // Загружаем текстуру корабля из файла
        try {
            shipTexture = new Texture("ship.png");
        } catch (Exception e) {
            // Если текстура не найдена, используем заглушку
            shipTexture = new Texture("badlogic.jpg");
        }
    }

    private void setFlame(){
        flameX[0] = x + MathUtils.cos(radians - 5 * MathUtils.PI / 6) * 5;
        flameY[0] = y + MathUtils.sin(radians - 5 * MathUtils.PI / 6) * 5;

        flameX[1] = x + MathUtils.cos(radians - MathUtils.PI) * (6 + acceleratingTimer * 50);
        flameY[1] = y + MathUtils.sin(radians - MathUtils.PI) * (6 + acceleratingTimer * 50);

        flameX[2] = x + MathUtils.cos(radians + 5 * MathUtils.PI / 6) * 5;
        flameY[2] = y + MathUtils.sin(radians + 5 * MathUtils.PI / 6) * 5;
    }

    public void setLeft(boolean b){
        left = b;
    }

    public void setRight(boolean b){
        right = b;
    }

    public void setUp(boolean b){
        up = b;
    }

    public void shoot(){
        if(bullets.size() == MAX_BULLETS) return;
        bullets.add(new Bullet(x, y, radians));
    }

    public boolean isHit() {
        return hit;
    }

    public boolean isDead() {
        return dead;
    }

    public void reset() {
        x = MyGdxGame.WIDTH / 2;
        y = MyGdxGame.HEIGHT / 2;
        setShape();
        hit = dead = false;
    }

    public void hit() {
        if(hit){
            return;
        }

        hit = true;
        dx = dy = 0;
        left = right = up = false;

        hitLines = new Line2D.Float[4];

        for(int i = 0, j = hitLines.length - 1; i < hitLines.length; j = i++) {
            hitLines[i] = new Line2D.Float(
                    shapeX[i], shapeY[i], shapeX[j], shapeY[j]
            );
        }

        hitLinesVector = new Point2D.Float[4];
        hitLinesVector[0] = new Point2D.Float(
                MathUtils.cos(radians + 1.5f),
                MathUtils.sin(radians + 1.5f)
        );

        hitLinesVector[1] = new Point2D.Float(
                MathUtils.cos(radians - 1.5f),
                MathUtils.sin(radians - 1.5f)
        );

        hitLinesVector[2] = new Point2D.Float(
                MathUtils.cos(radians - 2.8f),
                MathUtils.sin(radians - 2.8f)
        );

        hitLinesVector[3] = new Point2D.Float(
                MathUtils.cos(radians + 2.8f),
                MathUtils.sin(radians + 2.8f)
        );
    }

    public void update(float dt) {

        //hit check
        if(hit) {
            hitTimer += dt;
            if(hitTimer > hitTime) {
                dead = true;
                hitTimer = 0;
            }
            for(int i = 0; i < hitLines.length; i++) {
                hitLines[i].setLine(
                        hitLines[i].x1 + hitLinesVector[i].x * 10 * dt,
                        hitLines[i].y1 + hitLinesVector[i].y * 10 * dt,
                        hitLines[i].x2 + hitLinesVector[i].x * 10 * dt,
                        hitLines[i].y2 + hitLinesVector[i].y * 10 * dt
                );
            }
            return;
        }

        //turning
        if(left){
            radians += rotationSpeed * dt;
        } else if(right){
            radians -= rotationSpeed * dt;
        }

        //accelerating
        if(up){
            dx += MathUtils.cos(radians) * acceleration * dt;
            dy += MathUtils.sin(radians) * acceleration * dt;
            acceleratingTimer += dt;
            if(acceleratingTimer > 0.1f){
                acceleratingTimer = 0;
            }
        } else {
            acceleratingTimer = 0;
        }

        //deaceleration
        float vector = (float) Math.sqrt(dx * dx + dy * dy);
        if(vector > 0){
            dx -= (dx / vector) * deceleration * dt;
            dy -= (dy / vector) * deceleration * dt;
        }
        if(vector > maxSpeed){
            dx = (dx / vector) * maxSpeed;
            dy = (dy / vector) * maxSpeed;
        }

        //setPosition
        x += dx * dt;
        y += dy * dt;

        //set shape
        setShape();

        //set flame
        if(up){
            setFlame();
        }
        
        // Обновляем эффекты двигателей
        engineGlowTimer += dt * 3f;
        if (engineGlowTimer > MathUtils.PI2) {
            engineGlowTimer -= MathUtils.PI2;
        }
        
        // Обновляем частицы огня
        for(int i = flameParticles.size() - 1; i >= 0; i--) {
            FlameParticle particle = flameParticles.get(i);
            particle.update(dt);
            
            if(particle.shouldRemove()) {
                flameParticles.remove(i);
            }
        }

        //screen warp
        wrap();
    }

    public void draw(ShapeRenderer shapeRenderer, Camera camera, SpriteBatch spriteBatch){

        //hit check
        if(hit) {
            // Эффект взрыва - красные линии
            shapeRenderer.setColor(1, 0.3f, 0.1f, 1);
            for(int i = 0; i < hitLines.length; i++) {
                float screenX1 = camera.worldToScreenX(hitLines[i].x1);
                float screenY1 = camera.worldToScreenY(hitLines[i].y1);
                float screenX2 = camera.worldToScreenX(hitLines[i].x2);
                float screenY2 = camera.worldToScreenY(hitLines[i].y2);
                shapeRenderer.line(screenX1, screenY1, screenX2, screenY2);
            }
            return;
        }

        // Рисуем текстуру корабля
        if (shipTexture != null) {
            float screenX = camera.worldToScreenX(x);
            float screenY = camera.worldToScreenY(y);
            float textureSize = 32 * camera.getCurrentZoom(); // Масштабируем с зумом
            
            spriteBatch.begin();
            spriteBatch.draw(shipTexture, 
                screenX - textureSize/2, 
                screenY - textureSize/2, 
                textureSize/2, textureSize/2, // Точка вращения
                textureSize, textureSize, // Размер
                1, 1, // Масштаб
                radians * MathUtils.radiansToDegrees, // Угол поворота
                0, 0, // Область текстуры
                shipTexture.getWidth(), shipTexture.getHeight(), // Размер области
                false, false); // Переворот
            spriteBatch.end();
        }

        // Частицы огня двигателей
        if(up){
            // Создаем частицы огня из сопла
            float nozzleX = x - MathUtils.cos(radians) * 8;
            float nozzleY = y - MathUtils.sin(radians) * 8;
            float flameAngle = radians + MathUtils.PI; // Огонь направлен назад
            
            // Создаем больше частиц для лучшего эффекта
            for(int i = 0; i < 5; i++) {
                float spreadAngle = flameAngle + (MathUtils.random() - 0.5f) * 0.4f; // Больший разброс
                float flameSpeed = 150 + MathUtils.random() * 150; // Случайная скорость
                flameParticles.add(new FlameParticle(nozzleX, nozzleY, spreadAngle, flameSpeed));
            }
        }
        
        // Рисуем частицы огня (заполненные круги)
        shapeRenderer.end(); // Заканчиваем рендеринг линий
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        for(int i = flameParticles.size() - 1; i >= 0; i--) {
            FlameParticle particle = flameParticles.get(i);
            particle.draw(shapeRenderer, camera);
        }
        
        shapeRenderer.end(); // Заканчиваем рендеринг заполненных объектов
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line); // Возвращаемся к линиям
    }
    
    public int getWidth() {
        return 16; // Примерный размер корабля
    }
    
    public void dispose() {
        // Очищаем частицы огня
        if (flameParticles != null) {
            flameParticles.clear();
        }
        
        // Освобождаем текстуру
        if (shipTexture != null) {
            shipTexture.dispose();
        }
    }
}
