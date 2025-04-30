package com.autoclicker.config;

/**
 * Application-level configuration and constants.
 */
public class AppConfig {
    // App-wide constants
    public static final String APP_NAME = "YakClicker";
    public static final String VERSION = "1.0.0";
    public static final String SETTINGS_DIR_NAME = ".yakclicker";
    public static final String PATTERNS_FOLDER_NAME = "patterns";
    public static final String SETTINGS_FILE_NAME = "settings.properties";
    public static final String TRAY_ICON_RESOURCE_PATH = "/icon.png";

    // System Tray Menu Item Labels
    public static final String TRAY_MENU_SHOW = "Show " + APP_NAME;
    public static final String TRAY_MENU_START = "Start Clicking";
    public static final String TRAY_MENU_STOP = "Stop Clicking";
    public static final String TRAY_MENU_EXIT = "Exit " + APP_NAME;

    // Default settings constants
    public static final double DEFAULT_CPS = 10.0;
    public static final String DEFAULT_CLICK_MODE = "SINGLE";
    public static final String DEFAULT_MOUSE_BUTTON = "LEFT";
    public static final boolean DEFAULT_RANDOMIZE_INTERVAL = false;
    public static final double DEFAULT_RANDOMIZATION_FACTOR = 0.2;
    public static final boolean DEFAULT_RANDOM_MOVEMENT = false;
    public static final int DEFAULT_MOVEMENT_RADIUS = 2;
    public static final String DEFAULT_TOGGLE_HOTKEY = "F6";
    public static final String DEFAULT_VISIBILITY_HOTKEY = "F8";
    public static final String DEFAULT_PAUSE_HOTKEY = "F7";
    public static final String DEFAULT_INCREASE_SPEED_HOTKEY = "=";
    public static final String DEFAULT_DECREASE_SPEED_HOTKEY = "-";
    public static final boolean DEFAULT_ALWAYS_ON_TOP = false;
    public static final double DEFAULT_TRANSPARENCY = 1.0;
    public static final boolean DEFAULT_MINIMIZE_TO_TRAY = true;
    public static final boolean DEFAULT_AUTO_HIDE = false;
    public static final boolean DEFAULT_DARK_MODE = true;
    public static final boolean DEFAULT_PLATFORM_SPECIFIC_SETTINGS = true;
    public static final String DEFAULT_PROFILE_NAME = "Default";

    // Feature flags
    public static final boolean ENABLE_CRASH_REPORTING = false;
    public static final boolean ENABLE_LOGGING = true;

    private AppConfig() {
        // Prevent instantiation
    }
}