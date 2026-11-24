package com.gdx.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.gdx.gamestates.PlayState;
import com.gdx.utils.GameLogger;
import com.gdx.session.GameSaveData;
import com.gdx.session.GameSaveStore;
import com.gdx.scenarios.ScenarioRuntime;

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

    private GameSaveStore saveStore;
    private boolean persistentLoadRequested;

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
        savePersistentGame();
        GameLogger.info("Настоящая игра деактивирована");
    }

    @Override
    public void onDispose() {
        if (playState != null) {
            savePersistentGame();
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

            if (persistentLoadRequested && saveStore != null) {
                GameSaveData data = saveStore.load();
                if (data != null) {
                    playState.restorePersistentSave(data);
                    GameLogger.info("Сохранение загружено: " + saveStore.path());
                }
            }
            persistentLoadRequested = false;

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

    private void savePersistentGame() {
        if (ScenarioRuntime.isActive() || saveStore == null || playState == null) return;
        if (saveStore.save(playState.createPersistentSave())) {
            GameLogger.info("Игра сохранена: " + saveStore.path());
        }
    }

    public void setSaveStore(GameSaveStore saveStore) {
        this.saveStore = saveStore;
    }

    public boolean canContinue() {
        return playState != null || (saveStore != null && saveStore.hasValidSave());
    }

    public void requestContinue() {
        persistentLoadRequested = playState == null;
    }

    public void requestNewGame() {
        persistentLoadRequested = false;
        if (playState != null) {
            playState.dispose();
            playState = null;
            isInitialized = false;
        }
        if (saveStore != null) saveStore.delete();
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

}
