package com.gdx.managers;

import com.gdx.gamestates.GameState;
import com.gdx.gamestates.PlayState;
import com.gdx.gamestates.MenuState;
import com.gdx.gamestates.SettingsState;
import com.gdx.gamestates.UpgradeSelectionState;

public class GameStateManager {

    /**
     * current game state
     */
    private GameState gameState;
    
    // Сохраняем состояние игры для восстановления
    private com.gdx.managers.GameState savedGameState;

    public static final int MENU = 0;
    public static final int PLAY = 1;
    public static final int SETTINGS = 2;
    public static final int UPGRADE_SELECTION = 3;

    public  GameStateManager(){
        setState(MENU);
    }

    public void setState(int state){
        setState(state, null);
    }
    
    public void setState(int state, Object data){
        if(gameState != null){
            gameState.dispose();
        }

        if(state == MENU){
            //switch to menu state
            System.out.println("=== ПЕРЕХОД В МЕНЮ ===");
            System.out.println("Тип данных: " + (data != null ? data.getClass().getSimpleName() : "null"));
            // Сохраняем состояние игры, если оно передано
            if(data instanceof com.gdx.managers.GameState){
                System.out.println("Сохраняем состояние игры в GameStateManager");
                savedGameState = (com.gdx.managers.GameState) data;
                System.out.println("Состояние сохранено: " + (savedGameState != null ? "ДА" : "НЕТ"));
                System.out.println("Сохраненный игрок: " + (savedGameState.getPlayer() != null ? "ЕСТЬ" : "НЕТ"));
            } else {
                System.out.println("Нет данных для сохранения, сбрасываем состояние");
                // Сбрасываем сохраненное состояние при переходе в меню без сохранения
                savedGameState = null;
            }
            gameState = new MenuState(this);
        }
        else if(state == PLAY){
            //switch to play state
            // Передаем ProgressionManager, если он есть в data
            if(data instanceof ProgressionManager){
                System.out.println("=== ВОЗВРАТ В ИГРУ С ПРОГРЕССИЕЙ ===");
                gameState = new PlayState(this, (ProgressionManager) data);
                // Восстанавливаем состояние игры, если оно было сохранено
                if(savedGameState != null){
                    System.out.println("Восстанавливаем сохраненное состояние после выбора улучшения");
                    ((PlayState) gameState).restoreGameState(savedGameState);
                    savedGameState = null; // Очищаем сохраненное состояние
                    System.out.println("Состояние восстановлено и очищено");
                } else {
                    System.out.println("Нет сохраненного состояния для восстановления");
                }
            } else {
                System.out.println("=== ВОЗВРАТ В ИГРУ ===");
                System.out.println("Создаем новый PlayState");
                gameState = new PlayState(this);
                // Восстанавливаем состояние игры, если оно было сохранено (при возврате из меню)
                System.out.println("Проверяем savedGameState: " + (savedGameState != null ? "ЕСТЬ" : "НЕТ"));
                if(savedGameState != null){
                    System.out.println("Восстанавливаем сохраненное состояние");
                    System.out.println("Сохраненный игрок: " + (savedGameState.getPlayer() != null ? "ЕСТЬ" : "НЕТ"));
                    ((PlayState) gameState).restoreGameState(savedGameState);
                    savedGameState = null; // Очищаем сохраненное состояние
                    System.out.println("Состояние восстановлено и очищено");
                } else {
                    System.out.println("Нет сохраненного состояния для восстановления");
                }
            }
        }
        else if(state == SETTINGS){
            //switch to settings state
            gameState = new SettingsState(this);
        }
        else if(state == UPGRADE_SELECTION){
            //switch to upgrade selection state
            // Сохраняем состояние игры перед переходом в меню выбора улучшений
            if(gameState instanceof PlayState){
                savedGameState = ((PlayState) gameState).saveGameState();
            }
            ProgressionManager progressionManager = (ProgressionManager) data;
            gameState = new UpgradeSelectionState(this, progressionManager);
        }
        
        // Инициализируем новое состояние
        if(gameState != null){
            gameState.init();
        }
    }

    public void  update(float dt){
        gameState.update(dt);
    }

    public void draw(){
        gameState.draw();
    }
    
    public void handleInput(){
        gameState.handleInput();
    }
    
    public void push(GameState state) {
        if(gameState != null){
            gameState.dispose();
        }
        gameState = state;
    }
}
