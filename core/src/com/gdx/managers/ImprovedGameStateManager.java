package com.gdx.managers;

import com.badlogic.gdx.utils.Array;
import com.gdx.utils.GameLogger;
import java.util.HashMap;
import java.util.Map;

/**
 * Улучшенная система управления состояниями 
 * 
 * Ключевые принципы:
 * 1. Простые состояния через enum
 * 2. События при изменении состояния
 * 3. Централизованное управление
 * 4. Разделение логики и состояния
 */
public class ImprovedGameStateManager {
    
    // Текущее состояние
    private GameState currentState = null;
    
    // Предыдущее состояние для возможности отката
    private GameState previousState = null;
    
    // Игровое состояние, которое рисуется фоном под модальными экранами
    // (пауза и настройки, открытые из паузы). Позволяет видеть застывшую игру
    // за меню паузы даже после SETTINGS -> PAUSED.
    private GameState underlayState = null;
    
    // Стек состояний для push/pop операций
    private Array<GameState> stateStack = new Array<>();
    
    // Регистрированные обработчики состояний
    private Map<GameState, StateHandler> stateHandlers = new HashMap<>();
    
    // Обработчики событий изменения состояния
    private Array<StateChangeListener> stateChangeListeners = new Array<>();
    
    /**
     * Enum для всех возможных состояний игры
     */
    public enum GameState {
        MENU("Главное меню"),
        PLAYING("Игровой процесс"),
        PAUSED("Пауза"),
        SETTINGS("Настройки"),
        UPGRADE_SELECTION("Выбор улучшений"),
        GAME_OVER("Игра окончена"),
        VICTORY("Победа");
        
        private final String description;
        
        GameState(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Интерфейс для обработчиков состояний
     */
    public interface StateHandler {
        void onEnter();
        void onUpdate(float deltaTime);
        void onRender();
        void onHandleInput();
        void onExit();
    }
    
    /**
     * Интерфейс для слушателей изменения состояния
     */
    public interface StateChangeListener {
        void onStateChanged(GameState from, GameState to);
    }
    
    /**
     * Событие изменения состояния
     */
    public static class StateChangeEvent {
        public final GameState from;
        public final GameState to;
        public final long timestamp;
        
        public StateChangeEvent(GameState from, GameState to) {
            this.from = from;
            this.to = to;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    public ImprovedGameStateManager() {
        // Инициализируем обработчики по умолчанию
        initializeDefaultHandlers();
        
        // Принудительно входим в начальное состояние
        if (currentState != null) {
            StateHandler handler = stateHandlers.get(currentState);
            if (handler != null) {
                handler.onEnter();
            }
        }
    }
    
    /**
     * Инициализируем обработчики по умолчанию
     */
    private void initializeDefaultHandlers() {
        // Здесь можно зарегистрировать обработчики для каждого состояния
        // Например:
        // registerStateHandler(GameState.MENU, new MenuStateHandler());
        // registerStateHandler(GameState.PLAYING, new PlayingStateHandler());
    }
    
    /**
     * Регистрируем обработчик для состояния
     */
    public void registerStateHandler(GameState state, StateHandler handler) {
        stateHandlers.put(state, handler);
        GameLogger.info("Зарегистрирован обработчик для состояния: " + state.getDescription());
    }
    
    /**
     * Добавляем слушателя изменения состояния
     */
    public void addStateChangeListener(StateChangeListener listener) {
        stateChangeListeners.add(listener);
    }
    
    /**
     * Убираем слушателя изменения состояния
     */
    public void removeStateChangeListener(StateChangeListener listener) {
        stateChangeListeners.removeValue(listener, true);
    }
    
    /**
     * Изменяем состояние (основной метод)
     */
    public void setState(GameState newState) {
        GameLogger.info("setState вызван с: " + newState.getDescription());
        
        // Проверяем, не пытаемся ли установить то же состояние
        if (currentState == newState) {
            GameLogger.debug("Попытка установить то же состояние: " + newState.getDescription());
            return;
        }
        
        GameState oldState = currentState;
        GameLogger.info("Текущее состояние: " + (oldState != null ? oldState.getDescription() : "null"));
        
        // Выходим из текущего состояния
        if (currentState != null) {
            StateHandler currentHandler = stateHandlers.get(currentState);
            if (currentHandler != null) {
                GameLogger.info("Выходим из текущего состояния: " + currentState.getDescription());
                currentHandler.onExit();
            }
        }
        
        // Сохраняем предыдущее состояние
        previousState = oldState;

        // Отслеживаем игровое состояние под модальными экранами.
        // При входе в паузу из игры фиксируем PLAYING как фон; при переходе
        // SETTINGS -> PAUSED фон сохраняется, чтобы игра была видна за меню паузы.
        if (newState == GameState.PLAYING) {
            underlayState = null;
        } else if (newState == GameState.MENU) {
            underlayState = null;
        } else if (oldState == GameState.PLAYING && newState == GameState.PAUSED) {
            underlayState = GameState.PLAYING;
        }
        
        // Устанавливаем новое состояние
        currentState = newState;
        GameLogger.info("Новое состояние установлено: " + newState.getDescription());
        
        // Входим в новое состояние
        StateHandler newHandler = stateHandlers.get(newState);
        if (newHandler != null) {
            GameLogger.info("Входим в новое состояние: " + newState.getDescription());
            newHandler.onEnter();
        } else {
            GameLogger.error("Обработчик для состояния " + newState.getDescription() + " не найден!");
        }
        
        // Генерируем событие изменения состояния
        StateChangeEvent event = new StateChangeEvent(oldState, newState);
        fireStateChangeEvent(event);
        
        // Уведомляем всех слушателей
        notifyStateChangeListeners(oldState, newState);
        
        GameLogger.info("Состояние изменено: " + (oldState != null ? oldState.getDescription() : "null") + " -> " + newState.getDescription());
    }
    
    /**
     * Push состояние (сохраняем текущее в стек)
     */
    public void pushState(GameState newState) {
        if (currentState != null) {
            stateStack.add(currentState);
        }
        setState(newState);
    }
    
    /**
     * Pop состояние (возвращаемся к предыдущему из стека)
     */
    public void popState() {
        if (!stateStack.isEmpty()) {
            GameState previousState = stateStack.pop();
            setState(previousState);
        } else {
            GameLogger.warn("Попытка pop состояния из пустого стека");
        }
    }
    
    /**
     * Возвращаемся к предыдущему состоянию
     */
    public void goBack() {
        if (previousState != null) {
            setState(previousState);
        } else {
            GameLogger.warn("Нет предыдущего состояния для возврата");
        }
    }
    
    /**
     * Обновляем текущее состояние
     */
    public void update(float deltaTime) {
        if (currentState != null) {
            StateHandler handler = stateHandlers.get(currentState);
            if (handler != null) {
                handler.onUpdate(deltaTime);
            }
        }
    }
    
    /**
     * Рендерим текущее состояние
     */
    public void render() {
        if (currentState != null) {
            StateHandler handler = stateHandlers.get(currentState);
            if (handler != null) {
                // Если мы в паузе и под ней лежит игра, сначала рисуем её,
                // чтобы она была видна за полупрозрачным оверлеем меню паузы.
                // Настройки рисуются на непрозрачном фоне, под ними игру не рисуем.
                if (currentState == GameState.PAUSED && underlayState == GameState.PLAYING) {
                    StateHandler underlayHandler = stateHandlers.get(GameState.PLAYING);
                    if (underlayHandler != null) {
                        underlayHandler.onRender();
                    }
                }
                
                // Затем рендерим текущее состояние (пауза/настройки)
                handler.onRender();
            }
        }
    }
    
    /**
     * Обрабатываем ввод для текущего состояния
     */
    public void handleInput() {
        if (currentState != null) {
            StateHandler handler = stateHandlers.get(currentState);
            if (handler != null) {
                handler.onHandleInput();
            }
        }
    }
    
    /**
     * Генерируем событие изменения состояния
     */
    private void fireStateChangeEvent(StateChangeEvent event) {
        // Здесь можно добавить глобальную систему событий
        // Events.fire(event);
    }
    
    /**
     * Уведомляем всех слушателей об изменении состояния
     */
    private void notifyStateChangeListeners(GameState from, GameState to) {
        for (StateChangeListener listener : stateChangeListeners) {
            try {
                listener.onStateChanged(from, to);
            } catch (Exception e) {
                GameLogger.error("Ошибка в слушателе изменения состояния: " + e.getMessage());
            }
        }
    }
    
    // Геттеры
    public GameState getCurrentState() {
        return currentState;
    }
    
    public GameState getPreviousState() {
        return previousState;
    }
    
    public boolean isInState(GameState state) {
        return currentState == state;
    }
    
    public boolean isPlaying() {
        return currentState == GameState.PLAYING;
    }
    
    public boolean isPaused() {
        return currentState == GameState.PAUSED;
    }
    
    public boolean isInMenu() {
        return currentState == GameState.MENU;
    }
    
    /**
     * Получить информацию о текущем состоянии
     */
    public String getStateInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Текущее состояние: ").append(currentState.getDescription());
        
        if (previousState != null) {
            info.append("\nПредыдущее состояние: ").append(previousState.getDescription());
        }
        
        if (!stateStack.isEmpty()) {
            info.append("\nРазмер стека состояний: ").append(stateStack.size);
        }
        
        info.append("\nЗарегистрированных обработчиков: ").append(stateHandlers.size());
        info.append("\nСлушателей изменений: ").append(stateChangeListeners.size);
        
        return info.toString();
    }
    
    /**
     * Очистка ресурсов
     */
    public void dispose() {
        // Выходим из текущего состояния
        if (currentState != null) {
            StateHandler handler = stateHandlers.get(currentState);
            if (handler != null) {
                handler.onExit();
            }
        }
        
        // Очищаем все
        stateHandlers.clear();
        stateChangeListeners.clear();
        stateStack.clear();
        
        GameLogger.info("ImprovedGameStateManager очищен");
    }
}
