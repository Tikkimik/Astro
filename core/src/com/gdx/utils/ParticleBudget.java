package com.gdx.utils;

import com.badlogic.gdx.utils.Array;
import com.gdx.entities.FlameParticle;
import com.gdx.entities.RocketParticle;
import com.gdx.entities.ShieldParticle;
import com.gdx.entities.SpaceObject;
import com.gdx.entities.OptimizedParticle;
import com.gdx.entities.ExplosionParticle;
import com.gdx.entities.EnemyFlameParticle;
import com.gdx.entities.Particle;

/**
 * Двухуровневый лимит частиц.
 *
 * Hard active limit — внутренний защитный максимум ВСЕХ живых частиц,
 * предохраняющий от утечек памяти и неконтролируемой нагрузки CPU.
 *
 * Visible/render budget — настройка «Макс. частиц» (GameSettings.getMaxParticles):
 * максимальное число частиц, которое разрешено отображать ВНУТРИ области камеры.
 * Внеэкранные декоративные частицы не тратят видимый бюджет и не блокируют
 * появление эффектов на экране.
 *
 * При попытке создать видимую частицу, когда лимиты заполнены, слот освобождается
 * по приоритетам:
 *   1) старейшая внеэкранная декоративная частица;
 *   2) старейшая внеэкранная частица (низкоприоритетная);
 *   3) старейшая видимая декоративная частица;
 *   4) иначе — отказ от создания.
 *
 * Границы камеры копируются сюда каждый кадр (PlayState), чтобы решения о спавне
 * в игровой логике не зависели от рендера.
 */
public final class ParticleBudget {

    private static int active = 0;
    private static int visible = 0;
    private static int hardLimit = 4000;

    // Текущие границы камеры (обновляются каждый кадр из PlayState).
    private static float renderLeft, renderRight, renderBottom, renderTop;
    private static float spawnLeft, spawnRight, spawnBottom, spawnTop;

    // Реестр живых коллекций частиц для вытеснения внеэкранных частиц.
    // Хранятся сырые Array: частицы одного типа, все наследники SpaceObject.
    private static final Array<Array> collections = new Array<>();

    private ParticleBudget() {
    }

    /**
     * Копия границ камеры из PlayState (после обновления камеры).
     */
    public static void updateBounds(CameraBounds bounds) {
        renderLeft = bounds.renderLeft;
        renderRight = bounds.renderRight;
        renderBottom = bounds.renderBottom;
        renderTop = bounds.renderTop;
        spawnLeft = bounds.spawnLeft;
        spawnRight = bounds.spawnRight;
        spawnBottom = bounds.spawnBottom;
        spawnTop = bounds.spawnTop;
    }

    /**
     * Простые числа-границы для тестов и прямого вызова.
     */
    public static void updateBounds(float rl, float rr, float rb, float rt,
                                    float sl, float sr, float sb, float st) {
        renderLeft = rl; renderRight = rr; renderBottom = rb; renderTop = rt;
        spawnLeft = sl; spawnRight = sr; spawnBottom = sb; spawnTop = st;
    }

    public static boolean isInRenderBounds(float x, float y) {
        return x >= renderLeft && x <= renderRight
            && y >= renderBottom && y <= renderTop;
    }

    public static boolean isInSpawnBounds(float x, float y) {
        return x >= spawnLeft && x <= spawnRight
            && y >= spawnBottom && y <= spawnTop;
    }

    /**
     * Внутренний защитный предел всех живых частиц.
     */
    public static int getHardLimit() {
        return hardLimit;
    }

    /**
     * Видимый бюджет частиц (настройка «Макс. частиц»).
     */
    public static int getVisibleBudget() {
        return GameSettings.getMaxParticles();
    }

    /**
     * Число частиц, считающихся сейчас активными (все источники).
     */
    public static int getActive() {
        return active;
    }

    /**
     * Число видимых частиц по данным последнего render-кадра.
     */
    public static int getVisibleCount() {
        return visible;
    }

    /**
     * Обновить счётчик видимых частиц по итогам render-кадра.
     */
    public static void setVisible(int count) {
        visible = Math.max(0, count);
    }

    /**
     * Хватит ли бюджета на частицу в точке (x, y)?
     * Если точка внутри видимой области — учитывается видимый бюджет и
     * вызывается вытеснение при заполнении. Если снаружи — только hard limit.
     */
    public static boolean canSpawn(float x, float y) {
        if (isInRenderBounds(x, y)) {
            if (active < hardLimit && visible < getVisibleBudget()) {
                return true;
            }
            return evictForVisibleSpawn();
        }
        return active < hardLimit;
    }

    /**
     * Зарезервировать один слот под создаваемую частицу.
     */
    public static void add(int n) {
        active += n;
    }

    /**
     * Освободить слоты при удалении частиц.
     */
    public static void release(int n) {
        active = Math.max(0, active - n);
    }

    // === РЕЕСТР КОЛЛЕКЦИЙ ===

    /**
     * Зарегистрировать коллекцию частиц для вытеснения внеэкранных частиц.
     */
    public static void register(Array<?> collection) {
        if (!collections.contains(collection, true)) {
            collections.add(collection);
        }
    }

    /**
     * Убрать коллекцию из реестра (объект удалён из мира).
     */
    public static void unregister(Array<?> collection) {
        collections.removeValue(collection, true);
    }

    // === ВЫТЕСНЕНИЕ ===

    private static boolean evictForVisibleSpawn() {
        if (removeOffscreenDecorative()) return true;
        if (removeOffscreenAny()) return true;
        if (removeVisibleDecorative()) return true;
        return false;
    }

    private static boolean removeOffscreenDecorative() {
        for (int c = 0; c < collections.size; c++) {
            Array coll = collections.get(c);
            for (int i = 0; i < coll.size; i++) {
                Object o = coll.get(i);
                if (o instanceof SpaceObject && isDecorative((SpaceObject) o)
                        && !isInRenderBounds(((SpaceObject) o).getX(), ((SpaceObject) o).getY())) {
                    removeAt(coll, i, (SpaceObject) o);
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean removeOffscreenAny() {
        for (int c = 0; c < collections.size; c++) {
            Array coll = collections.get(c);
            for (int i = 0; i < coll.size; i++) {
                Object o = coll.get(i);
                if (o instanceof SpaceObject
                        && !isInRenderBounds(((SpaceObject) o).getX(), ((SpaceObject) o).getY())) {
                    removeAt(coll, i, (SpaceObject) o);
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean removeVisibleDecorative() {
        for (int c = 0; c < collections.size; c++) {
            Array coll = collections.get(c);
            for (int i = 0; i < coll.size; i++) {
                Object o = coll.get(i);
                if (o instanceof SpaceObject && isDecorative((SpaceObject) o)
                        && isInRenderBounds(((SpaceObject) o).getX(), ((SpaceObject) o).getY())) {
                    removeAt(coll, i, (SpaceObject) o);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Чисто декоративная частица (пламя, щит, след): не влияет на механику,
     * её можно вытеснить первой.
     */
    private static boolean isDecorative(SpaceObject p) {
        if (p instanceof FlameParticle || p instanceof ShieldParticle || p instanceof RocketParticle
                || p instanceof EnemyFlameParticle) {
            return true;
        }
        if (p instanceof OptimizedParticle) {
            String type = ((OptimizedParticle) p).getParticleType();
            return !"explosion".equals(type);
        }
        return false;
    }

    private static void removeAt(Array coll, int index, SpaceObject p) {
        coll.removeIndex(index);
        freeParticle(p);
        release(1);
    }

    /**
     * Вернуть частицу в её пул (если он существует).
     */
    private static void freeParticle(SpaceObject p) {
        if (p instanceof FlameParticle) {
            ParticlePool.freeFlameParticle((FlameParticle) p);
        } else if (p instanceof ShieldParticle) {
            ParticlePool.freeShieldParticle((ShieldParticle) p);
        } else if (p instanceof RocketParticle) {
            ParticlePool.freeRocketParticle((RocketParticle) p);
        } else if (p instanceof EnemyFlameParticle) {
            ParticlePool.freeEnemyFlameParticle((EnemyFlameParticle) p);
        } else if (p instanceof OptimizedParticle) {
            ObjectPools.freeOptimizedParticle((OptimizedParticle) p);
        } else if (p instanceof ExplosionParticle) {
            ObjectPools.freeExplosionParticle((ExplosionParticle) p);
        } else if (p instanceof Particle) {
            ObjectPools.freeParticle((Particle) p);
        }
    }

    /**
     * Полный сброс счётчиков (очистка мира). Реестр коллекций сохраняется:
     * массивы живут всё время существования игрового состояния.
     */
    public static void reset() {
        active = 0;
        visible = 0;
    }
}
