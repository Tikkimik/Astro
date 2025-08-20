package com.gdx.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class AndroidInputManager extends InputAdapter {
    private VirtualJoystick movementJoystick;
    private VirtualButton fireButton;
    private VirtualButton zoomInButton;
    private VirtualButton zoomOutButton;
    private VirtualButton resetZoomButton;
    
    private boolean isAndroid;
    
    public AndroidInputManager() {
        // Определяем, запущена ли игра на Android
        isAndroid = Gdx.app.getType().toString().contains("Android");
        
        if (isAndroid) {
            initializeControls();
        }
    }
    
    private void initializeControls() {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();
        
        // Джойстик для движения (левый нижний угол)
        float joystickRadius = Math.min(screenWidth, screenHeight) * 0.15f;
        movementJoystick = new VirtualJoystick(
            joystickRadius + 50, 
            joystickRadius + 50, 
            joystickRadius
        );
        
        // Кнопка стрельбы (правый нижний угол)
        float buttonSize = Math.min(screenWidth, screenHeight) * 0.15f; // Увеличили размер
        fireButton = new VirtualButton(
            screenWidth - buttonSize - 50,
            buttonSize + 50,
            buttonSize,
            buttonSize,
            "FIRE"
        );
        
        // Кнопки масштабирования (правый верхний угол)
        float smallButtonSize = buttonSize * 0.7f; // Увеличили размер
        zoomInButton = new VirtualButton(
            screenWidth - smallButtonSize - 20,
            screenHeight - smallButtonSize - 20,
            smallButtonSize,
            smallButtonSize,
            "+"
        );
        
        zoomOutButton = new VirtualButton(
            screenWidth - smallButtonSize * 2 - 30,
            screenHeight - smallButtonSize - 20,
            smallButtonSize,
            smallButtonSize,
            "-"
        );
        
        resetZoomButton = new VirtualButton(
            screenWidth - smallButtonSize * 3 - 40,
            screenHeight - smallButtonSize - 20,
            smallButtonSize,
            smallButtonSize,
            "R"
        );
    }
    
    public void update() {
        if (!isAndroid) return;
        
        // Обновляем все элементы управления
        movementJoystick.update();
        fireButton.update();
        zoomInButton.update();
        zoomOutButton.update();
        resetZoomButton.update();
        
        // Обновляем состояние клавиш на основе виртуальных элементов
        updateGameKeys();
    }
    
    private void updateGameKeys() {
        // Движение на основе джойстика
        float joystickX = movementJoystick.getX();
        float joystickY = movementJoystick.getY();
        
        // Устанавливаем направление движения
        GameKeys.setKey(GameKeys.UP, joystickY > 0.3f);
        GameKeys.setKey(GameKeys.DOWN, joystickY < -0.3f);
        GameKeys.setKey(GameKeys.LEFT, joystickX < -0.3f);
        GameKeys.setKey(GameKeys.RIGHT, joystickX > 0.3f);
        
        // Стрельба - используем isActive для непрерывной стрельбы
        boolean fireActive = fireButton.isActive();
        GameKeys.setKey(GameKeys.SPACE, fireActive);
        
        // Управление камерой через кнопки - используем isActive для непрерывного действия
        boolean zoomInActive = zoomInButton.isActive();
        boolean zoomOutActive = zoomOutButton.isActive();
        boolean resetActive = resetZoomButton.isActive();
        
        GameKeys.setKey(GameKeys.ZOOM_IN, zoomInActive);
        GameKeys.setKey(GameKeys.ZOOM_OUT, zoomOutActive);
        GameKeys.setKey(GameKeys.ENTER, resetActive);
        
        // Отладочная информация
        if (fireActive || zoomInActive || zoomOutActive || resetActive) {
            System.out.println("Android Debug - Fire: " + fireActive + 
                             ", ZoomIn: " + zoomInActive + 
                             ", ZoomOut: " + zoomOutActive + 
                             ", Reset: " + resetActive);
        }
    }
    
    public void render(ShapeRenderer shapeRenderer) {
        if (!isAndroid) return;
        
        // Включаем прозрачность для UI элементов
        Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
        Gdx.gl.glBlendFunc(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);
        
        // Рендерим все элементы управления
        movementJoystick.render(shapeRenderer);
        fireButton.render(shapeRenderer);
        zoomInButton.render(shapeRenderer);
        zoomOutButton.render(shapeRenderer);
        resetZoomButton.render(shapeRenderer);
        
        Gdx.gl.glDisable(Gdx.gl.GL_BLEND);
    }
    
    // Обработка сенсорного ввода
    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (!isAndroid) return false;
        
        // Проверяем все элементы управления
        if (movementJoystick.touchDown(screenX, screenY, pointer, button)) {
            System.out.println("Android Debug - Joystick touched at: " + screenX + ", " + screenY);
            return true;
        }
        if (fireButton.touchDown(screenX, screenY, pointer, button)) {
            System.out.println("Android Debug - Fire button touched at: " + screenX + ", " + screenY);
            return true;
        }
        if (zoomInButton.touchDown(screenX, screenY, pointer, button)) {
            System.out.println("Android Debug - Zoom In button touched at: " + screenX + ", " + screenY);
            return true;
        }
        if (zoomOutButton.touchDown(screenX, screenY, pointer, button)) {
            System.out.println("Android Debug - Zoom Out button touched at: " + screenX + ", " + screenY);
            return true;
        }
        if (resetZoomButton.touchDown(screenX, screenY, pointer, button)) {
            System.out.println("Android Debug - Reset button touched at: " + screenX + ", " + screenY);
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (!isAndroid) return false;
        
        // Проверяем все элементы управления
        if (movementJoystick.touchDragged(screenX, screenY, pointer)) return true;
        if (fireButton.touchDragged(screenX, screenY, pointer)) return true;
        if (zoomInButton.touchDragged(screenX, screenY, pointer)) return true;
        if (zoomOutButton.touchDragged(screenX, screenY, pointer)) return true;
        if (resetZoomButton.touchDragged(screenX, screenY, pointer)) return true;
        
        return false;
    }
    
    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (!isAndroid) return false;
        
        // Проверяем все элементы управления
        if (movementJoystick.touchUp(screenX, screenY, pointer, button)) return true;
        if (fireButton.touchUp(screenX, screenY, pointer, button)) return true;
        if (zoomInButton.touchUp(screenX, screenY, pointer, button)) return true;
        if (zoomOutButton.touchUp(screenX, screenY, pointer, button)) return true;
        if (resetZoomButton.touchUp(screenX, screenY, pointer, button)) return true;
        
        return false;
    }
    
    public boolean isAndroid() {
        return isAndroid;
    }
    
    public void resize(int width, int height) {
        if (isAndroid) {
            initializeControls();
        }
    }
}


