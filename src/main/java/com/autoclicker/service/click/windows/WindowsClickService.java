package com.autoclicker.service.click.windows;

import com.autoclicker.service.click.AbstractClickService;
import com.autoclicker.storage.SettingsManager;

import java.awt.*;

/**
 * Windows-specific implementation of the click service.
 * Optimized for Windows platform behavior with performance enhancements.
 */
public class WindowsClickService extends AbstractClickService {

    public WindowsClickService(SettingsManager settingsManager) {
        super(settingsManager);
    }

    @Override
    protected Robot createRobot() throws AWTException {
        Robot robot = new Robot();
        // Windows-specific robot configuration
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
            int pressDelay = cps > 50 ? 2 : (cps > 20 ? 5 : 10);  // Adaptive timing for different CPS rates
            int clickGap = cps > 50 ? 5 : (cps > 20 ? 10 : 20);   // Shorter gaps for higher CPS

            // Perform the click sequence based on mode
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
            System.err.println("Error performing Windows click: " + e.getMessage());
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
        }
    }
}