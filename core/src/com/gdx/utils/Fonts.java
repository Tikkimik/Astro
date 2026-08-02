package com.gdx.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

/**
 * Общий шрифт интерфейса с поддержкой кириллицы.
 * Генерируется один раз из локального assets/fonts/DejaVuSans.ttf через gdx-freetype,
 * поэтому русский текст не превращается в квадраты.
 */
public final class Fonts {

    private static BitmapFont uiFont;      // 16px — базовый интерфейсный шрифт
    private static BitmapFont bodyFont;    // 18px — основной текст справки
    private static BitmapFont headingFont; // 24px — заголовки разделов справки
    private static BitmapFont titleFont;   // 40px — заголовки экранов (Справка, ПАУЗА)
    private static FreeTypeFontGenerator generator;

    private Fonts() {
    }

    /**
     * Получить общий шрифт интерфейса (ленивая генерация, кэшируется).
     * Размер базовой глифовой клетки 16px, как у основного HUD-шрифта.
     */
    public static BitmapFont getUiFont() {
        if (uiFont == null) {
            uiFont = generateFont(16);
        }
        return uiFont;
    }

    /**
     * Шрифт основного текста справки (18px).
     */
    public static BitmapFont getBodyFont() {
        if (bodyFont == null) {
            bodyFont = generateFont(18);
        }
        return bodyFont;
    }

    /**
     * Шрифт заголовков разделов справки (24px).
     */
    public static BitmapFont getHeadingFont() {
        if (headingFont == null) {
            headingFont = generateFont(24);
        }
        return headingFont;
    }

    /**
     * Шрифт заголовков экранов (40px).
     */
    public static BitmapFont getTitleFont() {
        if (titleFont == null) {
            titleFont = generateFont(40);
        }
        return titleFont;
    }

    /**
     * Создать шрифт заданного размера из локального DejaVuSans.ttf.
     * Каждый размер генерируется ровно один раз и кэшируется.
     */
    private static BitmapFont generateFont(int size) {
        if (generator == null) {
            generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/DejaVuSans.ttf"));
        }
        FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();
        param.size = size;
        param.characters = buildCharacters();
        // Линейная фильтрация — чёткий текст без размытия
        param.minFilter = Texture.TextureFilter.Linear;
        param.magFilter = Texture.TextureFilter.Linear;
        return generator.generateFont(param);
    }

    /**
     * Набор глифов: ASCII + кириллица (включая дополнение) + спецсимволы.
     * Диапазоны формируются программно, чтобы ничего не пропустить.
     */
    private static String buildCharacters() {
        StringBuilder sb = new StringBuilder();

        // Печатные ASCII-символы (код 32–126)
        for (int i = 32; i <= 126; i++) {
            sb.append((char) i);
        }

        // Кириллица U+0400–U+04FF (включая Ё = U+0401 и ё = U+0451)
        for (int i = 0x0400; i <= 0x04FF; i++) {
            sb.append((char) i);
        }

        // Кириллическое дополнение U+0500–U+052F
        for (int i = 0x0500; i <= 0x052F; i++) {
            sb.append((char) i);
        }

        // Спецсимволы: тире, градус, стрелки и пр.
        sb.append("—–…°`•»«±→↓↑×·“”←−");

        // Служебные символы: перевод строки и табуляция
        sb.append('\n').append('\t');

        return sb.toString();
    }

    /**
     * Освободить ресурсы шрифтов (идемпотентно). После вызова шрифты
     * будут пересозданы при следующем обращении.
     */
    public static void dispose() {
        if (uiFont != null) {
            uiFont.dispose();
            uiFont = null;
        }
        if (bodyFont != null) {
            bodyFont.dispose();
            bodyFont = null;
        }
        if (headingFont != null) {
            headingFont.dispose();
            headingFont = null;
        }
        if (titleFont != null) {
            titleFont.dispose();
            titleFont = null;
        }
        if (generator != null) {
            generator.dispose();
            generator = null;
        }
    }
}
