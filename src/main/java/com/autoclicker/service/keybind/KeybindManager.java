package com.autoclicker.service.keybind;


import com.autoclicker.service.click.AutoClickerService;
import com.autoclicker.storage.SettingsManager;
import com.autoclicker.ui.frame.MainFrame;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/**
 * Manages global keyboard hotkeys for the application.
 */
public class KeybindManager implements NativeKeyListener {
    private final SettingsManager settingsManager;
    private final MainFrame mainFrame;
    private final AutoClickerService clickerService;

    // Keep track of keys that are currently pressed
    private final Set<Integer> pressedKeys = new HashSet<>();

    // Maps keycode to key name
    private static final Map<Integer, String> keycodeToName = new HashMap<>();

    // Maps key name to keycode
    private static final Map<String, Integer> nameToKeycode = new HashMap<>();

    static {
        // Initialize the keycode mappings
        // Function keys
        for (int i = 1; i <= 12; i++) {
            int keyCode = NativeKeyEvent.VC_F1 + i - 1;
            String keyName = "F" + i;
            keycodeToName.put(keyCode, keyName);
            nameToKeycode.put(keyName, keyCode);
        }

        // Number keys
        for (int i = 0; i <= 9; i++) {
            int keyCode = NativeKeyEvent.VC_1 + i;
            if (i == 0) keyCode = NativeKeyEvent.VC_0;
            String keyName = String.valueOf(i);
            keycodeToName.put(keyCode, keyName);
            nameToKeycode.put(keyName, keyCode);
        }

        // Letter keys
        for (int i = 0; i < 26; i++) {
            int keyCode = NativeKeyEvent.VC_A + i;
            char keyChar = (char) ('A' + i);
            String keyName = String.valueOf(keyChar);
            keycodeToName.put(keyCode, keyName);
            nameToKeycode.put(keyName, keyCode);
        }

        // Special keys
        keycodeToName.put(NativeKeyEvent.VC_ESCAPE, "Escape");
        nameToKeycode.put("Escape", NativeKeyEvent.VC_ESCAPE);

        keycodeToName.put(NativeKeyEvent.VC_SPACE, "Space");
        nameToKeycode.put("Space", NativeKeyEvent.VC_SPACE);

        keycodeToName.put(NativeKeyEvent.VC_TAB, "Tab");
        nameToKeycode.put("Tab", NativeKeyEvent.VC_TAB);

        keycodeToName.put(NativeKeyEvent.VC_CONTROL, "Ctrl");
        nameToKeycode.put("Ctrl", NativeKeyEvent.VC_CONTROL);

        keycodeToName.put(NativeKeyEvent.VC_ALT, "Alt");
        nameToKeycode.put("Alt", NativeKeyEvent.VC_ALT);

        keycodeToName.put(NativeKeyEvent.VC_SHIFT, "Shift");
        nameToKeycode.put("Shift", NativeKeyEvent.VC_SHIFT);

        keycodeToName.put(NativeKeyEvent.VC_INSERT, "Insert");
        nameToKeycode.put("Insert", NativeKeyEvent.VC_INSERT);

        keycodeToName.put(NativeKeyEvent.VC_DELETE, "Delete");
        nameToKeycode.put("Delete", NativeKeyEvent.VC_DELETE);

        keycodeToName.put(NativeKeyEvent.VC_HOME, "Home");
        nameToKeycode.put("Home", NativeKeyEvent.VC_HOME);

        keycodeToName.put(NativeKeyEvent.VC_END, "End");
        nameToKeycode.put("End", NativeKeyEvent.VC_END);

        keycodeToName.put(NativeKeyEvent.VC_PAGE_UP, "PgUp");
        nameToKeycode.put("PgUp", NativeKeyEvent.VC_PAGE_UP);

        keycodeToName.put(NativeKeyEvent.VC_PAGE_DOWN, "PgDown");
        nameToKeycode.put("PgDown", NativeKeyEvent.VC_PAGE_DOWN);
    }

    public KeybindManager(SettingsManager settingsManager, MainFrame mainFrame, AutoClickerService clickerService) {
        this.settingsManager = settingsManager;
        this.mainFrame = mainFrame;
        this.clickerService = clickerService;

        // Register this listener with GlobalScreen
        GlobalScreen.addNativeKeyListener(this);
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        String toggleHotkeyName = settingsManager.getToggleHotkey();
        String visibilityHotkeyName = settingsManager.getVisibilityHotkey();

        // Check if the pressed key matches one of our hotkeys
        String pressedKeyName = keycodeToName.getOrDefault(e.getKeyCode(), null);

        // Only process if this key wasn't already pressed (avoid repeat events)
        if (pressedKeyName != null && !pressedKeys.contains(e.getKeyCode())) {
            // Add to set of pressed keys
            pressedKeys.add(e.getKeyCode());

            if (pressedKeyName.equals(toggleHotkeyName)) {
                // Toggle auto-clicker
                clickerService.toggleClicking();
                mainFrame.updateStatusDisplay();
                System.out.println("Toggle hotkey pressed: " + pressedKeyName);
            } else if (pressedKeyName.equals(visibilityHotkeyName)) {
                // Toggle window visibility
                mainFrame.toggleVisibility();
                System.out.println("Visibility hotkey pressed: " + pressedKeyName);
            }
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        // Remove from set of pressed keys when released
        pressedKeys.remove(e.getKeyCode());
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {
        // Not needed for this implementation
    }

    /**
     * Converts a key name to its corresponding keycode.
     *
     * @param keyName The key name
     * @return The corresponding keycode, or -1 if not found
     */
    public static int getKeycodeFromName(String keyName) {
        return nameToKeycode.getOrDefault(keyName, -1);
    }

    /**
     * Gets a list of all supported key names for display in UI.
     *
     * @return Array of key names
     */
    public static String[] getAvailableKeyNames() {
        return nameToKeycode.keySet().toArray(new String[0]);
    }

    /**
     * Updates the hotkey settings.
     *
     * @param toggleHotkey The new toggle hotkey
     * @param visibilityHotkey The new visibility hotkey
     */
    public void updateHotkeys(String toggleHotkey, String visibilityHotkey) {
        settingsManager.setToggleHotkey(toggleHotkey);
        settingsManager.setVisibilityHotkey(visibilityHotkey);
        settingsManager.saveSettings();
    }
}