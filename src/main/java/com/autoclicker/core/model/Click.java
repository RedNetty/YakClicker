package com.autoclicker.core.model;

import java.io.Serializable;

/**
 * Represents a single mouse click with its properties.
 */
public class Click implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Enum for mouse button types.
     */
    public enum Button {
        LEFT, MIDDLE, RIGHT
    }

    /**
     * Enum for click types.
     */
    public enum Type {
        SINGLE, DOUBLE, TRIPLE
    }

    private final int x;
    private final int y;
    private final Button button;
    private final Type type;
    private final long timestamp;

    /**
     * Creates a new click with the specified properties.
     *
     * @param x The x-coordinate
     * @param y The y-coordinate
     * @param button The mouse button
     * @param type The click type
     */
    public Click(int x, int y, Button button, Type type) {
        this.x = x;
        this.y = y;
        this.button = button;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Creates a new click with the specified properties and timestamp.
     *
     * @param x The x-coordinate
     * @param y The y-coordinate
     * @param button The mouse button
     * @param type The click type
     * @param timestamp The timestamp
     */
    public Click(int x, int y, Button button, Type type, long timestamp) {
        this.x = x;
        this.y = y;
        this.button = button;
        this.type = type;
        this.timestamp = timestamp;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public Button getButton() { return button; }
    public Type getType() { return type; }
    public long getTimestamp() { return timestamp; }

    /**
     * Converts a string button name to a Button enum value.
     *
     * @param buttonName The button name (LEFT, MIDDLE, RIGHT)
     * @return The Button enum value
     */
    public static Button buttonFromString(String buttonName) {
        if (buttonName == null) {
            return Button.LEFT;
        }

        try {
            return Button.valueOf(buttonName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Button.LEFT;
        }
    }

    /**
     * Converts a string click type to a Type enum value.
     *
     * @param typeName The click type name (SINGLE, DOUBLE, TRIPLE)
     * @return The Type enum value
     */
    public static Type typeFromString(String typeName) {
        if (typeName == null) {
            return Type.SINGLE;
        }

        try {
            return Type.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Type.SINGLE;
        }
    }

    @Override
    public String toString() {
        return "Click{" +
                "x=" + x +
                ", y=" + y +
                ", button=" + button +
                ", type=" + type +
                ", timestamp=" + timestamp +
                '}';
    }
}