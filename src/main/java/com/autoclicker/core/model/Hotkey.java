package com.autoclicker.core.model;

import java.io.Serializable;

/**
 * Represents a keyboard hotkey configuration.
 */
public class Hotkey implements Serializable {
    private static final long serialVersionUID = 1L;

    private String keyName;
    private int keyCode;
    private boolean requiresCtrl;
    private boolean requiresShift;
    private boolean requiresAlt;

    /**
     * Creates a new hotkey with the specified key name.
     *
     * @param keyName The name of the key (e.g., "F1", "A")
     */
    public Hotkey(String keyName) {
        this.keyName = keyName;
        this.keyCode = -1;
        this.requiresCtrl = false;
        this.requiresShift = false;
        this.requiresAlt = false;
    }

    /**
     * Creates a new hotkey with the specified key name and modifiers.
     *
     * @param keyName The name of the key
     * @param requiresCtrl Whether Ctrl is required
     * @param requiresShift Whether Shift is required
     * @param requiresAlt Whether Alt is required
     */
    public Hotkey(String keyName, boolean requiresCtrl, boolean requiresShift, boolean requiresAlt) {
        this.keyName = keyName;
        this.keyCode = -1;
        this.requiresCtrl = requiresCtrl;
        this.requiresShift = requiresShift;
        this.requiresAlt = requiresAlt;
    }

    /**
     * Creates a new hotkey with the specified key code.
     *
     * @param keyCode The key code
     */
    public Hotkey(int keyCode) {
        this.keyName = null;
        this.keyCode = keyCode;
        this.requiresCtrl = false;
        this.requiresShift = false;
        this.requiresAlt = false;
    }

    public String getKeyName() { return keyName; }
    public void setKeyName(String keyName) { this.keyName = keyName; }

    public int getKeyCode() { return keyCode; }
    public void setKeyCode(int keyCode) { this.keyCode = keyCode; }

    public boolean requiresCtrl() { return requiresCtrl; }
    public void setRequiresCtrl(boolean requiresCtrl) { this.requiresCtrl = requiresCtrl; }

    public boolean requiresShift() { return requiresShift; }
    public void setRequiresShift(boolean requiresShift) { this.requiresShift = requiresShift; }

    public boolean requiresAlt() { return requiresAlt; }
    public void setRequiresAlt(boolean requiresAlt) { this.requiresAlt = requiresAlt; }

    /**
     * Gets a display name for the hotkey, including modifiers.
     *
     * @return The hotkey display name
     */
    public String getDisplayName() {
        StringBuilder sb = new StringBuilder();

        if (requiresCtrl) {
            sb.append("Ctrl+");
        }

        if (requiresShift) {
            sb.append("Shift+");
        }

        if (requiresAlt) {
            sb.append("Alt+");
        }

        sb.append(keyName != null ? keyName : "Unknown");

        return sb.toString();
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}