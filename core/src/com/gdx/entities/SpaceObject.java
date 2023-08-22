package com.gdx.entities;

import com.badlogic.gdx.math.Vector2;
import com.gdx.game.MyGdxGame;

public class SpaceObject extends Vector2 {

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

    public boolean intersects(SpaceObject other) {
        float[] sx = other.getShapeX();
        float[] sy = other.getShapeY();

        for (int i = 0; i < sx.length; i++) {
            if (contains(sx[i], sy[i])) {
                return true;
            }
        }
        return false;
    }

    public boolean contains(float x, float y) { //математика для коллизий
        boolean b = false;

        for (int i = 0, j = shapeX.length - 1; i < shapeX.length; j = i++) {
            if ((shapeY[i] > y) != (shapeY[j] > y) &&
                    (x < (shapeX[j] - shapeX[i]) * (y - shapeY[i]) / (shapeY[j]) + shapeX[i])) {
                b = !b;
            }
        }

        return b;
    }

    protected void wrap() {
        if (x < 0) {
            x = MyGdxGame.WIDTH;
        }

        if (x > MyGdxGame.WIDTH) {
            x = 0;
        }

        if (y < 0) {
            y = MyGdxGame.HEIGHT;
        }

        if (y > MyGdxGame.HEIGHT) {
            y = 0;
        }
    }
}
