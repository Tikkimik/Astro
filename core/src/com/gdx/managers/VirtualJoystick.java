package com.gdx.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.MathUtils;

public class VirtualJoystick {
    private Vector2 position;
    private Vector2 center;
    private Vector2 direction;
    private float radius;
    private float innerRadius;
    private boolean isActive;
    private int pointer;
    
    // Цвета
    private Color outerColor = new Color(0.3f, 0.3f, 0.3f, 0.7f);
    private Color innerColor = new Color(0.7f, 0.7f, 0.7f, 0.9f);
    private Color activeColor = new Color(0.2f, 0.8f, 0.2f, 0.9f);
    
    public VirtualJoystick(float x, float y, float radius) {
        this.center = new Vector2(x, y);
        this.position = new Vector2(x, y);
        this.direction = new Vector2(0, 0);
        this.radius = radius;
        this.innerRadius = radius * 0.3f;
        this.isActive = false;
        this.pointer = -1;
    }
    
    public void update() {
        if (isActive) {
            // Ограничиваем позицию джойстика в пределах радиуса
            float distance = center.dst(position);
            if (distance > radius) {
                position.sub(center).nor().scl(radius).add(center);
            }
            
            // Вычисляем направление
            direction.set(position).sub(center).nor();
        } else {
            // Плавно возвращаем джойстик в центр
            position.lerp(center, 0.1f);
            direction.setZero();
        }
    }
    
    public void render(ShapeRenderer shapeRenderer) {
        // Начинаем рендеринг
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        // Внешний круг
        shapeRenderer.setColor(outerColor);
        shapeRenderer.circle(center.x, center.y, radius);
        
        // Внутренний круг (ручка джойстика)
        Color currentInnerColor = isActive ? activeColor : innerColor;
        shapeRenderer.setColor(currentInnerColor);
        shapeRenderer.circle(position.x, position.y, innerRadius);
        
        shapeRenderer.end();
        
        // Обводка
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.circle(center.x, center.y, radius);
        shapeRenderer.circle(position.x, position.y, innerRadius);
        shapeRenderer.end();
    }
    
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (this.pointer == -1) {
            Vector2 touchPos = new Vector2(screenX, Gdx.graphics.getHeight() - screenY);
            if (center.dst(touchPos) <= radius) {
                this.pointer = pointer;
                this.isActive = true;
                this.position.set(touchPos);
                return true;
            }
        }
        return false;
    }
    
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (this.pointer == pointer && isActive) {
            Vector2 touchPos = new Vector2(screenX, Gdx.graphics.getHeight() - screenY);
            this.position.set(touchPos);
            return true;
        }
        return false;
    }
    
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (this.pointer == pointer) {
            this.pointer = -1;
            this.isActive = false;
            return true;
        }
        return false;
    }
    
    // Геттеры для получения направления
    public Vector2 getDirection() {
        return direction;
    }
    
    public float getX() {
        return direction.x;
    }
    
    public float getY() {
        return direction.y;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setPosition(float x, float y) {
        this.center.set(x, y);
        this.position.set(x, y);
    }
    
    public void setRadius(float radius) {
        this.radius = radius;
        this.innerRadius = radius * 0.3f;
    }
}


