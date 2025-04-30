package com.autoclicker.service.click.macos;

import com.autoclicker.service.click.AbstractClickService;
import com.autoclicker.storage.SettingsManager;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Platform;

import java.awt.MouseInfo;
import java.awt.Point;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A macOS-specific implementation of the click service using native CoreGraphics
 * APIs via JNA to avoid issues with Java's Robot class.
 * This version correctly fetches the current mouse position before clicking.
 *
 * IMPORTANT: For this service to work, the Java application running it MUST be granted
 * Accessibility permissions in macOS System Settings:
 * System Settings > Privacy & Security > Accessibility
 */
public class MacOSClickService extends AbstractClickService {
    private static final Logger LOGGER = Logger.getLogger(MacOSClickService.class.getName());

    // --- JNA Interface for CoreGraphics & CoreFoundation ---
    public interface CoreGraphics extends Library {
        CoreGraphics INSTANCE = Native.load("CoreGraphics", CoreGraphics.class);
        CoreFoundation CF = CoreFoundation.INSTANCE; // Load CoreFoundation as well for CFRelease

        // --- Event Types ---
        int kCGEventNull = 0;
        int kCGEventLeftMouseDown = 1;
        int kCGEventLeftMouseUp = 2;
        int kCGEventRightMouseDown = 3;
        int kCGEventRightMouseUp = 4;
        int kCGEventMouseMoved = 5;
        int kCGEventLeftMouseDragged = 6;
        int kCGEventRightMouseDragged = 7;
        int kCGEventOtherMouseDown = 25;
        int kCGEventOtherMouseUp = 26;
        int kCGEventOtherMouseDragged = 27;

        // --- Mouse Buttons ---
        int kCGMouseButtonLeft = 0;
        int kCGMouseButtonRight = 1;
        int kCGMouseButtonCenter = 2;

        // --- Event Tap Locations ---
        int kCGHIDEventTap = 0;
        int kCGSessionEventTap = 1;
        int kCGAnnotatedSessionEventTap = 2;

        // --- CoreGraphics Functions ---
        Pointer CGEventCreateMouseEvent(Pointer source, int mouseType, CGPoint mouseCursorPosition, int mouseButton);
        Pointer CGEventCreate(Pointer source);
        void CGEventPost(int tap, Pointer event);
        CGPoint CGEventGetLocation(Pointer event);
        Pointer CGEventSourceCreate(int stateID);
        void CGEventSetIntegerValueField(Pointer event, int field, long value);

        // Constant for setting click state
        int kCGMouseEventClickState = 28;
    }

    public interface CoreFoundation extends Library {
        CoreFoundation INSTANCE = Native.load("CoreFoundation", CoreFoundation.class);
        void CFRelease(Pointer cf);
    }

    // --- CGPoint Structure ---
    @Structure.FieldOrder({"x", "y"})
    public static class CGPoint extends Structure implements Structure.ByValue {
        public double x;
        public double y;

        public CGPoint() {
            super();
        }

        public CGPoint(double x, double y) {
            super();
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "CGPoint{" + "x=" + x + ", y=" + y + '}';
        }
    }

    // Native API instances
    private final CoreGraphics cg;
    private final CoreFoundation cf;

    /**
     * Creates a new MacOSClickService.
     *
     * @param settingsManager The settings manager instance
     * @throws UnsupportedOperationException if not running on macOS.
     */
    public MacOSClickService(SettingsManager settingsManager) {
        super(settingsManager);

        if (!Platform.isMac()) {
            throw new UnsupportedOperationException("MacOSClickService is only available on macOS.");
        }

        try {
            this.cg = CoreGraphics.INSTANCE;
            this.cf = CoreFoundation.INSTANCE;
            LOGGER.log(Level.INFO, "MacOSClickService initialized. Ensure Accessibility permissions are granted.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize CoreGraphics bindings", e);
            throw new UnsupportedOperationException("Failed to initialize macOS native services", e);
        }
    }

    @Override
    protected void performClick() {
        // Get the current mouse position
        CGPoint currentPos = getCurrentMouseLocation();
        if (currentPos == null) {
            LOGGER.log(Level.WARNING, "Failed to get current mouse position. Skipping click.");
            return;
        }

        Pointer eventSource = null;
        try {
            // Get click configuration
            String clickMode = settingsManager.getClickMode();
            String buttonSetting = settingsManager.getMouseButton();
            int cgMouseButton = getCoreGraphicsMouseButton(buttonSetting);
            int mouseDownEventType = getMouseDownEventType(cgMouseButton);
            int mouseUpEventType = getMouseUpEventType(cgMouseButton);

            // Create an event source
            eventSource = cg.CGEventSourceCreate(0);

            // Get clicks based on click mode
            int clickCount = 1;
            switch (clickMode.toUpperCase()) {
                case "DOUBLE":
                    clickCount = 2;
                    break;
                case "TRIPLE":
                    clickCount = 3;
                    break;
                case "SINGLE":
                default:
                    clickCount = 1;
                    break;
            }

            // Perform the click sequence
            postClickEvents(eventSource, currentPos, cgMouseButton, mouseDownEventType, mouseUpEventType, clickCount);

            // Add small random movement if enabled
            if (settingsManager.isRandomMovement()) {
                addRandomMovement();
            }

            // Notify that a click was performed
            onClickPerformed();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during native click sequence", e);
        } finally {
            if (eventSource != null) {
                cf.CFRelease(eventSource);
            }
        }
    }

    /**
     * Gets the current mouse cursor position using CoreGraphics.
     *
     * @return A CGPoint with the current coordinates, or null if an error occurs.
     */
    private CGPoint getCurrentMouseLocation() {
        Pointer event = null;
        try {
            event = cg.CGEventCreate(null);
            if (event == null) {
                LOGGER.log(Level.WARNING, "Failed to create null CGEvent to get location.");
                return null;
            }
            return cg.CGEventGetLocation(event);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting mouse location via CoreGraphics", e);
            return null;
        } finally {
            if (event != null) {
                cf.CFRelease(event);
            }
        }
    }

    /**
     * Posts the actual mouse down and mouse up events using CoreGraphics.
     * Handles single, double, and triple clicks by setting the click state.
     */
    private void postClickEvents(Pointer eventSource, CGPoint position, int cgMouseButton,
                                 int mouseDownEventType, int mouseUpEventType, int clickCount) {
        Pointer mouseDownEvent = null;
        Pointer mouseUpEvent = null;

        try {
            // Create Mouse Down Event
            mouseDownEvent = cg.CGEventCreateMouseEvent(eventSource, mouseDownEventType, position, cgMouseButton);
            if (mouseDownEvent == null) {
                LOGGER.log(Level.WARNING, "Failed to create mouse down event.");
                return;
            }
            cg.CGEventSetIntegerValueField(mouseDownEvent, CoreGraphics.kCGMouseEventClickState, clickCount);

            // Create Mouse Up Event
            mouseUpEvent = cg.CGEventCreateMouseEvent(eventSource, mouseUpEventType, position, cgMouseButton);
            if (mouseUpEvent == null) {
                LOGGER.log(Level.WARNING, "Failed to create mouse up event.");
                return;
            }
            cg.CGEventSetIntegerValueField(mouseUpEvent, CoreGraphics.kCGMouseEventClickState, clickCount);

            // Get CPS for timing optimization
            double cps = settingsManager.getCPS();
            long delay = cps > 50 ? 1 : (cps > 20 ? 2 : 5); // Adaptive timing based on CPS

            // Post Mouse Down Event
            cg.CGEventPost(CoreGraphics.kCGHIDEventTap, mouseDownEvent);

            // Short delay between down and up
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.log(Level.WARNING, "Click delay interrupted.", e);
                return;
            }

            // Post Mouse Up Event
            cg.CGEventPost(CoreGraphics.kCGHIDEventTap, mouseUpEvent);

        } finally {
            // Release the events
            if (mouseDownEvent != null) {
                cf.CFRelease(mouseDownEvent);
            }
            if (mouseUpEvent != null) {
                cf.CFRelease(mouseUpEvent);
            }
        }
    }

    /**
     * Gets the CoreGraphics mouse button code based on the setting string.
     */
    private int getCoreGraphicsMouseButton(String buttonName) {
        switch (buttonName.toUpperCase()) {
            case "LEFT":
                return CoreGraphics.kCGMouseButtonLeft;
            case "MIDDLE":
                return CoreGraphics.kCGMouseButtonCenter;
            case "RIGHT":
                return CoreGraphics.kCGMouseButtonRight;
            default:
                LOGGER.log(Level.WARNING, "Unknown mouse button name '{0}'. Defaulting to LEFT.", buttonName);
                return CoreGraphics.kCGMouseButtonLeft;
        }
    }

    /**
     * Gets the CoreGraphics mouse down event type for the given button code.
     */
    private int getMouseDownEventType(int cgButton) {
        switch (cgButton) {
            case CoreGraphics.kCGMouseButtonLeft:
                return CoreGraphics.kCGEventLeftMouseDown;
            case CoreGraphics.kCGMouseButtonRight:
                return CoreGraphics.kCGEventRightMouseDown;
            case CoreGraphics.kCGMouseButtonCenter:
                return CoreGraphics.kCGEventOtherMouseDown;
            default:
                return CoreGraphics.kCGEventLeftMouseDown;
        }
    }

    /**
     * Gets the CoreGraphics mouse up event type for the given button code.
     */
    private int getMouseUpEventType(int cgButton) {
        switch (cgButton) {
            case CoreGraphics.kCGMouseButtonLeft:
                return CoreGraphics.kCGEventLeftMouseUp;
            case CoreGraphics.kCGMouseButtonRight:
                return CoreGraphics.kCGEventRightMouseUp;
            case CoreGraphics.kCGMouseButtonCenter:
                return CoreGraphics.kCGEventOtherMouseUp;
            default:
                return CoreGraphics.kCGEventLeftMouseUp;
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

            // Create a new position
            CGPoint newPosition = new CGPoint(currentPos.x + offsetX, currentPos.y + offsetY);

            // Create and post a mouse moved event
            Pointer eventSource = null;
            Pointer moveEvent = null;

            try {
                eventSource = cg.CGEventSourceCreate(0);
                moveEvent = cg.CGEventCreateMouseEvent(
                        eventSource,
                        CoreGraphics.kCGEventMouseMoved,
                        newPosition,
                        0 // Button not relevant for move
                );

                cg.CGEventPost(CoreGraphics.kCGHIDEventTap, moveEvent);
            } finally {
                if (moveEvent != null) {
                    cf.CFRelease(moveEvent);
                }
                if (eventSource != null) {
                    cf.CFRelease(eventSource);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error applying random movement", e);
        }
    }
}