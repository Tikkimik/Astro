package com.gdx.utils;

public class Line2D {
    public float x1, y1, x2, y2;
    
    public Line2D(float x1, float y1, float x2, float y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }
    
    public void setLine(float x1, float y1, float x2, float y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }
    
    public static class Float extends Line2D {
        public Float(float x1, float y1, float x2, float y2) {
            super(x1, y1, x2, y2);
        }
    }
}
