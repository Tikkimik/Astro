package com.gdx.entities;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.gdx.game.MyGdxGame;

public class Player extends SpaceObject{

    protected boolean left;
    protected boolean right;
    protected boolean up; //speedBoost

    private final float maxSpeed;
    private final float acceleration; //скорость разгона игрока
    private final float deceleration; //скорость замедления игрока

    public Player(){
        x = MyGdxGame.WIDTH / 2;
        y = MyGdxGame.WIDTH / 2;

        maxSpeed = 300;
        acceleration = 200;
        deceleration = 10;

        shapeX = new float[4];
        shapeY = new float[4];

        radians = MathUtils.HALF_PI;
        rotationSpeed = 3;
    }

    private void setShape(){
        shapeX[0] = x + MathUtils.cos(radians) * 8;
        shapeY[0] = y + MathUtils.sin(radians) * 8;

        shapeX[1] = x + MathUtils.cos(radians - 4 * MathUtils.PI / 5) * 8;
        shapeY[1] = y + MathUtils.sin(radians - 4 * MathUtils.PI / 5) * 8;

        shapeX[2] = x + MathUtils.cos(radians + MathUtils.PI) * 5;
        shapeY[2] = y + MathUtils.sin(radians + MathUtils.PI) * 5;

        shapeX[3] = x + MathUtils.cos(radians + 4 * MathUtils.PI / 5) * 8;
        shapeY[3] = y + MathUtils.sin(radians + 4 * MathUtils.PI / 5) * 8;
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

    public void update(float dt){

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

        //screen warp
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
