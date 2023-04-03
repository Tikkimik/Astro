package com.gdx.gamestates;

import com.gdx.managers.GameStateManager;

public abstract class GameState {

    protected GameStateManager gameStateManager;

    protected GameState(GameStateManager gameStateManager){
        this.gameStateManager = gameStateManager;
        init();
    }

    /**
     * запускается при запуске игры
     */
    public abstract void init();

    /**
     * update и draw это основной цикл игры
     */
    public abstract void update(float dt);

    public abstract void draw();

    /**
     * дискриптор ввода для получения информации обычно запускается в update
     */
    public abstract void handleInput();

    /**
     * dispose помогает завершить существующее состояние
     */
    public abstract void dispose();

}
