package com.gdx.managers;


import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.gdx.utils.GameLogger;

public class GameInputProcessor extends InputAdapter {

    public boolean keyDown(int k) {
        GameLogger.input("Key pressed: " + Input.Keys.toString(k));
        
        if(k == Input.Keys.UP) {
            GameKeys.setKey(GameKeys.UP, true);
        }
        if(k == Input.Keys.DOWN) {
            GameKeys.setKey(GameKeys.DOWN, true);
        }
        if(k == Input.Keys.LEFT) {
            GameKeys.setKey(GameKeys.LEFT, true);
        }
        if(k == Input.Keys.RIGHT) {
            GameKeys.setKey(GameKeys.RIGHT, true);
        }
        if(k == Input.Keys.ENTER) {
            GameKeys.setKey(GameKeys.ENTER, true);
        }
        if(k == Input.Keys.ESCAPE) {
            GameKeys.setKey(GameKeys.ESCAPE, true);
        }
        if(k == Input.Keys.SPACE) {
            GameKeys.setKey(GameKeys.SPACE, true);
        }
        if(k == Input.Keys.SHIFT_RIGHT || k == Input.Keys.SHIFT_LEFT) {
            GameKeys.setKey(GameKeys.SHIFT, true);
        }
        if(k == Input.Keys.TAB) {
            GameKeys.setKey(GameKeys.TAB, true);
        }
        if(k == Input.Keys.PLUS || k == Input.Keys.EQUALS) {
            GameKeys.setKey(GameKeys.ZOOM_IN, true);
        }
        if(k == Input.Keys.MINUS) {
            GameKeys.setKey(GameKeys.ZOOM_OUT, true);
        }
        if(k == Input.Keys.F) {
            GameKeys.setKey(GameKeys.TOGGLE_FPS, true);
        }
        if(k == Input.Keys.GRAVE) { // Клавиша ` (тильда)
            GameKeys.setKey(GameKeys.FPS_UP, true);
        }
        if(k == Input.Keys.NUM_1) { // Клавиша 1
            GameKeys.setKey(GameKeys.FPS_DOWN, true);
        }
        if(k == Input.Keys.F11) { // Клавиша F11 для переключения полноэкранного режима
            GameKeys.setKey(GameKeys.TOGGLE_FULLSCREEN, true);
        }
        if(k == Input.Keys.T) { // Клавиша T для тестирования системы прокачки
            GameKeys.setKey(GameKeys.TEST, true);
        }

        return true;
    }

    public boolean keyUp(int k) {
        GameLogger.input("Key released: " + Input.Keys.toString(k));
        
        if(k == Input.Keys.UP) {
            GameKeys.setKey(GameKeys.UP, false);
        }
        if(k == Input.Keys.DOWN) {
            GameKeys.setKey(GameKeys.DOWN, false);
        }
        if(k == Input.Keys.LEFT) {
            GameKeys.setKey(GameKeys.LEFT, false);
        }
        if(k == Input.Keys.RIGHT) {
            GameKeys.setKey(GameKeys.RIGHT, false);
        }
        if(k == Input.Keys.ENTER) {
            GameKeys.setKey(GameKeys.ENTER, false);
        }
        if(k == Input.Keys.ESCAPE) {
            GameKeys.setKey(GameKeys.ESCAPE, false);
        }
        if(k == Input.Keys.SPACE) {
            GameKeys.setKey(GameKeys.SPACE, false);
        }
        if(k == Input.Keys.SHIFT_RIGHT || k == Input.Keys.SHIFT_LEFT) {
            GameKeys.setKey(GameKeys.SHIFT, false);
        }
        if(k == Input.Keys.TAB) {
            GameKeys.setKey(GameKeys.TAB, false);
        }
        if(k == Input.Keys.PLUS || k == Input.Keys.EQUALS) {
            GameKeys.setKey(GameKeys.ZOOM_IN, false);
        }
        if(k == Input.Keys.MINUS) {
            GameKeys.setKey(GameKeys.ZOOM_OUT, false);
        }
        if(k == Input.Keys.F) {
            GameKeys.setKey(GameKeys.TOGGLE_FPS, false);
        }
        if(k == Input.Keys.GRAVE) { // Клавиша ` (тильда)
            GameKeys.setKey(GameKeys.FPS_UP, false);
        }
        if(k == Input.Keys.NUM_1) { // Клавиша 1
            GameKeys.setKey(GameKeys.FPS_DOWN, false);
        }
        if(k == Input.Keys.F11) { // Клавиша F11 для переключения полноэкранного режима
            GameKeys.setKey(GameKeys.TOGGLE_FULLSCREEN, false);
        }
        if(k == Input.Keys.T) { // Клавиша T для тестирования системы прокачки
            GameKeys.setKey(GameKeys.TEST, false);
        }
        return true;
    }

}
