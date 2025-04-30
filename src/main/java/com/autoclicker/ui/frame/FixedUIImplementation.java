package com.autoclicker.ui.frame;

import com.autoclicker.util.PlatformDetector;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Applies platform-specific fixes to UI components to ensure consistent rendering
 * and behavior across different operating systems.
 */
public class FixedUIImplementation {
    private static final Logger LOGGER = Logger.getLogger(FixedUIImplementation.class.getName());

    /**
     * Applies global UI fixes that should be done at application startup.
     * This includes platform-specific adjustments for rendering consistency.
     */
    public static void applyUIFixes() {
        LOGGER.log(Level.INFO, "Applying global UI fixes");

        try {
            // General fixes for all platforms
            fixTextAntialiasing();

            // Platform-specific fixes
            if (PlatformDetector.isMacOS()) {
                applyMacOSFixes();
            } else if (PlatformDetector.isWindows()) {
                applyWindowsFixes();
            } else if (PlatformDetector.isLinux()) {
                applyLinuxFixes();
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error applying UI fixes", e);
        }
    }

    /**
     * Applies specific fixes to a MainFrame instance.
     * These fixes need to be applied after the frame is created but before it's shown.
     *
     * @param frame The MainFrame instance to fix
     */
    public static void applyFixesToFrame(MainFrame frame) {
        LOGGER.log(Level.INFO, "Applying fixes to MainFrame");

        try {
            // Apply basic frame fixes
            fixFrameMinimumSize(frame);
            fixWindowDecorations(frame);

            // Platform-specific frame fixes
            if (PlatformDetector.isMacOS()) {
                applyMacOSFrameFixes(frame);
            } else if (PlatformDetector.isWindows()) {
                applyWindowsFrameFixes(frame);
            } else if (PlatformDetector.isLinux()) {
                applyLinuxFrameFixes(frame);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error applying fixes to frame", e);
        }
    }

    /**
     * Fixes text antialiasing for all components.
     */
    private static void fixTextAntialiasing() {
        // Enable antialiasing for text
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
    }

    /**
     * Ensures the frame has a proper minimum size.
     */
    private static void fixFrameMinimumSize(JFrame frame) {
        Dimension minimumSize = new Dimension(700, 500);
        if (frame.getMinimumSize().width < minimumSize.width ||
                frame.getMinimumSize().height < minimumSize.height) {
            frame.setMinimumSize(minimumSize);
        }
    }

    /**
     * Fixes window decorations based on platform requirements.
     */
    private static void fixWindowDecorations(JFrame frame) {
        // Set proper window decoration style
        frame.setUndecorated(true); // Remove standard decorations

        // Apply custom shape for rounded corners
        frame.setShape(new java.awt.geom.RoundRectangle2D.Double(
                0, 0, frame.getWidth(), frame.getHeight(), 12, 12));

        // Make background transparent for custom drawing
        frame.setBackground(new Color(0, 0, 0, 0));
    }

    /**
     * Applies macOS-specific UI fixes.
     */
    private static void applyMacOSFixes() {
        // macOS-specific system properties
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("apple.awt.application.appearance", "system");
        System.setProperty("apple.awt.application.name", "YakClicker");

        // Fix potential focus issues in macOS
        UIManager.put("Button.focusInputMap", new UIDefaults.LazyInputMap(new Object[] {
                "SPACE", "pressed",
                "released SPACE", "released"
        }));
    }

    /**
     * Applies Windows-specific UI fixes.
     */
    private static void applyWindowsFixes() {
        // Fix scaling issues on high DPI displays
        System.setProperty("sun.java2d.uiScale.enabled", "true");

        // Fix font rendering issues
        System.setProperty("swing.useSystemFontSettings", "true");
    }

    /**
     * Applies Linux-specific UI fixes.
     */
    private static void applyLinuxFixes() {
        // Fix input method issues on some Linux distros
        System.setProperty("java.awt.im.style", "on-the-spot");

        // Fix potential GTK theme issues
        try {
            UIManager.put("swing.boldMetal", Boolean.FALSE);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error applying Linux-specific fixes", e);
        }
    }

    /**
     * Applies macOS-specific fixes to a MainFrame.
     */
    private static void applyMacOSFrameFixes(MainFrame frame) {
        // macOS wants the close/minimize/maximize buttons on the left
        // Our custom title bar already handles this

        // Fix potential focus traversal issues on macOS
        frame.setFocusTraversalKeysEnabled(false);

        // Ensure full keyboard accessibility on macOS
        frame.getRootPane().putClientProperty("apple.awt.fullKeyboardAccess", true);
    }

    /**
     * Applies Windows-specific fixes to a MainFrame.
     */
    private static void applyWindowsFrameFixes(MainFrame frame) {
        // Windows-specific transparency handling
        frame.getRootPane().putClientProperty("Window.shadow", Boolean.TRUE);
    }

    /**
     * Applies Linux-specific fixes to a MainFrame.
     */
    private static void applyLinuxFrameFixes(MainFrame frame) {
        // Linux-specific fixes for window manager compatibility
        frame.setType(Window.Type.NORMAL);
    }
}