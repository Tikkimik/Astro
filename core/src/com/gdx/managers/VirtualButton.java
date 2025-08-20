package com.gdx.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class VirtualButton {
    private Rectangle bounds;
    private String text;
    private boolean isPressed;
    private boolean isActive;
    private int pointer;
    
    // Цвета
    private Color normalColor = new Color(0.3f, 0.3f, 0.3f, 0.8f);
    private Color pressedColor = new Color(0.2f, 0.8f, 0.2f, 0.9f);
    private Color textColor = Color.WHITE;
    
    public VirtualButton(float x, float y, float width, float height, String text) {
        this.bounds = new Rectangle(x - width/2, y - height/2, width, height);
        this.text = text;
        this.isPressed = false;
        this.isActive = false;
        this.pointer = -1;
    }
    
    public void update() {
        // НЕ сбрасываем состояние нажатия - оно сбрасывается только при touchUp
        // isPressed = false; // Убираем эту строку
    }
    
    public void render(ShapeRenderer shapeRenderer) {
        // Основная кнопка
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        Color currentColor = isActive ? pressedColor : normalColor;
        shapeRenderer.setColor(currentColor);
        shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height);
        shapeRenderer.end();
        
        // Обводка
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height);
        shapeRenderer.end();
        
        // Добавляем простую визуальную индикацию для разных кнопок
        if (text.equals("FIRE")) {
            // Красная точка для кнопки стрельбы
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(Color.RED);
            shapeRenderer.circle(bounds.x + bounds.width/2, bounds.y + bounds.height/2, 8);
            shapeRenderer.end();
        } else if (text.equals("+")) {
            // Зеленая точка для приближения
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(Color.GREEN);
            shapeRenderer.circle(bounds.x + bounds.width/2, bounds.y + bounds.height/2, 6);
            shapeRenderer.end();
        } else if (text.equals("-")) {
            // Синяя точка для отдаления
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(Color.BLUE);
            shapeRenderer.circle(bounds.x + bounds.width/2, bounds.y + bounds.height/2, 6);
            shapeRenderer.end();
        } else if (text.equals("R")) {
            // Желтая точка для сброса
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(Color.YELLOW);
            shapeRenderer.circle(bounds.x + bounds.width/2, bounds.y + bounds.height/2, 6);
            shapeRenderer.end();
        }
    }
    
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (this.pointer == -1) {
            Vector2 touchPos = new Vector2(screenX, Gdx.graphics.getHeight() - screenY);
            if (bounds.contains(touchPos)) {
                this.pointer = pointer;
                this.isActive = true;
                this.isPressed = true;
                return true;
            }
        }
        return false;
    }
    
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (this.pointer == pointer) {
            Vector2 touchPos = new Vector2(screenX, Gdx.graphics.getHeight() - screenY);
            if (bounds.contains(touchPos)) {
                this.isActive = true;
            } else {
                this.isActive = false;
            }
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
    
    public boolean isPressed() {
        return isPressed;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setPosition(float x, float y) {
        bounds.setPosition(x - bounds.width/2, y - bounds.height/2);
    }
    
    public void setSize(float width, float height) {
        bounds.setSize(width, height);
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
}


