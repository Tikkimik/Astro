package com.gdx.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.gdx.utils.DisplayManager;
import com.gdx.utils.Fonts;
import com.gdx.utils.GameLogger;
import com.gdx.utils.GameSettings;

/**
 * Обработчик состояния "Настройки".
 *
 * Точка возврата определяется по предыдущему состоянию стейт-менеджера
 * (SettingsReturnTarget), Esc возвращает строго в то меню, из которого
 * настройки были открыты. Ввод обрабатывается только этим экраном.
 *
 * Экран разделён на независимые области (сверху вниз, ось Y вверх):
 *   1. Заголовок «Настройки»
 *   2. Вкладки «Игра / Графика»
 *   3. Область параметров (прокручиваемая, обрезается ScissorStack)
 *   4. Описание выбранного параметра (фиксированная строка)
 *   5. Подсказка управления (footer, зарезервировано FOOTER_HEIGHT px)
 *
 * Параметры рисуются в две выровненные колонки: название слева, значение
 * справа. Высота строки считается из font.getLineHeight() + ROW_SPACING,
 * поэтому строки не накладываются друг на друга и на footer. Вся геометрия —
 * в логических координатах viewport через единую uiCamera.
 *
 * Программная проверка раскладки: после каждого пересчёта вычисляются
 * bounding-прямоугольники всех областей и проверяются инварианты
 * (строки не пересекаются, не пересекают footer, scissor внутри viewport,
 * выбранный элемент виден). При нарушении пишется [ERROR]. Полный дамп
 * включается системным свойством astro.layoutDump=1.
 */
public class SettingsStateHandler implements ImprovedGameStateManager.StateHandler {

    /** Куда возвращаться по Esc / пункту «Назад». */
    public enum SettingsReturnTarget {
        MAIN_MENU,
        PAUSE_MENU
    }

    private boolean isInitialized = false;
    private boolean initFailed = false;

    // Графика
    private SpriteBatch spriteBatch;
    private ShapeRenderer shapeRenderer;

    // Ортографическая камера интерфейса: логическое разрешение окна.
    private final OrthographicCamera uiCamera = new OrthographicCamera();
    private final GlyphLayout layout = new GlyphLayout();

    // Точка возврата
    private SettingsReturnTarget returnTarget = SettingsReturnTarget.MAIN_MENU;
    private ImprovedGameStateManager stateManager;

    // Вкладки
    private static final String[] TABS = {"ИГРА", "ГРАФИКА"};
    private static final int TAB_GAMEPLAY = 0;
    private static final int TAB_GRAPHICS = 1;

    // Пункты вкладки «Игра»
    private static final String[] GAMEPLAY_ITEMS = {
        "Логический FPS",
        "Макс. FPS рендера",
        "Макс. частиц",
        "Частицы двигателя",
        "Скорость игрока",
        "Скорость пуль",
        "Назад"
    };

    // Пункты вкладки «Графика»
    private static final String[] GRAPHICS_ITEMS = {
        "V-Sync",
        "Разрешение",
        "Режим окна",
        "Режим отладки",
        "Уровень логов",
        "Назад"
    };

    private int selectedTab = 0;
    private int selectedItem = 0;

    // Анимация
    private float animationTimer = 0f;

    // === Типографика и отступы (логические пиксели, ось Y вверх) ===
    private static final float TITLE_Y_OFFSET = 88f;         // базовая линия заголовка от верха
    private static final float TABS_Y_OFFSET = 155f;         // базовая линия вкладок от верха
    private static final float TABS_CONTENT_GAP = 44f;       // отступ вкладок от области параметров
    private static final float FOOTER_HEIGHT = 60f;          // зарезервированная высота footer (мин. 50–60)
    private static final float FOOTER_BASELINE = 42f;        // базовая линия подсказки от низа
    private static final float ROW_SPACING = 14f;            // зазор между строками (12–18)
    private static final float COLUMN_GAP = 40f;             // зазор между названием и значением
    private static final float DESCRIPTION_GAP = 12f;        // зазор описания от области параметров
    private static final float SCROLL_WHEEL_STEP = 40f;      // px на «щелчок» колеса

    // === Геометрия раскладки (пересчитывается при ресайзе) ===
    private float winW = 0f;
    private float winH = 0f;
    private float lastWidth = 0f;
    private float lastHeight = 0f;

    private float titleBaseline = 0f;
    private float tabsBaseline = 0f;
    private float contentTop = 0f;
    private float contentBottom = 0f;
    private float availableHeight = 0f;
    private float rowHeight = 0f;
    private float bodyLineHeight = 0f;
    private float headingLineHeight = 0f;
    private float descriptionBaseline = 0f;

    private float maxNameWidth = 0f;
    private float maxValueWidth = 0f;
    private float blockX = 0f;
    private float nameX = 0f;
    private float valueX = 0f;

    private float requiredHeight = 0f;
    private float maxScroll = 0f;
    private float scroll = 0f;

    // Переиспользуемый прямоугольник для scissor
    private final Rectangle contentClip = new Rectangle();
    private final Rectangle scissor = new Rectangle();

    // Флаг: геометрия устарела (ресайз или смена вкладки)
    private boolean layoutDirty = true;

    @Override
    public void onEnter() {
        GameLogger.info("=== ВХОД В НАСТРОЙКИ ===");

        // Определяем точку возврата по предыдущему состоянию
        if (stateManager != null
                && stateManager.getPreviousState() == ImprovedGameStateManager.GameState.PAUSED) {
            returnTarget = SettingsReturnTarget.PAUSE_MENU;
            GameLogger.info("Настройки открыты из меню паузы — возврат в паузу");
        } else {
            returnTarget = SettingsReturnTarget.MAIN_MENU;
            GameLogger.info("Настройки открыты из главного меню — возврат в меню");
        }

        if (!isInitialized) {
            initializeSettings();
        }

        // Сбрасываем навигацию на начало
        selectedTab = 0;
        selectedItem = 0;
        scroll = 0f;
        layoutDirty = true;

        GameLogger.info("Настройки активированы");
    }

    @Override
    public void onUpdate(float deltaTime) {
        animationTimer += deltaTime * 2f;
    }

    @Override
    public void onRender() {
        if (!isInitialized) {
            // Даже при ошибке инициализации рисуем безопасный экран с подсказкой возврата
            if (initFailed) {
                renderInitFailed();
            }
            return;
        }

        // При изменении размера окна пересчитываем проекции и раскладку
        if (Gdx.graphics.getWidth() != lastWidth || Gdx.graphics.getHeight() != lastHeight) {
            layoutDirty = true;
        }
        if (layoutDirty) {
            ensureProjection();
            computeLayout();
            ensureSelectedVisible();
            layoutDirty = false;
        }

        // Тёмный непрозрачный фон экрана настроек
        drawBackdrop();

        BitmapFont titleFont = Fonts.getTitleFont();
        BitmapFont headingFont = Fonts.getHeadingFont();
        BitmapFont bodyFont = Fonts.getBodyFont();

        spriteBatch.begin();

        // 1. Заголовок
        titleFont.setColor(Color.WHITE);
        float titleWidth = measure(titleFont, "Настройки");
        titleFont.draw(spriteBatch, "Настройки", (winW - titleWidth) / 2f, titleBaseline);

        // 2. Вкладки
        float tabSpacing = 220f;
        float startX = (winW - (TABS.length - 1) * tabSpacing) / 2f;
        for (int i = 0; i < TABS.length; i++) {
            float x = startX + i * tabSpacing;
            if (i == selectedTab) {
                headingFont.setColor(1f, 1f, 0.45f, 1f);
            } else {
                headingFont.setColor(0.6f, 0.6f, 0.8f, 1f);
            }
            float tabWidth = measure(headingFont, TABS[i]);
            headingFont.draw(spriteBatch, TABS[i], x - tabWidth / 2f, tabsBaseline);
        }

        // 3. Область параметров (прокручиваемая) с обрезкой по scissor
        contentClip.set(0f, contentBottom, winW, availableHeight);
        ScissorStack.calculateScissors(uiCamera, spriteBatch.getTransformMatrix(), contentClip, scissor);
        ScissorStack.pushScissors(scissor);

        String[] items = getItems();
        for (int i = 0; i < items.length; i++) {
            float baseline = getRowBaseline(i);
            boolean selected = (i == selectedItem);

            if (selected) {
                float pulse = 0.85f + 0.15f * (float) Math.sin(animationTimer * 5f);
                headingFont.setColor(1f, 1f, 0.35f, pulse);
            } else {
                headingFont.setColor(0.8f, 0.8f, 1f, 1f);
            }

            headingFont.draw(spriteBatch, items[i], nameX, baseline);

            // Значение — только для параметров, не для «Назад»
            if (i < items.length - 1) {
                headingFont.draw(spriteBatch, getCurrentValueText(selectedTab, i), valueX, baseline);
            }
        }
        ScissorStack.popScissors();

        // 4. Описание выбранного параметра (фиксированная строка над footer)
        bodyFont.setColor(0.7f, 0.7f, 0.75f, 1f);
        String description = ellipsize(bodyFont, getDescription(selectedTab, selectedItem), winW - 80f);
        bodyFont.draw(spriteBatch, description, (winW - measure(bodyFont, description)) / 2f, descriptionBaseline);

        // 5. Footer — подсказка управления (не участвует в курсоре списка)
        bodyFont.setColor(0.6f, 0.6f, 0.65f, 1f);
        String hint = "↑/↓ — выбрать · ←/→ — изменить · Tab — вкладка · Esc — назад";
        float hintWidth = measure(bodyFont, hint);
        bodyFont.draw(spriteBatch, hint, (winW - hintWidth) / 2f, FOOTER_BASELINE);

        spriteBatch.end();
    }

    @Override
    public void onHandleInput() {
        if (!isInitialized && !initFailed) {
            return;
        }

        String[] items = getItems();

        // Esc — назад (в меню паузы или главное меню)
        if (GameKeys.isPressed(GameKeys.ESCAPE)) {
            GameLogger.info("ESC in settings - go back");
            goBack();
            return;
        }

        // Навигация по пунктам (стрелки или W/S)
        if (GameKeys.isPressed(GameKeys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            selectedItem = (selectedItem - 1 + items.length) % items.length;
            ensureSelectedVisible();
        }
        if (GameKeys.isPressed(GameKeys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            selectedItem = (selectedItem + 1) % items.length;
            ensureSelectedVisible();
        }

        // Переключение вкладок клавишей Tab
        if (GameKeys.isPressed(GameKeys.TAB)) {
            switchTab();
            return;
        }

        // Изменение значения выбранного параметра стрелками влево/вправо
        if (GameKeys.isPressed(GameKeys.LEFT) || GameKeys.isPressed(GameKeys.RIGHT)) {
            boolean left = GameKeys.isPressed(GameKeys.LEFT);
            changeValue(selectedTab, selectedItem, left);
        }

        // Enter — подтвердить (пункт «Назад»)
        if (GameKeys.isPressed(GameKeys.ENTER)) {
            if (selectedItem == items.length - 1) {
                goBack();
            }
        }

        // Колесо мыши — прокрутка списка (только список, footer/вкладки не трогаем)
        int wheel = Gdx.input.getDeltaY();
        if (wheel != 0) {
            scroll = clamp(scroll - wheel * SCROLL_WHEEL_STEP, 0f, maxScroll);
        }

        // Мышь: клик по пункту или вкладке
        if (Gdx.input.justTouched()) {
            float mx = Gdx.input.getX();
            float my = Gdx.graphics.getHeight() - Gdx.input.getY();
            handleClick(mx, my);
        }
    }

    @Override
    public void onExit() {
        GameLogger.info("=== ВЫХОД ИЗ НАСТРОЕК ===");
        saveSettingsChanges();
        GameLogger.info("Настройки деактивированы");
    }

    // === ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ===

    private String[] getItems() {
        return selectedTab == TAB_GAMEPLAY ? GAMEPLAY_ITEMS : GRAPHICS_ITEMS;
    }

    private void switchTab() {
        selectedTab = (selectedTab + 1) % TABS.length;
        selectedItem = 0;
        scroll = 0f;
        layoutDirty = true;
        GameLogger.info("Переключена вкладка: " + TABS[selectedTab]);
    }

    private void handleClick(float mx, float my) {
        // Клик по вкладкам
        float tabSpacing = 220f;
        float startX = (winW - (TABS.length - 1) * tabSpacing) / 2f;
        float half = Fonts.getHeadingFont().getLineHeight() * 0.6f;
        for (int i = 0; i < TABS.length; i++) {
            float x = startX + i * tabSpacing;
            float tabWidth = measure(Fonts.getHeadingFont(), TABS[i]);
            if (my > tabsBaseline - half && my < tabsBaseline + half
                    && mx > x - tabWidth / 2f - 20f && mx < x + tabWidth / 2f + 20f) {
                selectedTab = i;
                selectedItem = 0;
                scroll = 0f;
                layoutDirty = true;
                GameLogger.info("Переключена вкладка (клик): " + TABS[selectedTab]);
                return;
            }
        }

        // Клик по пунктам списка (в области прокрутки)
        String[] items = getItems();
        for (int i = 0; i < items.length; i++) {
            float rowTop = contentTop - i * rowHeight + scroll;
            float rowBottom = rowTop - rowHeight;
            if (my <= rowTop && my > rowBottom) {
                selectedItem = i;
                ensureSelectedVisible();
                // Клик по «Назад» возвращает
                if (i == items.length - 1) {
                    goBack();
                }
                return;
            }
        }
    }

    private void goBack() {
        if (stateManager == null) {
            GameLogger.error("SettingsStateHandler: stateManager не установлен, возврат невозможен");
            return;
        }
        if (returnTarget == SettingsReturnTarget.PAUSE_MENU) {
            GameLogger.info("Настройки → меню паузы");
            stateManager.setState(ImprovedGameStateManager.GameState.PAUSED);
        } else {
            GameLogger.info("Настройки → главное меню");
            stateManager.setState(ImprovedGameStateManager.GameState.MENU);
        }
    }

    // === РАСКЛАДКА ===

    /**
     * Пересчитать всю геометрию по текущему размеру окна и вкладке.
     */
    private void computeLayout() {
        winW = Gdx.graphics.getWidth();
        winH = Gdx.graphics.getHeight();

        BitmapFont headingFont = Fonts.getHeadingFont();
        BitmapFont bodyFont = Fonts.getBodyFont();
        headingLineHeight = headingFont.getLineHeight();
        bodyLineHeight = bodyFont.getLineHeight();

        titleBaseline = winH - TITLE_Y_OFFSET;
        tabsBaseline = winH - TABS_Y_OFFSET;
        contentTop = tabsBaseline - TABS_CONTENT_GAP;
        descriptionBaseline = FOOTER_HEIGHT + bodyLineHeight + 13f;
        contentBottom = descriptionBaseline + bodyLineHeight + DESCRIPTION_GAP;
        availableHeight = contentTop - contentBottom;

        // Высота строки: lineHeight + фиксированный зазор (12–18)
        rowHeight = headingLineHeight + ROW_SPACING;

        // Две колонки: ширина по максимуму названий и значений текущей вкладки
        String[] items = getItems();
        maxNameWidth = 0f;
        maxValueWidth = 0f;
        for (int i = 0; i < items.length; i++) {
            maxNameWidth = Math.max(maxNameWidth, measure(headingFont, items[i]));
            if (i < items.length - 1) {
                maxValueWidth = Math.max(maxValueWidth, measure(headingFont, getCurrentValueText(selectedTab, i)));
            }
        }
        float blockWidth = maxNameWidth + COLUMN_GAP + maxValueWidth;
        blockX = (winW - blockWidth) / 2f;
        nameX = blockX;
        valueX = nameX + maxNameWidth + COLUMN_GAP;

        requiredHeight = items.length * rowHeight;
        maxScroll = Math.max(0f, requiredHeight - availableHeight);
        scroll = clamp(scroll, 0f, maxScroll);

        // Программная проверка раскладки
        validateLayout();
    }

    /**
     * Базовая линия строки i с учётом прокрутки.
     */
    private float getRowBaseline(int i) {
        return contentTop - i * rowHeight + scroll - (rowHeight - headingLineHeight) / 2f;
    }

    /**
     * Гарантировать, что выбранная строка видна в области прокрутки.
     */
    private void ensureSelectedVisible() {
        String[] items = getItems();
        int i = Math.min(selectedItem, items.length - 1);
        float rowTop = contentTop - i * rowHeight + scroll;
        float rowBottom = rowTop - rowHeight;
        if (rowTop > contentTop) {
            scroll = clamp(i * rowHeight, 0f, maxScroll);
        } else if (rowBottom < contentBottom) {
            scroll = clamp((i + 1) * rowHeight - availableHeight, 0f, maxScroll);
        }
    }

    /**
     * Программная проверка раскладки: вычисляет bounding-прямоугольники всех
     * областей и проверяет инварианты. Нарушения логируются как [ERROR].
     * Полный диагностический дамп включается системным свойством astro.layoutDump=1.
     */
    private void validateLayout() {
        boolean dump = System.getProperty("astro.layoutDump") != null;
        boolean ok = true;
        String[] items = getItems();

        // Scissor (область списка) внутри viewport
        if (contentTop > winH) { ok = logLayoutError("content top выше viewport"); }
        if (contentBottom < 0f) { ok = logLayoutError("content bottom ниже viewport"); }

        // Первая строка ниже вкладок
        if (contentTop >= tabsBaseline) { ok = logLayoutError("первая строка не ниже вкладок"); }

        // Строки не пересекаются и не выходят за область
        for (int i = 0; i < items.length; i++) {
            float top = contentTop - i * rowHeight + scroll;
            float bottom = top - rowHeight;
            if (bottom < contentBottom - 0.5f && maxScroll <= 0f) {
                // Строка ниже области — проблема только если прокрутка невозможна
                ok = logLayoutError("строка " + i + " ниже области при maxScroll=0");
            }
            // Строки не пересекают footer
            if (bottom < FOOTER_HEIGHT) { ok = logLayoutError("строка " + i + " пересекает footer"); }
            if (i > 0) {
                float prevTop = contentTop - (i - 1) * rowHeight + scroll;
                if (top > prevTop) { ok = logLayoutError("строки " + (i - 1) + " и " + i + " пересекаются"); }
            }
            // Ширина названия/значения в пределах экрана
            float nameWidth = measure(Fonts.getHeadingFont(), items[i]);
            if (nameX + nameWidth > winW || nameX < 0f) { ok = logLayoutError("название строки " + i + " выходит за экран"); }
            if (i < items.length - 1) {
                float valueWidth = measure(Fonts.getHeadingFont(), getCurrentValueText(selectedTab, i));
                if (valueX + valueWidth > winW || valueX < 0f) { ok = logLayoutError("значение строки " + i + " выходит за экран"); }
            }
        }

        // Последняя строка доступна при полной прокрутке
        float lastTop = contentTop - (items.length - 1) * rowHeight + maxScroll;
        if (lastTop - rowHeight < contentBottom - 1f && maxScroll <= 0f) {
            ok = logLayoutError("последняя строка недоступна без прокрутки, но maxScroll=0");
        }

        if (dump) {
            GameLogger.info("=== LAYOUT DUMP ===");
            GameLogger.info("viewport: " + (int) winW + "x" + (int) winH);
            GameLogger.info("titleBaseline=" + fmt(titleBaseline) + " tabsBaseline=" + fmt(tabsBaseline));
            GameLogger.info("content top=" + fmt(contentTop) + " bottom=" + fmt(contentBottom)
                    + " available=" + fmt(availableHeight));
            GameLogger.info("rowHeight=" + fmt(rowHeight) + " items=" + items.length
                    + " required=" + fmt(requiredHeight) + " maxScroll=" + fmt(maxScroll) + " scroll=" + fmt(scroll));
            for (int i = 0; i < items.length; i++) {
                GameLogger.info("row[" + i + "] \"" + items[i] + "\" top=" + fmt(contentTop - i * rowHeight + scroll)
                        + " bottom=" + fmt(contentTop - i * rowHeight + scroll - rowHeight)
                        + " nameW=" + fmt(measure(Fonts.getHeadingFont(), items[i])));
            }
            GameLogger.info("footer reserved=" + fmt(FOOTER_HEIGHT) + " descriptionBaseline=" + fmt(descriptionBaseline)
                    + " footerBaseline=" + fmt(FOOTER_BASELINE));
            GameLogger.info("=== /LAYOUT DUMP ===");
        }

        if (!ok) {
            GameLogger.error("LAYOUT CHECK FAILED: геометрия настроек нарушена, смотрите ошибки выше");
        }
    }

    private boolean logLayoutError(String message) {
        GameLogger.error("LAYOUT: " + message);
        return false;
    }

    private static String fmt(float v) {
        return String.format(java.util.Locale.US, "%.0f", v);
    }

    // === ЗНАЧЕНИЯ И ОПИСАНИЯ ПАРАМЕТРОВ ===

    private String getCurrentValueText(int tab, int item) {
        switch (tab) {
            case TAB_GAMEPLAY:
                switch (item) {
                    case 0: return String.valueOf(GameSettings.getTargetLogicFPS());
                    case 1: return GameSettings.getMaxRenderFPS() == 0
                            ? "Без лимита" : String.valueOf(GameSettings.getMaxRenderFPS());
                    case 2: return String.valueOf(GameSettings.getMaxParticles());
                    case 3: return String.valueOf(GameSettings.getShipEngineParticles());
                    case 4: return String.valueOf((int) GameSettings.getPlayerSpeed());
                    case 5: return String.valueOf((int) GameSettings.getBulletSpeed());
                    default: return "";
                }
            case TAB_GRAPHICS:
                switch (item) {
                    case 0: return GameSettings.isVSyncEnabled() ? "ВКЛ" : "ВЫКЛ";
                    case 1: return DisplayManager.getResolutionName(GameSettings.getResolution());
                    case 2: return getDisplayModeText();
                    case 3: return GameSettings.isDebugMode() ? "ВКЛ" : "ВЫКЛ";
                    case 4: return GameLogger.getCurrentLogLevelName();
                    default: return "";
                }
            default:
                return "";
        }
    }

    private String getDescription(int tab, int item) {
        switch (tab) {
            case TAB_GAMEPLAY:
                switch (item) {
                    case 0: return "Частота игровой логики (30–240). Влияет на число фиксированных обновлений в секунду";
                    case 1: return "Ограничение частоты отрисовки (30–240, 0 = без лимита). Применяется сразу";
                    case 2: return "Глобальный лимит всех частиц: взрывы, пламя, щит, следы ракет (100–5000)";
                    case 3: return "Частиц за кадр логики на двигатель: игрок и вражеские корабли (0–100)";
                    case 4: return "Максимальная скорость корабля (100–1000). Видна в Debug-меню (клавиша F)";
                    case 5: return "Скорость новых пуль (100–1000). Уже летящие пули не меняются";
                    default: return "Вернуться в предыдущее меню";
                }
            case TAB_GRAPHICS:
                switch (item) {
                    case 0: return "Вертикальная синхронизация. Применяется сразу";
                    case 1: return "Разрешение окна (HD/FHD/2K/4K). Применяется сразу";
                    case 2: return "Режим окна: оконный / без рамки / полный экран. Применяется сразу";
                    case 3: return "Подробное отладочное логирование. Применяется сразу";
                    case 4: return "Уровень логирования (NONE–DEBUG). Применяется сразу";
                    default: return "Вернуться в предыдущее меню";
                }
            default:
                return "";
        }
    }

    private String getDisplayModeText() {
        switch (GameSettings.getDisplayMode()) {
            case 0: return "Оконный";
            case 1: return "Без рамки";
            case 2: return "Полный экран";
            default: return "Оконный";
        }
    }

    private void changeValue(int tab, int item, boolean left) {
        switch (tab) {
            case TAB_GAMEPLAY:
                changeGameplayValue(item, left);
                break;
            case TAB_GRAPHICS:
                changeGraphicsValue(item, left);
                break;
            default:
                break;
        }
    }

    private void changeGameplayValue(int item, boolean left) {
        switch (item) {
            case 0: // Логический FPS
                GameSettings.setTargetLogicFPS(GameSettings.getTargetLogicFPS() + (left ? -30 : 30));
                break;
            case 1: // Макс. FPS рендера
                int current = GameSettings.getMaxRenderFPS();
                if (left) {
                    if (current == 0) current = 240;
                    else if (current <= 30) current = 0;
                    else current -= 30;
                } else {
                    if (current == 0) current = 30;
                    else if (current >= 240) current = 0;
                    else current += 30;
                }
                GameSettings.setMaxRenderFPS(current);
                break;
            case 2: // Макс. частиц
                GameSettings.setMaxParticles(GameSettings.getMaxParticles() + (left ? -100 : 100));
                break;
            case 3: // Частицы двигателя
                GameSettings.setShipEngineParticles(GameSettings.getShipEngineParticles() + (left ? -1 : 1));
                break;
            case 4: // Скорость игрока
                GameSettings.setPlayerSpeed(GameSettings.getPlayerSpeed() + (left ? -25 : 25));
                break;
            case 5: // Скорость пуль
                GameSettings.setBulletSpeed(GameSettings.getBulletSpeed() + (left ? -25 : 25));
                break;
            default:
                break;
        }
    }

    private void changeGraphicsValue(int item, boolean left) {
        switch (item) {
            case 0: // V-Sync
                boolean vsync = !GameSettings.isVSyncEnabled();
                GameSettings.setVSyncEnabled(vsync);
                DisplayManager.applyVSync(vsync);
                GameLogger.settings("V-Sync " + (vsync ? "включён" : "выключен"));
                break;
            case 1: // Разрешение
                int res = GameSettings.getResolution();
                res = left ? (res - 1 + 4) % 4 : (res + 1) % 4;
                GameSettings.setResolution(res);
                GameLogger.settings("Разрешение: " + DisplayManager.getResolutionName(res));
                DisplayManager.applyResolution(res);
                break;
            case 2: // Режим окна
                int mode = GameSettings.getDisplayMode();
                mode = left ? (mode - 1 + 3) % 3 : (mode + 1) % 3;
                GameSettings.setDisplayMode(mode);
                GameLogger.settings("Режим окна: " + getDisplayModeText());
                DisplayManager.applyDisplaySettings();
                break;
            case 3: // Режим отладки
                GameSettings.setDebugMode(!GameSettings.isDebugMode());
                GameLogger.settings("Режим отладки " + (GameSettings.isDebugMode() ? "включён" : "выключен"));
                break;
            case 4: // Уровень логов
                int level = GameSettings.getLogLevel();
                level = left ? (level - 1 + 5) % 5 : (level + 1) % 5;
                GameSettings.setLogLevel(level);
                GameLogger.settings("Уровень логов: " + GameLogger.getCurrentLogLevelName());
                break;
            default:
                break;
        }
    }

    // === ГРАФИКА ===

    private void drawBackdrop() {
        Gdx.gl.glClearColor(0.06f, 0.06f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }

    private void renderInitFailed() {
        BitmapFont bodyFont = Fonts.getBodyFont();
        spriteBatch.begin();
        bodyFont.setColor(1f, 0.4f, 0.4f, 1f);
        String msg = "Не удалось загрузить экран настроек. Нажмите Esc для возврата.";
        float winW = Gdx.graphics.getWidth();
        float winH = Gdx.graphics.getHeight();
        float msgWidth = measure(bodyFont, msg);
        bodyFont.draw(spriteBatch, msg, (winW - msgWidth) / 2f, winH * 0.5f);
        spriteBatch.end();
    }

    /**
     * Ширина строки в указанном шрифте (без создания объектов).
     */
    private float measure(BitmapFont font, String text) {
        layout.setText(font, text);
        return layout.width;
    }

    /**
     * Обрезать строку с многоточием, если она шире maxWidth.
     */
    private String ellipsize(BitmapFont font, String text, float maxWidth) {
        if (measure(font, text) <= maxWidth) {
            return text;
        }
        String suffix = "…";
        for (int len = text.length(); len > 0; len--) {
            String candidate = text.substring(0, len) + suffix;
            if (measure(font, candidate) <= maxWidth) {
                return candidate;
            }
        }
        return suffix;
    }

    /**
     * Проекции спрайт-батча и shape-рендерера из единой камеры интерфейса
     * (логическое разрешение окна).
     */
    private void ensureProjection() {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        uiCamera.setToOrtho(false, w, h);
        uiCamera.update();
        spriteBatch.setProjectionMatrix(uiCamera.combined);
        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        lastWidth = w;
        lastHeight = h;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    // === ИНИЦИАЛИЗАЦИЯ ===

    private void initializeSettings() {
        GameLogger.info("Инициализация экрана настроек...");
        try {
            spriteBatch = new SpriteBatch();
            shapeRenderer = new ShapeRenderer();
            ensureProjection();

            // Шрифты с кириллицей (генерируются лениво, кэшируются)
            Fonts.getBodyFont();
            Fonts.getHeadingFont();
            Fonts.getTitleFont();

            isInitialized = true;
            GameLogger.info("Экран настроек инициализирован");
        } catch (Exception e) {
            GameLogger.error("Ошибка инициализации экрана настроек: " + e.getMessage());
            e.printStackTrace();
            initFailed = true;
        }
    }

    private void saveSettingsChanges() {
        GameLogger.info("Сохранение изменений настроек...");
        // Настройки применяются сразу при изменении (сеттеры GameSettings),
        // здесь только фиксируем факт выхода.
        GameLogger.info("Изменения настроек сохранены");
    }

    /**
     * Установить ссылку на менеджер состояний.
     */
    public void setStateManager(ImprovedGameStateManager stateManager) {
        this.stateManager = stateManager;
    }
}
