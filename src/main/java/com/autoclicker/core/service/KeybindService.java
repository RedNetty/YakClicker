package com.autoclicker.core.service;

/**
 * Interface for services that manage global keyboard hotkeys.
 */
public interface KeybindService {

    /**
     * Registers the native hook and initializes keyboard listeners.
     */
    void initialize();

    /**
     * Updates all hotkey settings.
     *
     * @param toggleHotkey The toggle hotkey
     * @param visibilityHotkey The visibility hotkey
     * @param pauseHotkey The pause/resume hotkey
     * @param increaseSpeedHotkey The increase speed hotkey
     * @param decreaseSpeedHotkey The decrease speed hotkey
     */
    void updateAllHotkeys(String toggleHotkey, String visibilityHotkey,
                          String pauseHotkey, String increaseSpeedHotkey,
                          String decreaseSpeedHotkey);

    /**
     * Re-registers hotkeys, useful after a UI change or settings update.
     */
    void refreshHotkeys();

    /**
     * Cleans up resources when the application is shutting down.
     */
    void cleanup();

    /**
     * Gets a list of all supported key names for display in UI.
     *
     * @return Array of key names
     */
    String[] getAvailableKeyNames();
}