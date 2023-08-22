package com.gdx.entities;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.gdx.game.MyGdxGame;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;

public class Player extends SpaceObject {

    private final int MAX_BULLETS = 1;
    private ArrayList<Bullet> bullets;

    private final float[] flameX;
    private final float[] flameY;

    protected boolean left;
    protected boolean right;
    protected boolean up; //speedBoost

    private final float maxSpeed;
    private final float acceleration; //скорость разгона игрока
    private final float deceleration; //скорость замедления игрока

    private float acceleratingTimer;

    private boolean hit;
    private boolean dead;

    private float hitTimer;
    private float hitTime;

    private Line2D.Float[] hitLines;
    private Point2D.Float[] hitLinesVector;

    public Player(ArrayList<Bullet> bullets) {

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

        radians = MathUtils.HALF_PI;
        rotationSpeed = 3;

        hit = false;
        hitTimer = 0;
        hitTime = 2;
    }

    private void setShape() {
        shapeX[0] = x + MathUtils.cos(radians) * 8;
        shapeY[0] = y + MathUtils.sin(radians) * 8;

        shapeX[1] = x + MathUtils.cos(radians - 4 * MathUtils.PI / 5) * 8;
        shapeY[1] = y + MathUtils.sin(radians - 4 * MathUtils.PI / 5) * 8;

        shapeX[2] = x + MathUtils.cos(radians + MathUtils.PI) * 5;
        shapeY[2] = y + MathUtils.sin(radians + MathUtils.PI) * 5;

        shapeX[3] = x + MathUtils.cos(radians + 4 * MathUtils.PI / 5) * 8;
        shapeY[3] = y + MathUtils.sin(radians + 4 * MathUtils.PI / 5) * 8;
    }

    private void setFlame() {
        flameX[0] = x + MathUtils.cos(radians - 5 * MathUtils.PI / 6) * 5;
        flameY[0] = y + MathUtils.sin(radians - 5 * MathUtils.PI / 6) * 5;

        flameX[1] = x + MathUtils.cos(radians - MathUtils.PI) * (6 + acceleratingTimer * 50);
        flameY[1] = y + MathUtils.sin(radians - MathUtils.PI) * (6 + acceleratingTimer * 50);

        flameX[2] = x + MathUtils.cos(radians + 5 * MathUtils.PI / 6) * 5;
        flameY[2] = y + MathUtils.sin(radians + 5 * MathUtils.PI / 6) * 5;
    }

    public void setLeft(boolean b) {
        left = b;
    }

    public void setRight(boolean b) {
        right = b;
    }

    public void setUp(boolean b) {
        up = b;
    }

    public void shoot() {
        if (bullets.size() == MAX_BULLETS) return;
        bullets.add(new Bullet(x, y, radians));
    }

    public void shoot(float rad) {
        if (bullets.size() == MAX_BULLETS) return;
        bullets.add(new Bullet(x, y, rad));
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
        if (hit) {
            return;
        }

        hit = true;
        dx = dy = 0;
        left = right = up = false;

        hitLines = new Line2D.Float[4];

        for (int i = 0, j = hitLines.length - 1; i < hitLines.length; j = i++) {
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
        if (hit) {
            hitTimer += dt;
            if (hitTimer > hitTime) {
                dead = true;
                hitTimer = 0;
            }
            for (int i = 0; i < hitLines.length; i++) {
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
        if (left) {
            radians += rotationSpeed * dt;
        } else if (right) {
            radians -= rotationSpeed * dt;
        }

        //accelerating
        if (up) {
            dx += MathUtils.cos(radians) * acceleration * dt;
            dy += MathUtils.sin(radians) * acceleration * dt;
            acceleratingTimer += dt;
            if (acceleratingTimer > 0.1f) {
                acceleratingTimer = 0;
            }
        } else {
            acceleratingTimer = 0;
        }

        //deaceleration
        float vector = (float) Math.sqrt(dx * dx + dy * dy);
        if (vector > 0) {
            dx -= (dx / vector) * deceleration * dt;
            dy -= (dy / vector) * deceleration * dt;
        }
        if (vector > maxSpeed) {
            dx = (dx / vector) * maxSpeed;
            dy = (dy / vector) * maxSpeed;
        }

        //setPosition
        x += dx * dt;
        y += dy * dt;

        //set shape
        setShape();

        //set flame
        if (up) {
            setFlame();
        }

        //screen warp
        wrap();
    }

    public void draw(ShapeRenderer shapeRenderer) {

        shapeRenderer.setColor(1, 1, 1, 1);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        //hit check
        if (hit) {
            for (int i = 0; i < hitLines.length; i++) {
                shapeRenderer.line(
                        hitLines[i].x1,
                        hitLines[i].y1,
                        hitLines[i].x2,
                        hitLines[i].y2
                );
            }
            shapeRenderer.end();
            return;
        }

        //draw ship
        for (int i = 0, j = shapeX.length - 1; i < shapeX.length; j = i++) {
            shapeRenderer.line(shapeX[i], shapeY[i], shapeX[j], shapeY[j]);
        }

        //draw flames
        if (up) {
            for (int i = 0, j = flameX.length - 1; i < flameX.length; j = i++) {
                shapeRenderer.line(flameX[i], flameY[i], flameX[j], flameY[j]);
            }
        }

        shapeRenderer.end();
    }
}
