# Текущая карта владения runtime Astro

## Статус

Карта описывает фактический код в ходе этапа 6 архитектурного рефакторинга. Это диагностический документ, а не целевая модель. Колонка «Пробел» отмечает неоднозначности, которые следующие этапы должны устранять по одной.

## Приложение и экраны

| Объект/ресурс | Создаёт и хранит | Update/input | Render | Очистка сейчас | Пробел |
|---|---|---|---|---|---|
| `ImprovedGameStateManager` | `MyGdxGame` | `MyGdxGame.render()` | вызывает активный handler | `MyGdxGame.dispose()` вызывает `dispose()` менеджера | Менеджер очищает registry, но у handler-контракта нет `dispose` |
| `MenuStateHandler` | создаётся в `MyGdxGame`, хранится state manager | handler | handler | только `onExit` | `SpriteBatch` и `ShapeRenderer` окончательно не освобождаются |
| `PausedStateHandler` | создаётся в `MyGdxGame`, хранится state manager | handler | handler поверх `PLAYING` | только `onExit` | lifecycle модального экрана выражен специальным `underlayState` |
| `SettingsStateHandler` | создаётся в `MyGdxGame`, хранится state manager | handler | handler | только `onExit` | GPU-ресурсы не имеют окончательного владельца очистки |
| `RealPlayStateHandler` | создаётся в `MyGdxGame`, хранится state manager | делегирует `PlayState` | делегирует `PlayState` | `onExit` сохраняет, но не вызывает `PlayState.dispose()` | Выход, сохранение и окончательная очистка смешаны |
| Общие UI-шрифты `Fonts` | статический кэш `Fonts` | — | экраны UI | `Fonts.dispose()` существует, но приложение его не вызывает | Нужен один application-level владелец |

## Игровая сессия

| Объект/ресурс | Создаёт и хранит | Update | Render | Удаление/очистка | Пробел |
|---|---|---|---|---|---|
| `PlayState` | `RealPlayStateHandler` | координирует `GameSimulation`, presentation и renderer-компоненты | вызывает world → HUD → debug → mobile UI | `RealPlayStateHandler.onDispose()` вызывает `PlayState.dispose()` | Всё ещё создаёт общие GPU-ресурсы и собирает view-снимки |
| `GameSession` | `PlayState` создаёт один раз на новый забег | детерминированное время обновляет `GameSimulation` | не рисует | живёт и очищается вместе с `PlayState` | Владеет `Player`; `PlayState` пока хранит быстрые ссылки для инициализации и render-view |
| `GameSimulation` | `PlayState` | применяет controls, обновляет session/world/player/objects, targeting и collision | не рисует | живёт вместе с `PlayState` | Разделён на `beginTick/finishTick`, чтобы сохранить presentation update между историческими фазами pipeline |
| `Camera` | `GamePresentation` | `GamePresentation.updateView()` | читается render-проходами через presentation | явной очистки не требует | Зависит от глобальных размеров `MyGdxGame` |
| `Background` | `GamePresentation` | `GamePresentation.updateView()` | пока вызывается координатором `PlayState` | живёт вместе с presentation | Render-проходы мира ещё не собраны в отдельный renderer |
| `GameHudRenderer` | `PlayState` | обновляет только локальную анимацию HUD из `GameHudView` | владеет HUD-проходами `SpriteBatch`/`ShapeRenderer` | не владеет GPU-ресурсами | Общий font пока разделяется с debug overlay |
| `GameDebugOverlay` | `PlayState` | не обновляет simulation; получает `GameDebugView` только при включённом overlay | владеет текстовым debug-проходом | не владеет GPU-ресурсами | Общий font принадлежит `PlayState`; timing-поля пока собирает координатор |
| `GameWorldRenderer` | `PlayState` | не обновляет simulation | владеет порядком background/world/player/effects, общими render-pass и `RenderStats` | не владеет GPU-ресурсами | Engine plume и exhaust используют собственные специализированные renderer-компоненты |
| `ProgressionManager` | `GameSession` | через события наград и HUD-анимацию | HUD читает состояние | живёт вместе с session | Presentation пока читает progression напрямую |
| `GameSessionSnapshot` | создаёт `GameSession.createSnapshot()` | — | — | immutable snapshot для round-trip | Коллекции объектов восстанавливаются как in-memory состояние; файловая сериализация пока не реализована |

## Мир и игровые объекты

| Семейство | Создание | Фактический владелец коллекции | Update | Render | Удаление |
|---|---|---|---|---|---|
| `Player` | `GameSession`; `GameObjectManager` хранит прямой collision-индекс | отдельно через `GameSimulation` | отдельные методы проходов вызывает `GameWorldRenderer` | `Player.dispose()` при финальной очистке `PlayState` | Не входит в `allObjects`, но учитывается менеджером в active/created статистике |
| `Asteroid` | `WorldManager`, split или scenario setup | `GameObjectManager.obstacles` | `GameObjectManager` | `GameObjectManager` | `SpaceObject` + `Damageable` + `Targetable`; здоровье через `HealthState`, без ложного наследования `Enemy` |
| `OrkAsteroid` | `WorldManager` или scenario setup | `GameObjectManager.obstacles` | `GameObjectManager` | `GameObjectManager` | mark/remove; стреляет через ссылку на менеджер |
| `EnemyShip` | `WorldManager` или scenario setup | `GameObjectManager.enemies` | `GameObjectManager` | корпус через менеджер, smoke отдельным manager-pass | remove вызывает `EnemyShip.dispose()` |
| `Bullet` | `Player`/scenario, регистрирует `GameObjectManager` | `GameObjectManager.projectiles` | `GameObjectManager` | `GameObjectManager` | wrapper mark/remove; создаётся напрямую и после удаления собирается GC |
| `OrkBullet` | enemy entity, регистрирует `GameObjectManager` | `GameObjectManager.projectiles` | `GameObjectManager` | `GameObjectManager` | wrapper mark/remove; создаётся напрямую и после удаления собирается GC |
| `AutoRocket` | `GameSimulation`/scenario, регистрирует `GameObjectManager` | `GameObjectManager.projectiles` | `GameObjectManager` | rocket через менеджер, trail отдельным manager-pass | обычное удаление и полная очистка возвращают в `ObjectPools` через единый release-path |
| Секторы мира | `WorldManager` | `WorldManager.loadedSectors` | `WorldManager` | не рисуются напрямую | ключи загруженных секторов не удаляются при cleanup объектов |
| Spatial grid | `WorldManager` | `WorldManager` | перестраивается каждый tick | — | сейчас не используется основным collision detection |

## Частицы и эффекты

| Семейство | Владелец активных объектов | Update | Render | Возврат/очистка | Пробел |
|---|---|---|---|---|---|
| Старые `Particle` explosions | `GamePresentation.particles` | `GamePresentation.updateEffects()` | `PlayState`/ShapeRenderer | естественное завершение, eviction и `dispose` возвращают в `ObjectPools` | Параллельный путь с optimized particles |
| `OptimizedParticle` | `GamePresentation.optimizedParticles` | `GamePresentation.updateEffects()` | `PlayState`/SpriteBatch | presentation возвращает в `ObjectPools` | Render пока остаётся в координаторе |
| `RocketExplosionEffect` | `GamePresentation.rocketExplosionEffects` | `GamePresentation.updateEffects()` | два render-прохода `PlayState` | presentation удаляет по `isComplete()` | Render пока остаётся в координаторе |
| Player flame/shield/spark | `Player` | `Player.update()` | отдельные методы `Player` внутри проходов `GameWorldRenderer` | `Player` возвращает в `ParticlePool` | Сущность хранит эффекты, но больше не управляет общими begin/end |
| Player exhaust trail | `Player`/внутренний renderer | `Player` | внутренний renderer | `Player.dispose()` | В статистике считается вместе с частицами, но lifecycle другой |
| Rocket trail particles | `GameObjectManager.rocketParticles` | `GameObjectManager` | отдельный SpriteBatch-pass менеджера | `ParticlePool.freeRocketParticle()` | Не являются `GameObject`, несмотря на владение менеджером объектов |
| Enemy flame/smoke | `EnemyShip` | `EnemyShip` | flame внутри ship draw, smoke отдельным manager-pass | `EnemyShip.dispose()` | Активные эффекты принадлежат entity, но считает их manager |
| `ParticlePool` | статические массивы | — | — | `MyGdxGame.dispose()` очищает | Глобальное состояние процесса |
| `ObjectPools` | статические LibGDX pools | — | — | `MyGdxGame.dispose()` очищает после финализации экранов; `AutoRocket.reset()` разрывает session-ссылки | Пулы flame/shield/rocket удалены как мёртвые дубли `ParticlePool`; остальные контракты ещё проверяются |
| `ParticleBudget` | статический глобальный счётчик | обновляется из session/render | влияет на spawn | сбрасывается в `GameObjectManager.clear()` | Зависит от правильной ручной симметрии add/release |

## Столкновения и награды

| Шаг | Текущий владелец | Наблюдение |
|---|---|---|
| Все основные пары столкновений | `GameObjectManager.checkCollisions()` | Единственный detection-проход для projectiles, obstacles, enemies и player; визуальный эффект player ↔ obstacle отправляется listener-событием |
| Projectile collision resolution | `GameObjectManager` | Получает явную пару после detection; применяет прямой и splash damage через `Damageable`, затем выполняет типовое последствие уничтожения |
| Split обычного астероида | `GameObjectManager` | Создаёт дочерние стадии и вызывает listener |
| Уничтожение | `GameObjectManager` публикует `DestructionEvent` нескольким подписчикам | Событие содержит цель, источник и причину direct/splash/player collision |
| Score/experience | `CombatRewardRules` вычисляет награду, `GameSession` применяет | `PlayState` маршрутизирует событие в presentation и session, но больше не содержит типовую формулу награды |
| Collision broad phase | nested loops `GameObjectManager` | Построенный `WorldManager.spatialGrid` не подключён |
| Автонаведение | `GameObjectManager.findNearestTarget()` + `AutoRocket` | Поиск и хранение цели зависят от `Targetable`, а не от иерархии `Enemy` |

## Ввод

| Шаг | Владелец | Наблюдение |
|---|---|---|
| OS events | `GameInputProcessor` или `AndroidInputManager` | Записывают глобальное состояние `GameKeys`/mobile input |
| Fixed tick input | `PlayState.update()` передаёт стабильный `PlayerControls` в `GameSimulation` | Один снимок непрерывного ввода применяется ко всем fixed ticks кадра |
| Frame input | `ImprovedGameStateManager.handleInput()` → `RealPlayStateHandler` → `PlayState.handleInput()` | Снимает held-состояние и отдельно потребляет одноразовые команды |
| Edge reset | `MyGdxGame.render()` вызывает `GameKeys.update()` | Происходит один раз в конце render frame |

Повторное применение одноразовых команд устранено на этапе 1; непрерывное состояние намеренно повторяется на каждом fixed tick.

## Ближайшие решения

1. На этапе 7 отделить collision detection от урона и наград, сохраняя за `GameObjectManager` владение runtime-коллекциями.
2. Убирать внутренние быстрые ссылки `PlayState` только после появления единственного владельца объектов.
3. Не считать in-memory snapshot форматом долговременного сохранения: для него потребуется отдельный сериализуемый DTO и фабрики сущностей.
4. Пересматривать эту карту после каждого завершённого архитектурного этапа.
