package com.gdx.entities;

import com.gdx.game.MyGdxGame;

public class SpaceObject {

    protected float x;
    protected float y;

    /**
     * направления движения (векторы)
     */
    protected float dx;
    protected float dy;

    /**
     * угол под которым они повернуты
     */
    protected float radians;

    /**
     * speed
     */
    protected float speed;

    /**
     * rotation speed
     */
    protected float rotationSpeed;

    /**
     * size
     */
    protected int width;
    protected int height;

    /**
     * форма
     */
    protected float[] shapeX;
    protected float[] shapeY;

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getDx() {
        return dx;
    }

    public float getDy() {
        return dy;
    }

    public float getRadians() {
        return radians;
    }

    public float getSpeed() {
        return speed;
    }

    public float getRotationSpeed() {
        return rotationSpeed;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public float[] getShapeX() {
        return shapeX;
    }

    public float[] getShapeY() {
        return shapeY;
    }

    protected void wrap() {
        if(x < 0){
            x = MyGdxGame.WIDTH;
        }
        if(x > MyGdxGame.WIDTH){
            x = 0;
        }
        if(y < 0){
            y = MyGdxGame.HEIGHT;
        }
        if(y > MyGdxGame.HEIGHT){
            y = 0;
        }
    }
}
