package com.gdx.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;

/**
 * Менеджер для управления настройками дисплея
 */
public class DisplayManager {
    
    // Разрешения
    public static final int RESOLUTION_HD = 0;      // 1280x720
    public static final int RESOLUTION_FHD = 1;     // 1920x1080
    public static final int RESOLUTION_2K = 2;      // 2560x1440
    public static final int RESOLUTION_4K = 3;      // 3840x2160
    
    // Режимы отображения
    public static final int DISPLAY_WINDOWED = 0;
    public static final int DISPLAY_BORDERLESS = 1;
    public static final int DISPLAY_FULLSCREEN = 2;
    
    private static int currentWidth = 1280;
    private static int currentHeight = 720;
    private static boolean isFullscreen = false;
    private static long lastDisplayChange = 0;
    private static final long DISPLAY_CHANGE_DELAY = 500; // 500ms задержка между изменениями
    
    /**
     * Применить настройки дисплея
     */
    public static void applyDisplaySettings() {
        int displayMode = GameSettings.getDisplayMode();
        
        // Используем текущие размеры окна
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        
        // Применяем только режим отображения
        applyDisplayMode(displayMode, width, height);
        
        System.out.println("Display settings applied: " + width + "x" + height + 
                          " Mode: " + getDisplayModeName(displayMode));
    }
    
    /**
     * Применить режим отображения
     */
    public static void applyDisplayMode(int mode, int width, int height) {
        if (Gdx.app == null) return;
        
        // Проверяем задержку между изменениями
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastDisplayChange < DISPLAY_CHANGE_DELAY) {
            return; // Слишком частое изменение
        }
        
        // Проверяем, не пытаемся ли мы установить те же настройки
        if (currentWidth == width && currentHeight == height && 
            ((mode == DISPLAY_FULLSCREEN && isFullscreen) || 
             (mode != DISPLAY_FULLSCREEN && !isFullscreen))) {
            return; // Ничего не меняем
        }
        
        lastDisplayChange = currentTime;
        
        try {
            switch (mode) {
                case DISPLAY_WINDOWED:
                    if (isFullscreen) {
                        // Сначала выходим из полноэкранного режима
                        Gdx.graphics.setWindowedMode(currentWidth, currentHeight);
                        isFullscreen = false;
                    }
                    // Затем устанавливаем нужный размер
                    if (currentWidth != width || currentHeight != height) {
                        Gdx.graphics.setWindowedMode(width, height);
                    }
                    break;
                    
                case DISPLAY_BORDERLESS:
                    if (isFullscreen) {
                        // Сначала выходим из полноэкранного режима
                        Gdx.graphics.setWindowedMode(currentWidth, currentHeight);
                        isFullscreen = false;
                    }
                    // Borderless windowed mode
                    if (currentWidth != width || currentHeight != height) {
                        Gdx.graphics.setWindowedMode(width, height);
                    }
                    break;
                    
                case DISPLAY_FULLSCREEN:
                    if (!isFullscreen) {
                        // Получаем текущий режим дисплея для полноэкранного режима
                        Graphics.DisplayMode displayMode = Gdx.graphics.getDisplayMode();
                        if (displayMode != null) {
                            Gdx.graphics.setFullscreenMode(displayMode);
                            isFullscreen = true;
                        } else {
                            // Fallback на оконный режим с максимальным размером
                            Gdx.graphics.setWindowedMode(width, height);
                        }
                    }
                    break;
            }
            
            // Обновляем текущие размеры
            currentWidth = width;
            currentHeight = height;
            
        } catch (Exception e) {
            System.err.println("Error applying display mode: " + e.getMessage());
            // Fallback на безопасный режим
            try {
                Gdx.graphics.setWindowedMode(1280, 720);
                currentWidth = 1280;
                currentHeight = 720;
                isFullscreen = false;
            } catch (Exception fallbackError) {
                System.err.println("Critical error: cannot set fallback display mode: " + fallbackError.getMessage());
            }
        }
    }
    
    /**
     * Применить V-Sync
     */
    public static void applyVSync(boolean enabled) {
        if (Gdx.app == null) return;
        
        try {
            Gdx.graphics.setVSync(enabled);
        } catch (Exception e) {
            System.err.println("Error applying V-Sync: " + e.getMessage());
        }
    }
    
    /**
     * Получить размеры для выбранного разрешения
     */
    public static int[] getResolutionDimensions(int resolution) {
        switch (resolution) {
            case RESOLUTION_HD:
                return new int[]{1280, 720};
            case RESOLUTION_FHD:
                return new int[]{1920, 1080};
            case RESOLUTION_2K:
                return new int[]{2560, 1440};
            case RESOLUTION_4K:
                return new int[]{3840, 2160};
            default:
                return new int[]{1920, 1080};
        }
    }
    
    /**
     * Получить название режима отображения
     */
    public static String getDisplayModeName(int mode) {
        switch (mode) {
            case DISPLAY_WINDOWED:
                return "Windowed";
            case DISPLAY_BORDERLESS:
                return "Borderless";
            case DISPLAY_FULLSCREEN:
                return "Fullscreen";
            default:
                return "Unknown";
        }
    }
    
    /**
     * Получить название разрешения
     */
    public static String getResolutionName(int resolution) {
        switch (resolution) {
            case RESOLUTION_HD:
                return "HD (1280x720)";
            case RESOLUTION_FHD:
                return "FHD (1920x1080)";
            case RESOLUTION_2K:
                return "2K (2560x1440)";
            case RESOLUTION_4K:
                return "4K (3840x2160)";
            default:
                return "FHD (1920x1080)";
        }
    }
    
    /**
     * Проверить, поддерживается ли разрешение
     */
    public static boolean isResolutionSupported(int resolution) {
        if (Gdx.app == null) return false;
        
        Graphics.DisplayMode[] modes = Gdx.graphics.getDisplayModes();
        int[] targetDimensions = getResolutionDimensions(resolution);
        
        for (Graphics.DisplayMode mode : modes) {
            if (mode.width == targetDimensions[0] && mode.height == targetDimensions[1]) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Получить текущую ширину окна
     */
    public static int getCurrentWidth() {
        return currentWidth;
    }
    
    /**
     * Получить текущую высоту окна
     */
    public static int getCurrentHeight() {
        return currentHeight;
    }
    
    /**
     * Проверить, находится ли игра в полноэкранном режиме
     */
    public static boolean isFullscreen() {
        return isFullscreen;
    }
    
    /**
     * Переключить полноэкранный режим
     */
    public static void toggleFullscreen() {
        if (Gdx.app == null) return;
        
        try {
            if (isFullscreen) {
                // Переключаемся в оконный режим
                Gdx.graphics.setWindowedMode(currentWidth, currentHeight);
                isFullscreen = false;
                System.out.println("Switched to windowed mode: " + currentWidth + "x" + currentHeight);
            } else {
                // Сохраняем текущие размеры окна
                currentWidth = Gdx.graphics.getWidth();
                currentHeight = Gdx.graphics.getHeight();
                
                // Переключаемся в полноэкранный режим
                Graphics.DisplayMode displayMode = Gdx.graphics.getDisplayMode();
                if (displayMode != null) {
                    Gdx.graphics.setFullscreenMode(displayMode);
                    isFullscreen = true;
                    System.out.println("Switched to fullscreen mode");
                } else {
                    System.err.println("Failed to get display mode for fullscreen");
                }
            }
        } catch (Exception e) {
            System.err.println("Error toggling fullscreen: " + e.getMessage());
            // Fallback на безопасный режим
            try {
                Gdx.graphics.setWindowedMode(1280, 720);
                currentWidth = 1280;
                currentHeight = 720;
                isFullscreen = false;
            } catch (Exception fallbackError) {
                System.err.println("Critical error: cannot set fallback display mode: " + fallbackError.getMessage());
            }
        }
    }
    
    /**
     * Получить список поддерживаемых разрешений
     */
    public static String[] getSupportedResolutions() {
        if (Gdx.app == null) return new String[]{"FHD (1920x1080)"};
        
        Graphics.DisplayMode[] modes = Gdx.graphics.getDisplayModes();
        java.util.Set<String> supported = new java.util.HashSet<>();
        
        // Добавляем стандартные разрешения
        supported.add("HD (1280x720)");
        supported.add("FHD (1920x1080)");
        supported.add("2K (2560x1440)");
        supported.add("4K (3840x2160)");
        
        // Фильтруем только поддерживаемые
        java.util.List<String> result = new java.util.ArrayList<>();
        for (String resolution : supported) {
            if (isResolutionSupportedByName(resolution, modes)) {
                result.add(resolution);
            }
        }
        
        return result.toArray(new String[0]);
    }
    
    private static boolean isResolutionSupportedByName(String resolutionName, Graphics.DisplayMode[] modes) {
        int[] dimensions = null;
        
        if (resolutionName.contains("HD (1280x720)")) {
            dimensions = new int[]{1280, 720};
        } else if (resolutionName.contains("FHD (1920x1080)")) {
            dimensions = new int[]{1920, 1080};
        } else if (resolutionName.contains("2K (2560x1440)")) {
            dimensions = new int[]{2560, 1440};
        } else if (resolutionName.contains("4K (3840x2160)")) {
            dimensions = new int[]{3840, 2160};
        }
        
        if (dimensions != null) {
            for (Graphics.DisplayMode mode : modes) {
                if (mode.width == dimensions[0] && mode.height == dimensions[1]) {
                    return true;
                }
            }
        }
        
        return false;
    }
}
