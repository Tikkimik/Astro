package com.gdx.utils;

public class Point2D {
    public float x, y;
    
    public Point2D(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    public static class Float extends Point2D {
        public Float(float x, float y) {
            super(x, y);
        }
    }
}
