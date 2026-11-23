# Текущая карта владения runtime Astro

## Статус

Карта описывает фактический код после этапа 4 архитектурного рефакторинга. Это диагностический документ, а не целевая модель. Колонка «Пробел» отмечает неоднозначности, которые следующие этапы должны устранять по одной.

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
| `PlayState` | `RealPlayStateHandler` | handler → `PlayState.update()` | handler → `PlayState.draw()` | `RealPlayStateHandler.onDispose()` вызывает `PlayState.dispose()` | Всё ещё совмещает orchestration, renderer и HUD; игровые данные вынесены в `GameSession` |
| `GameSession` | `PlayState` создаёт один раз на новый забег | детерминированное время обновляется из fixed tick | не рисует | живёт и очищается вместе с `PlayState` | `PlayState` пока хранит внутренние быстрые ссылки на объекты сессии до разделения simulation/presentation |
| `Camera` | `PlayState` | `PlayState` | читается всеми render-проходами | явной очистки не требует | Зависит от глобальных размеров `MyGdxGame` |
| `Background` | `PlayState` | `PlayState` | `PlayState` | не выражено | Presentation находится внутри session orchestration |
| `GameHudRenderer` | `PlayState` | обновляет только локальную анимацию HUD из `GameHudView` | владеет HUD-проходами `SpriteBatch`/`ShapeRenderer` | не владеет GPU-ресурсами | Общий font пока разделяется с debug overlay |
| `ProgressionManager` | `GameSession` | через события наград и HUD-анимацию | HUD читает состояние | живёт вместе с session | Presentation пока читает progression напрямую |
| `GameSessionSnapshot` | создаёт `GameSession.createSnapshot()` | — | — | immutable snapshot для round-trip | Коллекции объектов восстанавливаются как in-memory состояние; файловая сериализация пока не реализована |

## Мир и игровые объекты

| Семейство | Создание | Фактический владелец коллекции | Update | Render | Удаление |
|---|---|---|---|---|---|
| `Player` | `PlayState` | прямая ссылка `PlayState` + wrapper в `GameObjectManager.allObjects` | отдельно в `PlayState`; wrapper не обновляет его, так как `Player` не реализует `Updatable` | отдельно в `PlayState` | `Player.dispose()` есть; вызывается только из недостигаемого сейчас `PlayState.dispose()` |
| `Asteroid` | `WorldManager`, split или scenario setup | `GameObjectManager` | `GameObjectManager` | `GameObjectManager` | mark/remove в менеджере |
| `OrkAsteroid` | `WorldManager` или scenario setup | `GameObjectManager.obstacles` | `GameObjectManager` | `GameObjectManager` | mark/remove; стреляет через ссылку на менеджер |
| `EnemyShip` | `WorldManager` или scenario setup | `GameObjectManager.enemies` | `GameObjectManager` | корпус через менеджер, smoke отдельным manager-pass | remove вызывает `EnemyShip.dispose()` |
| `Bullet` | `Player`/scenario, регистрирует `GameObjectManager` | `GameObjectManager.projectiles` | `GameObjectManager` | `GameObjectManager` | wrapper mark/remove; обычные bullet не возвращаются в pool в `removeObject` |
| `OrkBullet` | enemy entity, регистрирует `GameObjectManager` | `GameObjectManager.projectiles` | `GameObjectManager` | `GameObjectManager` | wrapper mark/remove |
| `AutoRocket` | `PlayState`/scenario, регистрирует `GameObjectManager` | `GameObjectManager.projectiles` | `GameObjectManager` | rocket через менеджер, trail отдельным manager-pass | возвращается в `ObjectPools` |
| Секторы мира | `WorldManager` | `WorldManager.loadedSectors` | `WorldManager` | не рисуются напрямую | ключи загруженных секторов не удаляются при cleanup объектов |
| Spatial grid | `WorldManager` | `WorldManager` | перестраивается каждый tick | — | сейчас не используется основным collision detection |

## Частицы и эффекты

| Семейство | Владелец активных объектов | Update | Render | Возврат/очистка | Пробел |
|---|---|---|---|---|---|
| Старые `Particle` explosions | `PlayState.particles` | `PlayState` | `PlayState`/ShapeRenderer | удаляются из массива; не все происходят из одного пула | Параллельный путь с optimized particles |
| `OptimizedParticle` | `PlayState.optimizedParticles` | `PlayState` | `PlayState`/SpriteBatch | `ObjectPools.freeOptimizedParticle()` | Отдельный lifecycle |
| `RocketExplosionEffect` | `PlayState.rocketExplosionEffects` | `PlayState` | два render-прохода `PlayState` | удаляется по `isComplete()` | Presentation effect хранится в gameplay orchestration |
| Player flame/shield/spark | `Player` | `Player.update()` | `Player.draw()` | `Player` возвращает в `ParticlePool` | Сущность управляет render pass и завершает чужой ShapeRenderer |
| Player exhaust trail | `Player`/внутренний renderer | `Player` | внутренний renderer | `Player.dispose()` | В статистике считается вместе с частицами, но lifecycle другой |
| Rocket trail particles | `GameObjectManager.rocketParticles` | `GameObjectManager` | отдельный SpriteBatch-pass менеджера | `ParticlePool.freeRocketParticle()` | Не являются `GameObject`, несмотря на владение менеджером объектов |
| Enemy flame/smoke | `EnemyShip` | `EnemyShip` | flame внутри ship draw, smoke отдельным manager-pass | `EnemyShip.dispose()` | Активные эффекты принадлежат entity, но считает их manager |
| `ParticlePool` | статические массивы | — | — | `MyGdxGame.dispose()` очищает | Глобальное состояние процесса |
| `ObjectPools` | статические LibGDX pools | — | — | общей очистки на shutdown нет | Два независимых семейства пулов без общего контракта |
| `ParticleBudget` | статический глобальный счётчик | обновляется из session/render | влияет на spawn | сбрасывается в `GameObjectManager.clear()` | Зависит от правильной ручной симметрии add/release |

## Столкновения и награды

| Шаг | Текущий владелец | Наблюдение |
|---|---|---|
| Player ↔ obstacle предварительная проверка | `PlayState.checkCollisions()` | Наносит hit и создаёт ship explosion |
| Все основные пары столкновений | `GameObjectManager.checkCollisions()` | Повторно проверяет player ↔ obstacle, а также projectiles/enemies/player |
| Прямой и splash damage | `GameObjectManager` | Связан с type checks и конкретными классами |
| Split обычного астероида | `GameObjectManager` | Создаёт дочерние стадии и вызывает listener |
| Score/experience | listeners, установленные `PlayState` | Правило награды замыкается обратно в `PlayState.addScore()` |
| Collision broad phase | nested loops `GameObjectManager` | Построенный `WorldManager.spatialGrid` не подключён |

## Ввод

| Шаг | Владелец | Наблюдение |
|---|---|---|
| OS events | `GameInputProcessor` или `AndroidInputManager` | Записывают глобальное состояние `GameKeys`/mobile input |
| Fixed tick input | `PlayState.update()` вызывает `handleInput()` | Может выполняться несколько раз за render frame |
| Frame input | `ImprovedGameStateManager.handleInput()` → `RealPlayStateHandler` → `PlayState.handleInput()` | Второй вызов после fixed ticks |
| Edge reset | `MyGdxGame.render()` вызывает `GameKeys.update()` | Происходит один раз в конце render frame |

Двойной путь ввода — первый runtime-кандидат на исправление после завершения baseline.

## Ближайшие решения

1. На этапе 5 разделить simulation pipeline и presentation без изменения боевых контрактов.
2. Убрать внутренние быстрые ссылки `PlayState` после появления отдельного simulation-владельца.
3. Не считать in-memory snapshot форматом долговременного сохранения: для него потребуется отдельный сериализуемый DTO и фабрики сущностей.
4. Пересматривать эту карту после каждого завершённого архитектурного этапа.
