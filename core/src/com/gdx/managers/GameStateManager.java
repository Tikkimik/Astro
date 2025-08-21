package com.gdx.managers;

import com.gdx.gamestates.GameState;
import com.gdx.gamestates.PlayState;
import com.gdx.gamestates.MenuState;
import com.gdx.gamestates.SettingsState;

public class GameStateManager {

    /**
     * current game state
     */
    private GameState gameState;

    public static final int MENU = 0;
    public static final int PLAY = 1;
    public static final int SETTINGS = 2;

    public  GameStateManager(){
        setState(MENU);
    }

    public void setState(int state){
        if(gameState != null){
            gameState.dispose();
        }

        if(state == MENU){
            //switch to menu state
            gameState = new MenuState(this);
        }

        if(state == PLAY){
            //switch to play state
            gameState = new PlayState(this);
        }
        
        if(state == SETTINGS){
            //switch to settings state
            gameState = new SettingsState(this);
        }
    }

    public void  update(float dt){
        gameState.update(dt);
    }

    public void draw(){
        gameState.draw();
    }
    
    public void push(GameState state) {
        if(gameState != null){
            gameState.dispose();
        }
        gameState = state;
    }
}
