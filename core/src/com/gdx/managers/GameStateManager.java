package com.gdx.managers;

import com.gdx.gamestates.GameState;
import com.gdx.gamestates.PlayState;

public class GameStateManager {

    /**
     * current game state
     */
    private GameState gameState;

    public static final int MENU = 0;
    public static final int PLAY = 1;

    public  GameStateManager(){
        setState(PLAY);
    }

    public void setState(int state){
        if(gameState != null){
            gameState.dispose();
        }

        if(state == MENU){
            //switch to menu state
        }

        if(state == PLAY){
            //switch to play state
            gameState = new PlayState(this);
        }
    }

    public void  update(float dt){
        gameState.update(dt);
    }

    public void draw(){
        gameState.draw();
    }
}
