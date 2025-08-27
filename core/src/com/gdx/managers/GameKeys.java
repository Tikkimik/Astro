package com.gdx.managers;

import com.gdx.utils.GameLogger;

public class GameKeys {

    private static boolean[] keys;
    private static boolean[] pkeys;

    private static final int NUM_KEYS = 16;

    public static final int UP = 0;
    public static final int LEFT = 1;
    public static final int DOWN = 2;
    public static final int RIGHT = 3;

    public static final int ENTER = 4;
    public static final int ESCAPE = 5;
    public static final int SPACE = 6;
    public static final int SHIFT = 7;
    public static final int TAB = 8;
    
    public static final int ZOOM_IN = 9;
    public static final int ZOOM_OUT = 10;
    public static final int TOGGLE_FPS = 11;
    public static final int FPS_UP = 12;    // Увеличить лимит FPS (клавиша `)
    public static final int FPS_DOWN = 13;  // Уменьшить лимит FPS (клавиша 1)
    public static final int TOGGLE_FULLSCREEN = 14; // Переключение полноэкранного режима (F11)
    public static final int TEST = 15;      // Тестовая кнопка для получения опыта

    static {
        keys = new boolean[NUM_KEYS];
        pkeys = new boolean[NUM_KEYS];

    }

    public static void update() {
        for(int i = 0; i < NUM_KEYS; i++) {
            pkeys[i] = keys[i];
        }
    }

    public static void setKey(int k, boolean b) {
        keys[k] = b;
        GameLogger.debug("Key " + getKeyName(k) + " set to " + b);
    }

    public static boolean isDown(int k) {
        return keys[k];
    }

    public static boolean isPressed(int k) {
        boolean pressed = keys[k] && !pkeys[k];
        if (pressed) {
            GameLogger.debug("Key " + getKeyName(k) + " is pressed");
        }
        return pressed;
    }
    
    /**
     * Получить название клавиши по индексу
     */
    private static String getKeyName(int k) {
        switch (k) {
            case UP: return "UP";
            case DOWN: return "DOWN";
            case LEFT: return "LEFT";
            case RIGHT: return "RIGHT";
            case ENTER: return "ENTER";
            case ESCAPE: return "ESCAPE";
            case SPACE: return "SPACE";
            case SHIFT: return "SHIFT";
            case TAB: return "TAB";
            case ZOOM_IN: return "ZOOM_IN";
            case ZOOM_OUT: return "ZOOM_OUT";
            case TOGGLE_FPS: return "TOGGLE_FPS";
            case FPS_UP: return "FPS_UP";
            case FPS_DOWN: return "FPS_DOWN";
            case TOGGLE_FULLSCREEN: return "TOGGLE_FULLSCREEN";
            case TEST: return "TEST";
            default: return "UNKNOWN(" + k + ")";
        }
    }
}
