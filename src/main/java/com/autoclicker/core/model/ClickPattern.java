package com.autoclicker.core.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a pattern of mouse clicks to be recorded or played back.
 */
public class ClickPattern implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final List<ClickPoint> clickPoints;
    private boolean looping;

    /**
     * Creates a new click pattern with the given name.
     *
     * @param name The name of the pattern
     */
    public ClickPattern(String name) {
        this.name = name;
        this.clickPoints = new ArrayList<>();
        this.looping = false;
    }

    /**
     * Adds a click point to the pattern.
     *
     * @param point The click point to add
     */
    public void addClickPoint(ClickPoint point) {
        clickPoints.add(point);
    }

    /**
     * Gets the list of click points in the pattern.
     *
     * @return The click points
     */
    public List<ClickPoint> getClickPoints() {
        return clickPoints;
    }

    /**
     * Gets the number of clicks in the pattern.
     *
     * @return The number of clicks
     */
    public int getClickCount() {
        return clickPoints.size();
    }

    /**
     * Gets the name of the pattern.
     *
     * @return The pattern name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the total duration of the pattern in milliseconds.
     *
     * @return The total duration
     */
    public long getTotalDuration() {
        if (clickPoints.isEmpty()) {
            return 0;
        }

        return clickPoints.stream()
                .mapToLong(ClickPoint::getDelay)
                .sum();
    }

    /**
     * Sets whether the pattern should loop during playback.
     *
     * @param looping Whether to loop
     */
    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    /**
     * Checks if the pattern is set to loop during playback.
     *
     * @return true if looping, false otherwise
     */
    public boolean isLooping() {
        return looping;
    }

    /**
     * Represents a single click point in a pattern.
     */
    public static class ClickPoint implements Serializable {
        private static final long serialVersionUID = 1L;

        private final int x;
        private final int y;
        private final long delay;
        private final String mouseButton;
        private final String clickType;

        /**
         * Creates a new click point.
         *
         * @param x The x-coordinate
         * @param y The y-coordinate
         * @param delay The delay before the click
         * @param mouseButton The mouse button (LEFT, MIDDLE, RIGHT)
         * @param clickType The click type (SINGLE, DOUBLE, TRIPLE)
         */
        public ClickPoint(int x, int y, long delay, String mouseButton, String clickType) {
            this.x = x;
            this.y = y;
            this.delay = delay;
            this.mouseButton = mouseButton;
            this.clickType = clickType;
        }

        public int getX() { return x; }
        public int getY() { return y; }
        public long getDelay() { return delay; }
        public String getMouseButton() { return mouseButton; }
        public String getClickType() { return clickType; }

        @Override
        public String toString() {
            return "ClickPoint{" +
                    "x=" + x +
                    ", y=" + y +
                    ", delay=" + delay +
                    ", mouseButton='" + mouseButton + '\'' +
                    ", clickType='" + clickType + '\'' +
                    '}';
        }
    }
}