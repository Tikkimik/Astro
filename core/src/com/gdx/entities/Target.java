package com.gdx.entities;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

public class Target extends SpaceObject {
    private int numPoints;
    private float[] dists; //растояние от центра до грани астероида


    private boolean remove;

    public Target(Asteroid asteroid) {
        this.numPoints = 4;

        x = asteroid.getX();
        y = asteroid.getY();

        shapeX = new float[numPoints];
        shapeY = new float[numPoints];
        dists = new float[numPoints];

//        int radius = width / 2;
//        for(int i = 0; i < numPoints; i++){
//            dists[i] = MathUtils.random(radius / 2, radius);
//        }

        setShape();
    }

    public void setShape(){
        shapeX[0] = x +  55 - 25;
        shapeY[0] = y +  55 - 25;

        shapeX[1] = x + MathUtils.cos(MathUtils.PI / 2 ) * 55 - 25;
        shapeY[1] = y + MathUtils.sin(MathUtils.PI / 2 ) * 55 - 25;

        shapeX[2] = x + MathUtils.cos(MathUtils.PI) - 25;
        shapeY[2] = y + MathUtils.sin(MathUtils.PI) - 25;

        shapeX[3] = x + MathUtils.cos(MathUtils.PI * 2 ) * 55 - 25;
        shapeY[3] = y + MathUtils.sin(MathUtils.PI * 2 ) * 55 - 25;
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
        shapeRenderer.setColor(1,0,0,0);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        for(int i = 0, j = shapeX.length - 1; i < shapeX.length; j = i++){
            shapeRenderer.line(shapeX[i], shapeY[i], shapeX[j], shapeY[j]);
        }

        shapeRenderer.end();
    }
}