package com.gdx.entities;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

public class Asteroid extends SpaceObject {
    private int type;
    public static final int SMALL = 0;
    public static final int MEDIUM = 1;
    public static final int LARGE = 2;

    private int numPoints; //количество граней у астероида
    private float[] dists; //растояние от центра до грани астероида

    private boolean remove;

    public Asteroid(float x, float y, int type){
        this.x = x;
        this.y = y;
        this.type = type;

        if(type == SMALL){
            numPoints = 8;
            width = height = 12;
            speed = MathUtils.random(70, 100);
        } else if(type == MEDIUM){
            numPoints = 10;
            width = height = 20;
            speed = MathUtils.random(50, 60);
        } else if(type == LARGE){
            numPoints = 12;
            width = height = 40;
            speed = MathUtils.random(20, 30);
        }

        rotationSpeed = MathUtils.random(-1, 1);
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

        radians = rotationSpeed * dt;
        setShape();

        wrap();
    }

    public void draw(ShapeRenderer shapeRenderer){
        shapeRenderer.setColor(1,1,1,1);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        for(int i = 0, j = shapeX.length - 1; i < shapeX.length; j = i++){
            shapeRenderer.line(shapeX[i], shapeY[i], shapeX[j], shapeY[j]);
        }

        shapeRenderer.end();
    }
}
