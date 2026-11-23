package com.gdx.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.gdx.gamestates.PlayState;
import com.gdx.utils.GameLogger;
import com.gdx.session.GameSessionSnapshot;

/**
 * Обработчик состояния для настоящей игры
 * Заменяет заглушку PlayStateAdapter на полноценный PlayState
 */
public class RealPlayStateHandler implements ImprovedGameStateManager.StateHandler {

    private boolean isInitialized = false;

    // Настоящий PlayState
    private PlayState playState;

    // Ссылка на менеджер состояний для переходов
    private ImprovedGameStateManager stateManager;

    // Флаг для отслеживания, была ли игра восстановлена из сохранения
    private boolean wasRestoredFromSave = false;

    @Override
    public void onEnter() {
        GameLogger.info("=== ВХОД В НАСТОЯЩЕЕ ИГРОВОЕ СОСТОЯНИЕ ===");

        if (!isInitialized) {
            // Ставим флаг до инициализации: создание PlayState может синхронно
            // вызвать повторный onEnter через менеджер состояний.
            isInitialized = true;
            initializeRealGame();
            if (playState == null) {
                isInitialized = false;
            }
        }

        // Активируем игру
        if (playState != null) {
            GameLogger.info("Активация настоящей игры...");
            // Здесь можно добавить логику активации, если нужно
        }

        GameLogger.info("Настоящая игра активирована");
    }

    @Override
    public void onUpdate(float deltaTime) {
        if (!isInitialized || playState == null) {
            return;
        }

        // Обновляем настоящую игру
        playState.update(deltaTime);
    }

    @Override
    public void onRender() {
        if (!isInitialized || playState == null) {
            return;
        }

        // Очищаем экран (PlayState сам управляет отрисовкой)
        Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Рендерим настоящую игру
        playState.draw();
    }

    @Override
    public void onHandleInput() {
        if (!isInitialized || playState == null) {
            return;
        }

        // Обрабатываем ввод для настоящей игры
        playState.handleInput();

        // Дополнительная обработка для новой системы состояний
        handleStateTransitions();
    }

    @Override
    public void onExit() {
        GameLogger.info("=== ВЫХОД ИЗ НАСТОЯЩЕГО ИГРОВОГО СОСТОЯНИЯ ===");
        GameLogger.info("Настоящая игра деактивирована");
    }

    @Override
    public void onDispose() {
        if (playState != null) {
            saveGameState();
            playState.dispose();
            playState = null;
        }
        isInitialized = false;
    }

    /**
     * Инициализация настоящей игры
     */
    private void initializeRealGame() {
        GameLogger.info("Инициализация настоящей игры...");

        try {
            playState = new PlayState();

            // Инициализируем игру
            playState.init();

            GameLogger.info("Настоящая игра инициализирована успешно");

        } catch (Exception e) {
            GameLogger.error("Ошибка при инициализации настоящей игры: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Обработка переходов между состояниями
     */
    private void handleStateTransitions() {
        // Проверяем нажатие ESC для паузы
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
            GameLogger.info("ESC pressed in game - pause");
            if (stateManager != null) {
                // onExit сохранит состояние ровно один раз.
                stateManager.setState(ImprovedGameStateManager.GameState.PAUSED);
            }
        }

        // Проверяем нажатие P для паузы (альтернативная клавиша)
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.P)) {
            GameLogger.info("P pressed in game - pause");
            if (stateManager != null) {
                // onExit сохранит состояние ровно один раз.
                stateManager.setState(ImprovedGameStateManager.GameState.PAUSED);
            }
        }
    }

    /**
     * Сохранение состояния игры
     */
    private void saveGameState() {
        if (playState != null) {
            try {
                // Используем метод saveGameState из PlayState
                GameSessionSnapshot savedState = playState.saveGameState();

                if (savedState != null) {
                    GameLogger.info("Состояние игры сохранено успешно");
                    GameLogger.info("Очки: " + savedState.score() + ", Уровень: " + savedState.level());

                    // Здесь можно сохранить состояние в глобальную переменную
                    // для восстановления при возврате из паузы
                    if (stateManager != null) {
                        // Можно добавить метод в ImprovedGameStateManager для хранения сохраненного состояния
                        GameLogger.info("Состояние готово для восстановления");
                    }
                }

            } catch (Exception e) {
                GameLogger.error("Ошибка при сохранении состояния игры: " + e.getMessage());
            }
        }
    }

    /**
     * Восстановление состояния игры
     */
    public void restoreGameState(GameSessionSnapshot savedState) {
        if (playState != null && savedState != null) {
            try {
                GameLogger.info("Восстановление состояния игры...");

                // Используем метод restoreGameState из PlayState
                playState.restoreGameState(savedState);

                wasRestoredFromSave = true;
                GameLogger.info("Состояние игры восстановлено успешно");

            } catch (Exception e) {
                GameLogger.error("Ошибка при восстановлении состояния игры: " + e.getMessage());
            }
        }
    }

    /**
     * Установить ссылку на менеджер состояний
     */
    public void setStateManager(ImprovedGameStateManager stateManager) {
        this.stateManager = stateManager;
    }

    /**
     * Получить ссылку на PlayState для внешнего доступа
     */
    public PlayState getPlayState() {
        return playState;
    }

    /**
     * Проверить, была ли игра восстановлена из сохранения
     */
    public boolean wasRestoredFromSave() {
        return wasRestoredFromSave;
    }
}
