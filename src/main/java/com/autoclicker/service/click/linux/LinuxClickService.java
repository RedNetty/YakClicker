package com.autoclicker.service.click.linux;

import com.autoclicker.service.click.AbstractClickService;
import com.autoclicker.storage.SettingsManager;

import java.awt.*;

/**
 * Linux-specific implementation of the click service.
 * Optimized for Linux platform behavior with improved performance.
 */
public class LinuxClickService extends AbstractClickService {

    public LinuxClickService(SettingsManager settingsManager) {
        super(settingsManager);
    }

    @Override
    protected Robot createRobot() throws AWTException {
        Robot robot = new Robot();
        // Linux-specific robot configuration
        robot.setAutoDelay(0);
        robot.setAutoWaitForIdle(false);
        return robot;
    }

    @Override
    protected void performClick() {
        try {
            // Get click configuration
            String clickMode = settingsManager.getClickMode();
            int buttonMask = getButtonMask(settingsManager.getMouseButton());

            // Get CPS for timing optimization
            double cps = settingsManager.getCPS();
            int pressDelay = cps > 50 ? 3 : (cps > 20 ? 5 : 10);     // Linux typically needs slightly longer delays
            int clickGap = cps > 50 ? 8 : (cps > 20 ? 15 : 25);      // than Windows for reliable operation

            // Linux has similar behavior to Windows but may need different timing
            switch (clickMode) {
                case "SINGLE":
                    robot.mousePress(buttonMask);
                    robot.delay(pressDelay);
                    robot.mouseRelease(buttonMask);
                    break;

                case "DOUBLE":
                    robot.mousePress(buttonMask);
                    robot.delay(pressDelay);
                    robot.mouseRelease(buttonMask);
                    robot.delay(clickGap);
                    robot.mousePress(buttonMask);
                    robot.delay(pressDelay);
                    robot.mouseRelease(buttonMask);
                    break;

                case "TRIPLE":
                    robot.mousePress(buttonMask);
                    robot.delay(pressDelay);
                    robot.mouseRelease(buttonMask);
                    robot.delay(clickGap);
                    robot.mousePress(buttonMask);
                    robot.delay(pressDelay);
                    robot.mouseRelease(buttonMask);
                    robot.delay(clickGap);
                    robot.mousePress(buttonMask);
                    robot.delay(pressDelay);
                    robot.mouseRelease(buttonMask);
                    break;
            }

            // Add small random movement if enabled
            if (settingsManager.isRandomMovement()) {
                addRandomMovement();
            }

            // Notify that a click was performed
            onClickPerformed();

        } catch (Exception e) {
            System.err.println("Error performing Linux click: " + e.getMessage());
        }
    }

    /**
     * Adds a very slight random movement to the mouse cursor.
     * This helps avoid detection in some applications.
     */
    private void addRandomMovement() {
        try {
            int radius = settingsManager.getMovementRadius();
            if (radius <= 0) {
                return;
            }

            // Get current mouse position
            Point currentPos = MouseInfo.getPointerInfo().getLocation();

            // Calculate a very small random offset within the specified radius
            int offsetX = random.nextInt(radius * 2) - radius;
            int offsetY = random.nextInt(radius * 2) - radius;

            // Move the mouse
            robot.mouseMove(currentPos.x + offsetX, currentPos.y + offsetY);

        } catch (Exception e) {
            // Ignore movement errors
            System.err.println("Error in Linux random movement: " + e.getMessage());
        }
    }

    /**
     * For X11-specific optimizations on Linux systems.
     * Can be extended for more complex operations if needed.
     */
    private boolean isX11Environment() {
        // Simple detection method, could be improved
        String display = System.getenv("DISPLAY");
        return display != null && !display.trim().isEmpty();
    }

    /**
     * For Wayland-specific optimizations on Linux systems.
     * Can be extended for more complex operations if needed.
     */
    private boolean isWaylandEnvironment() {
        String wayland = System.getenv("WAYLAND_DISPLAY");
        return wayland != null && !wayland.trim().isEmpty();
    }
}