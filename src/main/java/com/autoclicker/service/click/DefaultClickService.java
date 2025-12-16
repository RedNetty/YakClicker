package com.autoclicker.service.click;

import com.autoclicker.storage.SettingsManager;

import java.awt.*;

/**
 * A standard implementation of the click service using Java's Robot class.
 * This serves as a fallback for platforms where native implementations aren't available.
 * Enhanced with performance optimizations for high CPS rates.
 */
public class DefaultClickService extends AbstractClickService {

    /**
     * Creates a new DefaultClickService.
     *
     * @param settingsManager The settings manager instance
     */
    public DefaultClickService(SettingsManager settingsManager) {
        super(settingsManager);
    }

    /**
     * Creates an optimized Robot instance for the current system.
     *
     * @return An optimized Robot instance
     * @throws AWTException If the Robot cannot be created
     */
    @Override
    protected Robot createRobot() throws AWTException {
        Robot robot = new Robot();

        // Disable auto-delay and auto-wait for higher performance
        robot.setAutoDelay(0);
        robot.setAutoWaitForIdle(false);

        return robot;
    }

    @Override
    protected void performClick() {
        try {
            // Get mouse button mask
            int buttonMask = getButtonMask(settingsManager.getMouseButton());

            // Get CPS for timing optimization
            double cps = settingsManager.getCPS();
            int pressDelay = cps > 50 ? 5 : 10;  // Shorter press time for high CPS
            int clickGap = cps > 50 ? 20 : 50;   // Shorter gap for high CPS

            // Perform clicks based on click mode
            switch (settingsManager.getClickMode()) {
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

            // Add randomized movement if enabled
            if (settingsManager.isRandomMovement()) {
                addRandomMovement();
            }

            // Notify that a click was performed
            onClickPerformed();

        } catch (Exception e) {
            System.err.println("Error performing click: " + e.getMessage());
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

            // Get current position
            Point currentPos = MouseInfo.getPointerInfo().getLocation();

            // Calculate random offset within specified radius
            int offsetX = random.nextInt(radius * 2) - radius;
            int offsetY = random.nextInt(radius * 2) - radius;

            // Move the mouse
            robot.mouseMove(currentPos.x + offsetX, currentPos.y + offsetY);

        } catch (Exception e) {
            // Ignore movement errors
            System.err.println("Error applying random movement: " + e.getMessage());
        }
    }
}