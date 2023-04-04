package com.gdx.gamestates;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.gdx.entities.Bullet;
import com.gdx.entities.Player;
import com.gdx.managers.GameKeys;
import com.gdx.managers.GameStateManager;

import java.util.ArrayList;

public class PlayState extends GameState {

    private ShapeRenderer shapeRenderer;

    private Player player;
    private ArrayList<Bullet> bullets;

    public PlayState(GameStateManager gameStateManager){
        super(gameStateManager);
    }

    @Override
    public void init() {
        shapeRenderer = new ShapeRenderer();
        bullets = new ArrayList<Bullet>();
        player = new Player(bullets);
    }

    @Override
    public void update(float dt) {
//        System.out.println("PLAY STATE UPDATING");
        handleInput();
        player.update(dt);
    }

    @Override
    public void draw() {
//        System.out.println("PLAY STATE DRAWING");
        player.draw(shapeRenderer);
    }

    @Override
    public void handleInput() {
        player.setLeft(GameKeys.isDown(GameKeys.LEFT));
        player.setRight(GameKeys.isDown(GameKeys.RIGHT));
        player.setUp(GameKeys.isDown(GameKeys.UP));
    }

    @Override
    public void dispose() {

    }
}
