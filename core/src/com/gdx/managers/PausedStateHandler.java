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
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.gdx.utils.Fonts;
import com.gdx.utils.GameLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Обработчик состояния "Пауза"
 * Показывает меню паузы (Продолжить / Справка / Выйти в меню) и модальную
 * справку, которая открывается подменю внутри состояния паузы — игра при этом
 * остаётся на паузе.
 *
 * Вся геометрия справки считается в ЛОГИЧЕСКИХ координатах viewport (ось Y —
 * вверх). Для отрисовки используется ортографическая камера uiCamera: её
 * combined-матрица ставится в проекции SpriteBatch и ShapeRenderer. Область
 * прокручиваемого текста обрезается через ScissorStack (calculateScissors +
 * push/popScissors) — он сам преобразует логические координаты в физические
 * пиксели бэкбуфера на HiDPI-экранах (HdpiUtils.glScissor).
 */
public class PausedStateHandler implements ImprovedGameStateManager.StateHandler {

    private boolean isInitialized = false;

    // Графика
    private SpriteBatch spriteBatch;
    private ShapeRenderer shapeRenderer;

    // Ортографическая камера интерфейса: логическое разрешение окна.
    private final OrthographicCamera uiCamera = new OrthographicCamera();

    // Переиспользуемые прямоугольники для scissor
    private final Rectangle clipBounds = new Rectangle();
    private final Rectangle scissor = new Rectangle();

    // Меню паузы
    private static final String[] MENU_ITEMS = {"Продолжить", "Настройки", "Справка", "Выйти в меню"};
    private static final int ITEM_RESUME = 0;
    private static final int ITEM_SETTINGS = 1;
    private static final int ITEM_HELP = 2;
    private static final int ITEM_EXIT = 3;

    private static final float MENU_SPACING = 60f;

    private int selectedItem = 0;
    private boolean showHelp = false;

    // Анимация
    private float animationTimer = 0f;

    // === Типографика справки (логические пиксели, ось Y вверх) ===
    private static final float TITLE_BASELINE_OFFSET = 56f;   // от верха до базовой линии заголовка
    private static final float TITLE_TOP_GAP = 40f;           // от низа заголовка до верха содержимого
    private static final float CONTENT_BOTTOM = 100f;         // нижняя граница содержимого
    private static final float FOOTER_BASELINE = 38f;         // базовая линия нижней подсказки
    private static final float MAX_CONTENT_WIDTH = 960f;      // максимум ширины текстовой колонки
    private static final float MIN_SIDE_MARGIN = 48f;         // минимум боковых полей

    private static final float BODY_LINE_SPACING = 1.3f;      // межстрочный интервал
    private static final float PARAGRAPH_GAP = 10f;           // между параметрами/абзацами
    private static final float SECTION_GAP = 26f;             // между разделами
    private static final float HEADER_BOTTOM_GAP = 8f;        // после заголовка раздела

    // === Прокрутка ===
    private static final float SCROLL_SPEED = 400f;           // px/сек при зажатой клавише
    private static final float WHEEL_STEP = 30f;              // px на «щелчок» колеса
    private static final float SCROLLBAR_WIDTH = 4f;
    private static final float SCROLLBAR_MARGIN = 8f;
    private static final float MIN_THUMB_HEIGHT = 20f;

    // Параметры раскладки (логические координаты), пересчитываются при открытии/ресайзе
    private float contentWidth = 0f;
    private float contentX = 0f;
    private float contentTop = 0f;
    private float contentBottom = 0f;
    private float visibleContentHeight = 0f;
    private float titleBaselineY = 0f;
    private float helpContentHeight = 0f;
    private float maxScroll = 0f;
    private float helpScroll = 0f;
    private float bodyLineHeight = 0f;
    private float headingLineHeight = 0f;
    private float lastWidth = 0f;
    private float lastHeight = 0f;

    // Кэшированная раскладка справки: строки + графлэйаут для переносов.
    // Собирается только при открытии справки или изменении размера окна.
    private final List<HelpLine> helpLines = new ArrayList<>();
    private final GlyphLayout layout = new GlyphLayout();

    // Ссылка на менеджер состояний для переходов
    private ImprovedGameStateManager stateManager;

    /**
     * Одна готовая к отрисовке строка справки.
     * header=true: text=заголовок раздела, r/g/b=цвет раздела.
     * header=false: name=окрашенное название параметра (или null),
     * text=серая часть. baselineOffset — отступ базовой линии этой строки
     * от верха документа (положительный, вниз).
     */
    private static class HelpLine {
        final boolean header;
        final float r, g, b;
        final String name;
        final String text;
        final float nameWidth;
        final float height;
        final float gap;
        float baselineOffset;

        HelpLine(boolean header, float r, float g, float b, String name, String text,
                 float nameWidth, float height, float gap) {
            this.header = header;
            this.r = r;
            this.g = g;
            this.b = b;
            this.name = name;
            this.text = text;
            this.nameWidth = nameWidth;
            this.height = height;
            this.gap = gap;
        }
    }

    /**
     * Абзац внутри раздела: name — название параметра (может быть null
     * для простого абзаца), body — описание.
     */
    private static class HelpEntry {
        final String name;
        final String body;

        HelpEntry(String name, String body) {
            this.name = name;
            this.body = body;
        }
    }

    private static class HelpSection {
        final String title;
        final float r, g, b;
        final HelpEntry[] entries;

        HelpSection(String title, float r, float g, float b, HelpEntry[] entries) {
            this.title = title;
            this.r = r;
            this.g = g;
            this.b = b;
            this.entries = entries;
        }
    }

    // Статическое содержимое справки — подготавливается один раз.
    // Описания объясняют назначение показателей, без имён классов и методов.
    private static final HelpSection[] HELP_SECTIONS = {
        new HelpSection("Управление", 0.4f, 0.85f, 1f, new HelpEntry[]{
            new HelpEntry("← →", "поворачивают корабль влево и вправо."),
            new HelpEntry("↑", "ускоряет корабль вперёд; при отпускании корабль плавно замедляется."),
            new HelpEntry("SPACE", "стрельба в направлении носа корабля (удерживайте для серии выстрелов)."),
            new HelpEntry("+ / =", "приблизить камеру, «−» — отдалить (0.5×–2.0×), Enter — сбросить масштаб."),
            new HelpEntry("Esc / P", "пауза."),
            new HelpEntry("F", "показать или скрыть отладочную таблицу."),
            new HelpEntry("` / 1", "увеличить / уменьшить лимит рендера (Render cap): 30 → 60 → 120 → 240 → без ограничений. На скорость игровой логики не влияет."),
            new HelpEntry("F11", "переключить полноэкранный режим."),
        }),
        new HelpSection("Основной HUD", 0.6f, 0.85f, 1f, new HelpEntry[]{
            new HelpEntry("Score", "текущие очки за уничтоженные цели."),
            new HelpEntry("High", "лучший результат текущего запуска игры."),
            new HelpEntry("Level", "текущий уровень прокачки; игра начинается с 1 уровня."),
            new HelpEntry("XP", "накопленный опыт и требуемый для следующего уровня. Стартовый порог — 100, каждый следующий на 15% больше."),
            new HelpEntry("Полоса опыта", "анимированная доля опыта до следующего уровня; красная подсветка означает готовность к повышению."),
        }),
        new HelpSection("Производительность", 1f, 0.85f, 0.45f, new HelpEntry[]{
            new HelpEntry("Render FPS", "фактическое количество отрисованных кадров в секунду. Влияет на визуальную плавность, но не должно менять скорость игровой логики."),
            new HelpEntry("Render cap", "максимальная частота отрисовки. Значение «без ограничений» разрешает игре рендерить с максимально доступной скоростью. При включённом VSync фактический FPS также зависит от частоты монитора."),
            new HelpEntry("Simulation UPS", "фактическое количество логических обновлений игры в секунду. Во время каждого тика обновляются движение, столкновения, враги, снаряды и игровая механика."),
            new HelpEntry("Simulation target", "целевая частота фиксированной симуляции. Обычно составляет 60 тиков в секунду. Если фактический UPS ниже цели, процессор не успевает выполнять игровую логику."),
            new HelpEntry("Fixed dt", "фиксированная длительность одного логического тика. При 60 Гц она равна примерно 16,67 мс."),
            new HelpEntry("Ticks/frame", "количество логических тиков, выполненных во время последнего кадра рендера. Оно может быть 0, 1 или больше, потому что рендер и симуляция работают с разной частотой."),
            new HelpEntry("Simulation tick", "суммарное время выполнения одного логического тика в микросекундах: камера, фон, игрок, снаряды, астероиды, частицы и коллизии."),
            new HelpEntry("Collisions", "время обработки столкновений за последний тик; Camera — время обновления камеры за последний тик."),
            new HelpEntry(null, "Рост значений таймеров означает возросшую нагрузку соответствующей подсистемы."),
        }),
        new HelpSection("Игрок", 0.45f, 1f, 0.6f, new HelpEntry[]{
            new HelpEntry("Pos", "мировые координаты корабля (x, y)."),
            new HelpEntry("Vel / speed", "компоненты скорости и её итоговый модуль в единицах в секунду."),
            new HelpEntry("Facing", "направление носа корабля в градусах (0–360) и сторона света (E, NE, N, NW, W, SW, S, SE)."),
            new HelpEntry("Zoom", "текущий масштаб камеры (0.5×–2.0×); в скобках — мировые координаты камеры."),
        }),
        new HelpSection("Мир и объекты", 1f, 0.55f, 0.5f, new HelpEntry[]{
            new HelpEntry("Active objects", "объекты, находящиеся сейчас в активной коллекции мира."),
            new HelpEntry("Created total", "объекты, созданные за текущую сессию; счётчик не убывает."),
            new HelpEntry("Projectiles", "активные снаряды: пули и автоматические ракеты."),
            new HelpEntry("Obstacles", "активные препятствия: астероиды и орк-астероиды."),
            new HelpEntry("Enemies", "живые вражеские корабли."),
            new HelpEntry("Sectors", "загруженные секторы бесконечного мира вокруг корабля."),
            new HelpEntry("Grid cells", "занятые ячейки пространственной сетки — оптимизация коллизий."),
        }),
        new HelpSection("Отсечение (Culling)", 1f, 0.85f, 0.5f, new HelpEntry[]{
            new HelpEntry("Active", "все живые объекты или частицы в игровых коллекциях мира."),
            new HelpEntry("Visible", "объекты и частицы, пересекающие расширенную область камеры — с запасом на края экрана и крупные объекты."),
            new HelpEntry("Drawn", "объекты и частицы, фактически отправленные в рендер в текущем кадре."),
            new HelpEntry("Culled", "активные объекты и частицы вне области камеры, пропущенные при отрисовке. Они по-прежнему живут, двигаются и обновляют свои таймеры."),
            new HelpEntry("Макс. частиц", "видимый бюджет частиц внутри камеры. Внеэкранные декоративные частицы (пламя, следы, искры) его не тратят и не мешают появлению эффектов на экране. Внутренний защитный предел не даёт частицам накапливаться без ограничений; при его достижении первыми переиспользуются внеэкранные частицы."),
        }),
        new HelpSection("Прогресс", 1f, 0.65f, 0.3f, new HelpEntry[]{
            new HelpEntry("Дублирует HUD", "Level, XP, Score и High показывают те же данные, что и в левом верхнем углу экрана."),
            new HelpEntry("Level", "текущий уровень прокачки."),
            new HelpEntry("XP", "накопленный и требуемый опыт."),
            new HelpEntry("Score / High", "очки и лучший результат текущего запуска."),
        }),
        new HelpSection("Система", 0.75f, 0.6f, 1f, new HelpEntry[]{
            new HelpEntry("Mem", "используемая и доступная память JVM в мегабайтах."),
            new HelpEntry("HUD", "логическое разрешение окна для интерфейса; может отличаться от физического, например на Retina-экранах."),
            new HelpEntry("GL", "данные графического API: тип (OpenGL/GLES), версия, производитель и используемый рендерер."),
        }),
    };

    @Override
    public void onEnter() {
        GameLogger.info("=== ВХОД В СОСТОЯНИЕ ПАУЗЫ ===");

        if (!isInitialized) {
            initializePauseScreen();
            isInitialized = true;
        }

        // При каждом входе в паузу — начало меню и справки сверху
        selectedItem = 0;
        showHelp = false;
        helpScroll = 0f;
        rebuildHelpLayout();

        GameLogger.info("Экран паузы активирован");
    }

    @Override
    public void onUpdate(float deltaTime) {
        if (!isInitialized) {
            return;
        }

        // Обновляем анимацию
        animationTimer += deltaTime * 2f;

        // Плавная прокрутка справки при зажатых клавишах
        if (showHelp) {
            updateHelpScroll(deltaTime);
        }
    }

    @Override
    public void onRender() {
        if (!isInitialized) {
            return;
        }

        // При изменении размера окна пересчитываем раскладку справки и проекции
        if (Gdx.graphics.getWidth() != lastWidth || Gdx.graphics.getHeight() != lastHeight) {
            rebuildHelpLayout();
        }

        if (showHelp) {
            renderHelp();
        } else {
            renderMenu();
        }
    }

    @Override
    public void onHandleInput() {
        if (!isInitialized) {
            return;
        }

        if (showHelp) {
            handleHelpInput();
        } else {
            handleMenuInput();
        }
    }

    @Override
    public void onExit() {
        GameLogger.info("=== ВЫХОД ИЗ СОСТОЯНИЯ ПАУЗЫ ===");

        savePauseSettings();

        GameLogger.info("Экран паузы деактивирован");
    }

    // === МЕНЮ ПАУЗЫ ===

    private void renderMenu() {
        float winW = Gdx.graphics.getWidth();
        float winH = Gdx.graphics.getHeight();

        // Полупрозрачный тёмный оверлей поверх игры (существующий фон паузы)
        drawBackdrop(0.3f);

        BitmapFont titleFont = Fonts.getTitleFont();
        BitmapFont itemFont = Fonts.getHeadingFont();
        BitmapFont hintFont = Fonts.getBodyFont();

        spriteBatch.begin();

        // Заголовок
        titleFont.setColor(Color.WHITE);
        float titleWidth = measure(titleFont, "ПАУЗА");
        titleFont.draw(spriteBatch, "ПАУЗА", (winW - titleWidth) / 2f, winH * 0.72f);

        // Пункты меню
        for (int i = 0; i < MENU_ITEMS.length; i++) {
            float y = getItemY(i);
            boolean selected = (i == selectedItem);

            // Пульсация яркости выбранного пункта
            float pulse = selected ? 0.85f + 0.15f * (float) Math.sin(animationTimer * 5f) : 1f;
            if (selected) {
                itemFont.setColor(1f, 1f, 0.35f, pulse);
            } else {
                itemFont.setColor(0.8f, 0.8f, 1f, 1f);
            }

            String item = MENU_ITEMS[i];
            float itemWidth = measure(itemFont, item);
            itemFont.draw(spriteBatch, item, (winW - itemWidth) / 2f, y);
        }

        // Нижняя подсказка
        hintFont.setColor(0.7f, 0.7f, 0.7f, 1f);
        String hint = "↑/↓ — выбрать · Enter/клик — подтвердить · Esc — продолжить игру";
        float hintWidth = measure(hintFont, hint);
        hintFont.draw(spriteBatch, hint, (winW - hintWidth) / 2f, winH * 0.14f);

        spriteBatch.end();
    }

    private void handleMenuInput() {
        // Перемещение по пунктам (стрелки или W/S)
        if (GameKeys.isPressed(GameKeys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            selectedItem = (selectedItem - 1 + MENU_ITEMS.length) % MENU_ITEMS.length;
        }
        if (GameKeys.isPressed(GameKeys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            selectedItem = (selectedItem + 1) % MENU_ITEMS.length;
        }

        // Выбор пункта клавишей Enter
        if (GameKeys.isPressed(GameKeys.ENTER)) {
            selectMenuItem();
        }

        // Выбор пункта кликом мыши
        if (Gdx.input.justTouched()) {
            float mx = Gdx.input.getX();
            float my = Gdx.graphics.getHeight() - Gdx.input.getY();
            handleMenuClick(mx, my);
        }

        // Esc возвращает в игру (текущая логика паузы)
        if (GameKeys.isPressed(GameKeys.ESCAPE)) {
            GameLogger.info("ESC pressed in pause - return to game");
            if (stateManager != null) {
                stateManager.setState(ImprovedGameStateManager.GameState.PLAYING);
            }
        }
    }

    private void handleMenuClick(float mx, float my) {
        float half = Fonts.getHeadingFont().getLineHeight() * 0.6f;
        for (int i = 0; i < MENU_ITEMS.length; i++) {
            float y = getItemY(i);
            if (my > y - half && my < y + half) {
                selectedItem = i;
                selectMenuItem();
                return;
            }
        }
    }

    private void selectMenuItem() {
        switch (selectedItem) {
            case ITEM_RESUME:
                GameLogger.info("Resume game");
                if (stateManager != null) {
                    stateManager.setState(ImprovedGameStateManager.GameState.PLAYING);
                }
                break;
            case ITEM_SETTINGS:
                GameLogger.info("Open settings");
                if (stateManager != null) {
                    stateManager.setState(ImprovedGameStateManager.GameState.SETTINGS);
                }
                break;
            case ITEM_HELP:
                GameLogger.info("Open help");
                openHelp();
                break;
            case ITEM_EXIT:
                GameLogger.info("Return to main menu");
                if (stateManager != null) {
                    stateManager.setState(ImprovedGameStateManager.GameState.MENU);
                }
                break;
        }
    }

    private float getItemY(int i) {
        return Gdx.graphics.getHeight() * 0.62f - i * MENU_SPACING;
    }

    // === СПРАВКА ===

    private void openHelp() {
        showHelp = true;
        helpScroll = 0f;
        rebuildHelpLayout();
    }

    private void renderHelp() {
        float winW = Gdx.graphics.getWidth();
        float winH = Gdx.graphics.getHeight();

        // Непрозрачный полноэкранный фон: полностью скрывает игровой мир,
        // HUD и Debug-меню, поэтому текст читается без помех.
        drawBackdrop(1f, 0.03f, 0.03f, 0.05f);

        // Индикатор прокрутки справа (только если контент не помещается)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (maxScroll > 0f) {
            float trackX = contentX + contentWidth + SCROLLBAR_MARGIN;
            float trackH = visibleContentHeight;
            shapeRenderer.setColor(0.2f, 0.2f, 0.28f, 0.6f);
            shapeRenderer.rect(trackX, contentBottom, SCROLLBAR_WIDTH, trackH);
            float thumbH = Math.max(MIN_THUMB_HEIGHT, trackH * visibleContentHeight / helpContentHeight);
            float normalized = helpScroll / maxScroll;
            float thumbY = contentTop - thumbH - normalized * (trackH - thumbH);
            shapeRenderer.setColor(0.55f, 0.65f, 1f, 0.85f);
            shapeRenderer.rect(trackX, thumbY, SCROLLBAR_WIDTH, thumbH);
        }
        shapeRenderer.end();

        BitmapFont titleFont = Fonts.getTitleFont();
        BitmapFont headingFont = Fonts.getHeadingFont();
        BitmapFont bodyFont = Fonts.getBodyFont();

        spriteBatch.begin();

        // Заголовок (неподвижный, вне scissor)
        titleFont.setColor(Color.WHITE);
        float titleWidth = measure(titleFont, "Справка");
        titleFont.draw(spriteBatch, "Справка", (winW - titleWidth) / 2f, titleBaselineY);
        spriteBatch.flush(); // фиксируем заголовок до включения scissor

        // Прокручиваемое содержимое обрезается областью между заголовком и подсказкой.
        // ScissorStack сам преобразует логические координаты в физические пиксели (HiDPI).
        clipBounds.set(contentX, contentBottom, contentWidth, visibleContentHeight);
        ScissorStack.calculateScissors(uiCamera, spriteBatch.getTransformMatrix(), clipBounds, scissor);
        ScissorStack.pushScissors(scissor);

        for (HelpLine line : helpLines) {
            // Базовая линия строки в логических координатах (Y вверх):
            // у верхней границы документа при scroll=0, растёт с прокруткой.
            float drawY = contentTop + helpScroll - line.baselineOffset;

            // Пропускаем строки, полностью вне видимой области
            if (drawY < contentBottom - bodyLineHeight || drawY > contentTop + bodyLineHeight) {
                continue;
            }

            if (line.header) {
                headingFont.setColor(line.r, line.g, line.b, 1f);
                headingFont.draw(spriteBatch, line.text, contentX, drawY);
            } else {
                if (line.name != null) {
                    bodyFont.setColor(0.45f, 0.9f, 1f, 1f);
                    bodyFont.draw(spriteBatch, line.name, contentX, drawY);
                    bodyFont.setColor(0.82f, 0.82f, 0.82f, 1f);
                    if (line.text.length() > 0) {
                        bodyFont.draw(spriteBatch, line.text, contentX + line.nameWidth, drawY);
                    }
                } else {
                    bodyFont.setColor(0.82f, 0.82f, 0.82f, 1f);
                    bodyFont.draw(spriteBatch, line.text, contentX, drawY);
                }
            }
        }

        spriteBatch.flush(); // фиксируем содержимое с активным scissor
        ScissorStack.popScissors();

        // Нижняя подсказка (после снятия scissor, не перекрывает содержимое)
        bodyFont.setColor(0.65f, 0.65f, 0.68f, 1f);
        String hint = "↑/↓ или W/S — прокрутка · колесо мыши — прокрутка · Esc — назад";
        float hintWidth = measure(bodyFont, hint);
        bodyFont.draw(spriteBatch, hint, (winW - hintWidth) / 2f, FOOTER_BASELINE);

        spriteBatch.end();
    }

    private void handleHelpInput() {
        // Первый Esc внутри справки возвращает в меню паузы, а не в игру
        if (GameKeys.isPressed(GameKeys.ESCAPE)) {
            GameLogger.info("ESC in help - return to pause menu");
            showHelp = false;
            return;
        }

        // Прокрутка колёсиком мыши (положительное значение = вверх):
        // вниз (amountY<0) → scrollOffset увеличивается, вверх → уменьшается.
        float wheel = GameInputProcessor.scrollDeltaY;
        GameInputProcessor.scrollDeltaY = 0f;
        if (wheel != 0f) {
            setHelpScroll(helpScroll - wheel * WHEEL_STEP);
        }
    }

    private void updateHelpScroll(float deltaTime) {
        float delta = 0f;
        if (GameKeys.isDown(GameKeys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) {
            delta -= SCROLL_SPEED; // вверх — scrollOffset уменьшается
        }
        if (GameKeys.isDown(GameKeys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) {
            delta += SCROLL_SPEED; // вниз — scrollOffset увеличивается
        }
        if (delta != 0f) {
            setHelpScroll(helpScroll + delta * deltaTime);
        }
    }

    private void setHelpScroll(float value) {
        helpScroll = MathUtils.clamp(value, 0f, maxScroll);
    }

    /**
     * Пересчитать раскладку справки: перенос строк по ширине колонки через
     * GlyphLayout, высоту содержимого, смещения строк и предел прокрутки.
     * Вызывается только при открытии справки или изменении размера окна.
     */
    private void rebuildHelpLayout() {
        ensureProjection();
        helpLines.clear();

        BitmapFont body = Fonts.getBodyFont();
        BitmapFont heading = Fonts.getHeadingFont();
        BitmapFont title = Fonts.getTitleFont();

        float winW = Gdx.graphics.getWidth();
        float winH = Gdx.graphics.getHeight();

        contentWidth = Math.max(0f, Math.min(MAX_CONTENT_WIDTH, winW - 2f * MIN_SIDE_MARGIN));
        contentX = (winW - contentWidth) / 2f;

        bodyLineHeight = body.getLineHeight() * BODY_LINE_SPACING;
        headingLineHeight = heading.getLineHeight() * BODY_LINE_SPACING;

        boolean firstSection = true;
        for (HelpSection section : HELP_SECTIONS) {
            float sectionGap = firstSection ? 0f : SECTION_GAP;
            firstSection = false;
            helpLines.add(new HelpLine(true, section.r, section.g, section.b, null,
                    section.title, 0f, headingLineHeight, sectionGap));

            boolean firstEntry = true;
            for (HelpEntry entry : section.entries) {
                float entryGap = firstEntry ? PARAGRAPH_GAP + HEADER_BOTTOM_GAP : PARAGRAPH_GAP;
                firstEntry = false;

                if (entry.name == null) {
                    List<String> lines = wrapParagraph(entry.body, contentWidth, body);
                    for (int i = 0; i < lines.size(); i++) {
                        float gap = (i == 0) ? entryGap : 0f;
                        helpLines.add(new HelpLine(false, 0f, 0f, 0f, null,
                                lines.get(i), 0f, bodyLineHeight, gap));
                    }
                } else {
                    String combined = entry.name + " — " + entry.body;
                    List<String> lines = wrapParagraph(combined, contentWidth, body);
                    for (int i = 0; i < lines.size(); i++) {
                        if (i == 0) {
                            // Первая строка: название цветом + остаток светло-серым.
                            // Остаток — это ровно суффикс первой строки после названия,
                            // поэтому суммарная ширина не превышает contentWidth.
                            float nameWidth = measure(body, entry.name);
                            String rest = lines.get(0).length() > entry.name.length()
                                    ? lines.get(0).substring(entry.name.length()) : "";
                            helpLines.add(new HelpLine(false, 0f, 0f, 0f, entry.name,
                                    rest, nameWidth, bodyLineHeight, entryGap));
                        } else {
                            helpLines.add(new HelpLine(false, 0f, 0f, 0f, null,
                                    lines.get(i), 0f, bodyLineHeight, 0f));
                        }
                    }
                }
            }
        }

        // Смещение базовой линии каждой строки от верха документа.
        // Первая строка — заголовок первого раздела, поэтому якорь — ascent заголовка.
        float cursor = heading.getAscent();
        for (HelpLine line : helpLines) {
            cursor += line.gap;
            line.baselineOffset = cursor;
            cursor += line.height;
        }
        helpContentHeight = cursor;

        // Область прокрутки — между заголовком и нижней подсказкой (Y вверх).
        titleBaselineY = winH - TITLE_BASELINE_OFFSET;
        contentTop = titleBaselineY - title.getLineHeight() - TITLE_TOP_GAP;
        contentBottom = CONTENT_BOTTOM;
        visibleContentHeight = Math.max(0f, contentTop - contentBottom);

        // Предел прокрутки: scrollOffset==0 — начало документа, maxScroll — конец.
        maxScroll = Math.max(0f, helpContentHeight - visibleContentHeight);
        setHelpScroll(helpScroll);

        lastWidth = winW;
        lastHeight = winH;
    }

    /**
     * Перенос абзаца по словам в строки не шире maxWidth (логические px).
     * Вызывается только при построении раскладки.
     */
    private List<String> wrapParagraph(String text, float maxWidth, BitmapFont font) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            if (current.length() == 0) {
                current.append(word);
                continue;
            }
            String candidate = current.toString() + " " + word;
            layout.setText(font, candidate);
            if (layout.width > maxWidth) {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            } else {
                current.append(' ').append(word);
            }
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    // === ОБЩИЕ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ===

    /**
     * Ширина строки в указанном шрифте (без создания объектов).
     */
    private float measure(BitmapFont font, String text) {
        layout.setText(font, text);
        return layout.width;
    }

    /**
     * Нарисовать полноэкранный затемняющий фон указанной непрозрачности и цвета.
     */
    private void drawBackdrop(float alpha, float r, float g, float b) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(r, g, b, alpha);
        shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawBackdrop(float alpha) {
        drawBackdrop(alpha, 0f, 0f, 0f);
    }

    /**
     * Проекции спрайт-батча и shape-рендерера ставятся из единой камеры интерфейса
     * (логическое разрешение окна). При изменении размера окна пересчитываются.
     */
    private void ensureProjection() {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        uiCamera.setToOrtho(false, w, h);
        uiCamera.update();
        spriteBatch.setProjectionMatrix(uiCamera.combined);
        shapeRenderer.setProjectionMatrix(uiCamera.combined);
    }

    // === ИНИЦИАЛИЗАЦИЯ ===

    /**
     * Инициализация экрана паузы
     */
    private void initializePauseScreen() {
        GameLogger.info("Инициализация экрана паузы...");

        spriteBatch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        ensureProjection();

        // Шрифты с кириллицей (генерируются лениво, кэшируются)
        Fonts.getBodyFont();
        Fonts.getHeadingFont();
        Fonts.getTitleFont();
        Fonts.getUiFont();

        GameLogger.info("Экран паузы инициализирован");
    }

    /**
     * Сохранение настроек паузы
     */
    private void savePauseSettings() {
        GameLogger.info("Сохранение настроек паузы...");
        GameLogger.info("Настройки паузы сохранены");
    }

    /**
     * Установить ссылку на менеджер состояний
     */
    public void setStateManager(ImprovedGameStateManager stateManager) {
        this.stateManager = stateManager;
    }
}
