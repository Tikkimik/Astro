# План рефакторинга core/src

## 🔴 Критические баги

### 1. `GameObjectManager.cleanupEmptyLists()` очищает всё, а не мусор
- **Файл**: `core/src/com/gdx/managers/GameObjectManager.java:364-371`
- **Проблема**: метод вызывает `clear()` на всех быстрых списках (`projectiles`, `obstacles`, `enemies`, `particles`) *каждый кадр*. Коллизии через `GameObjectManager` никогда не срабатывают.
- **Исправление**: удалить `cleanupEmptyLists()` или переписать — удалять только объекты с `shouldRemove() == true`, а не чистить всё.

### 2. `OrkAsteroid.shoot()` — NPE при стрельбе
- **Файл**: `core/src/com/gdx/entities/OrkAsteroid.java:137`
- **Проблема**: `orkBullets` передаётся `null` из `WorldManager` и `PlayState`. При вызове `orkBullets.add(...)` — NPE. Пули орков никогда не создаются.
- **Исправление**: перевести орк-пули на `GameObjectManager` или создать настоящий список.

### 3. `cleanupDistantObjects()` — двойной проход по obstacles
- **Файл**: `core/src/com/gdx/managers/WorldManager.java:186-228`
- **Проблема**: два цикла по `obstacles` — один для `Asteroid`, второй для `OrkAsteroid`. Можно объединить.

## 🟡 Архитектура (полуfinished миграция)

### 4. Две системы состояний
- Старая: `GameStateManager` (int) + `PlayState`, `MenuState`, `SettingsState`, `UpgradeSelectionState`
- Новая: `ImprovedGameStateManager` (enum) + `RealPlayStateHandler`, `MenuStateHandler` и т.д.
- **Реально работает только новая**, но старые классы висят мёртвым грузом.
- **План**: удалить старые `GameState`, `GameStateManager`, `PlayState`, `MenuState`, `SettingsState`, `UpgradeSelectionState` — перенести логику в соответствующие Handler-ы.

### 5. Дубли `MenuState` / `MenuStateHandler`, `SettingsState` / `SettingsStateHandler` и т.д.
- Один и тот же код рендеринга меню существует в двух файлах.
- **План**: удалить дубликаты старой системы.

### 6. `PlayState.init()` вызывается через `new PlayState(null)`
- В `RealPlayStateHandler.initializeRealGame()` старый `GameStateManager` передаётся как `null`. Все методы `PlayState`, которые используют `gameStateManager.setState(...)`, упадут с NPE (если будут вызваны).

## 🟠 Рендеринг

### 7. `Player.draw()` ломает ShapeRenderer
- **Файл**: `core/src/com/gdx/entities/Player.java:578-593`
- Вызывает `shapeRenderer.end()` внутри чужого `begin()`. Игра работает случайно — только потому что `PlayState.draw()` перезапускает `ShapeType` следом.
- **План**: реструктурировать рендеринг — убрать `end/begin` из `Player.draw()`, вынести частицы в отдельный проход.

### 8. `Particle.draw()` — неверные координаты
- **Файл**: `core/src/com/gdx/entities/Particle.java:50-51`
- `camera.worldToScreenX(x - width/2)` — оффсет применяется к мировым координатам до трансформации.
- **План**: `worldToScreenX(x)`, затем рисовать на `screenX - width/2`.

### 9. `AutoRocket.draw()` — двойной triangle
- **Файл**: `core/src/com/gdx/entities/AutoRocket.java:199-203`
- Треугольник рисуется дважды: сначала оранжевый, потом белый поверх — как "outline", но оба `triangle()` (залитые).

### 10. `EnemyShip` создаёт частицы без пула
- **Файл**: `core/src/com/gdx/entities/EnemyShip.java:241`
- `new FlameParticle(...)` вместо `ParticlePool.obtainFlameParticle()`. У `Player` через пул — разнобой.

## 🔵 Производительность и логи

### 11. Лог-спам по каждому нажатию клавиши
- `GameInputProcessor` + `GameKeys` логируют каждый `keyDown/keyUp`. С `debugMode = true` — сотни строк в секунду.
- **План**: убрать `GameLogger.input()` и `GameLogger.debug()` из `GameKeys.setKey()`.

### 12. Профилирование считается каждый кадр
- **Файл**: `core/src/com/gdx/gamestates/PlayState.java:580`
- Условие `currentFPS < 100` всегда true (FPS > 250). `cameraTime`, `backgroundTime` и т.д. вычисляются даже когда `showFPS == false`.
- **План**: завернуть замеры в `if (showFPS)`.

## ⚫ Мёртвый код

### 13. Неиспользуемые поля
- `MyGdxGame.img` (Texture), `MyGdxGame.batch` (SpriteBatch)
- `PlayState.getAutoRockets()` возвращает пустой `new Array<>()`
- `ObjectPools.obtainAutoRocket()` — вызывает `pool.obtain()`, затем `return new AutoRocket(...)`, выбросив полученный объект

### 14. Неиспользуемые файлы шейдеров?
- `FlameShader` загружает `shaders/flame.vert` и `shaders/flame.frag` — проверить, существуют ли они.
