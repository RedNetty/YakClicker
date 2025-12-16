package com.autoclicker.config;

import java.io.Serializable;

/**
 * Contains settings values.
 * Separated from storage and management logic.
 */
public class Settings implements Serializable {
    private static final long serialVersionUID = 1L;

    // --- Settings Fields (Initialized with Defaults) ---
    private double cps = AppConfig.DEFAULT_CPS;
    private String clickMode = AppConfig.DEFAULT_CLICK_MODE;
    private String mouseButton = AppConfig.DEFAULT_MOUSE_BUTTON;
    private boolean randomizeInterval = AppConfig.DEFAULT_RANDOMIZE_INTERVAL;
    private double randomizationFactor = AppConfig.DEFAULT_RANDOMIZATION_FACTOR;
    private boolean randomMovement = AppConfig.DEFAULT_RANDOM_MOVEMENT;
    private int movementRadius = AppConfig.DEFAULT_MOVEMENT_RADIUS;
    private String toggleHotkey = AppConfig.DEFAULT_TOGGLE_HOTKEY;
    private String visibilityHotkey = AppConfig.DEFAULT_VISIBILITY_HOTKEY;
    private String pauseHotkey = AppConfig.DEFAULT_PAUSE_HOTKEY;
    private String increaseSpeedHotkey = AppConfig.DEFAULT_INCREASE_SPEED_HOTKEY;
    private String decreaseSpeedHotkey = AppConfig.DEFAULT_DECREASE_SPEED_HOTKEY;
    private boolean alwaysOnTop = AppConfig.DEFAULT_ALWAYS_ON_TOP;
    private double transparency = AppConfig.DEFAULT_TRANSPARENCY;
    private boolean minimizeToTray = AppConfig.DEFAULT_MINIMIZE_TO_TRAY;
    private boolean autoHide = AppConfig.DEFAULT_AUTO_HIDE;
    private boolean darkMode = AppConfig.DEFAULT_DARK_MODE;
    private boolean platformSpecificSettings = AppConfig.DEFAULT_PLATFORM_SPECIFIC_SETTINGS;
    private String currentProfileName = AppConfig.DEFAULT_PROFILE_NAME;

    // Getters and setters with validation
    public double getCPS() { return cps; }
    public void setCPS(double cps) {
        this.cps = Math.max(0.1, Math.min(500.0, cps)); // Validate range
    }

    public String getClickMode() { return clickMode; }
    public void setClickMode(String clickMode) {
        // Basic validation, could be expanded (e.g., check against a Set of valid modes)
        if (clickMode != null && !clickMode.trim().isEmpty()) {
            this.clickMode = clickMode;
        } else {
            this.clickMode = AppConfig.DEFAULT_CLICK_MODE;
        }
    }

    public String getMouseButton() { return mouseButton; }
    public void setMouseButton(String mouseButton) {
        // Basic validation
        if (mouseButton != null && !mouseButton.trim().isEmpty()) {
            this.mouseButton = mouseButton;
        } else {
            this.mouseButton = AppConfig.DEFAULT_MOUSE_BUTTON;
        }
    }

    public boolean isRandomizeInterval() { return randomizeInterval; }
    public void setRandomizeInterval(boolean randomizeInterval) { this.randomizeInterval = randomizeInterval; }

    public double getRandomizationFactor() { return randomizationFactor; }
    public void setRandomizationFactor(double randomizationFactor) {
        this.randomizationFactor = Math.max(0.0, Math.min(1.0, randomizationFactor)); // Validate range
    }

    public boolean isRandomMovement() { return randomMovement; }
    public void setRandomMovement(boolean randomMovement) { this.randomMovement = randomMovement; }

    public int getMovementRadius() { return movementRadius; }
    public void setMovementRadius(int movementRadius) {
        this.movementRadius = Math.max(0, Math.min(20, movementRadius)); // Validate range
    }

    public String getToggleHotkey() { return toggleHotkey; }
    public void setToggleHotkey(String toggleHotkey) {
        this.toggleHotkey = (toggleHotkey != null) ? toggleHotkey : AppConfig.DEFAULT_TOGGLE_HOTKEY;
    }

    public String getVisibilityHotkey() { return visibilityHotkey; }
    public void setVisibilityHotkey(String visibilityHotkey) {
        this.visibilityHotkey = (visibilityHotkey != null) ? visibilityHotkey : AppConfig.DEFAULT_VISIBILITY_HOTKEY;
    }

    public String getPauseHotkey() { return pauseHotkey; }
    public void setPauseHotkey(String pauseHotkey) {
        this.pauseHotkey = (pauseHotkey != null) ? pauseHotkey : AppConfig.DEFAULT_PAUSE_HOTKEY;
    }

    public String getIncreaseSpeedHotkey() { return increaseSpeedHotkey; }
    public void setIncreaseSpeedHotkey(String increaseSpeedHotkey) {
        this.increaseSpeedHotkey = (increaseSpeedHotkey != null) ? increaseSpeedHotkey : AppConfig.DEFAULT_INCREASE_SPEED_HOTKEY;
    }

    public String getDecreaseSpeedHotkey() { return decreaseSpeedHotkey; }
    public void setDecreaseSpeedHotkey(String decreaseSpeedHotkey) {
        this.decreaseSpeedHotkey = (decreaseSpeedHotkey != null) ? decreaseSpeedHotkey : AppConfig.DEFAULT_DECREASE_SPEED_HOTKEY;
    }

    public boolean isAlwaysOnTop() { return alwaysOnTop; }
    public void setAlwaysOnTop(boolean alwaysOnTop) { this.alwaysOnTop = alwaysOnTop; }

    public double getTransparency() { return transparency; }
    public void setTransparency(double transparency) {
        this.transparency = Math.max(0.2, Math.min(1.0, transparency)); // Validate range
    }

    public boolean isMinimizeToTray() { return minimizeToTray; }
    public void setMinimizeToTray(boolean minimizeToTray) { this.minimizeToTray = minimizeToTray; }

    public boolean isAutoHide() { return autoHide; }
    public void setAutoHide(boolean autoHide) { this.autoHide = autoHide; }

    public boolean isDarkMode() { return darkMode; }
    public void setDarkMode(boolean darkMode) { this.darkMode = darkMode; }

    public boolean isPlatformSpecificSettings() { return platformSpecificSettings; }
    public void setPlatformSpecificSettings(boolean platformSpecificSettings) {
        this.platformSpecificSettings = platformSpecificSettings;
    }

    public String getCurrentProfileName() { return currentProfileName; }
    public void setCurrentProfileName(String currentProfileName) {
        if (currentProfileName != null && !currentProfileName.trim().isEmpty()) {
            this.currentProfileName = currentProfileName;
        } else {
            this.currentProfileName = AppConfig.DEFAULT_PROFILE_NAME;
        }
    }

    /**
     * Resets all settings to their default values.
     */
    public void resetToDefaults() {
        this.cps = AppConfig.DEFAULT_CPS;
        this.clickMode = AppConfig.DEFAULT_CLICK_MODE;
        this.mouseButton = AppConfig.DEFAULT_MOUSE_BUTTON;
        this.randomizeInterval = AppConfig.DEFAULT_RANDOMIZE_INTERVAL;
        this.randomizationFactor = AppConfig.DEFAULT_RANDOMIZATION_FACTOR;
        this.randomMovement = AppConfig.DEFAULT_RANDOM_MOVEMENT;
        this.movementRadius = AppConfig.DEFAULT_MOVEMENT_RADIUS;
        this.toggleHotkey = AppConfig.DEFAULT_TOGGLE_HOTKEY;
        this.visibilityHotkey = AppConfig.DEFAULT_VISIBILITY_HOTKEY;
        this.pauseHotkey = AppConfig.DEFAULT_PAUSE_HOTKEY;
        this.increaseSpeedHotkey = AppConfig.DEFAULT_INCREASE_SPEED_HOTKEY;
        this.decreaseSpeedHotkey = AppConfig.DEFAULT_DECREASE_SPEED_HOTKEY;
        this.alwaysOnTop = AppConfig.DEFAULT_ALWAYS_ON_TOP;
        this.transparency = AppConfig.DEFAULT_TRANSPARENCY;
        this.minimizeToTray = AppConfig.DEFAULT_MINIMIZE_TO_TRAY;
        this.autoHide = AppConfig.DEFAULT_AUTO_HIDE;
        this.darkMode = AppConfig.DEFAULT_DARK_MODE;
        this.platformSpecificSettings = AppConfig.DEFAULT_PLATFORM_SPECIFIC_SETTINGS;
        this.currentProfileName = AppConfig.DEFAULT_PROFILE_NAME;
    }
}