package com.autoclicker.ui.frame;

import com.autoclicker.config.AppConfig;
import com.autoclicker.service.click.AutoClickerService;
import com.autoclicker.service.keybind.KeybindManager;
import com.autoclicker.service.pattern.PatternRecorderService;
import com.autoclicker.storage.SettingsManager;
import com.autoclicker.ui.theme.UIThemeManager;
import com.autoclicker.util.PlatformDetector;
import com.formdev.flatlaf.FlatLightLaf;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
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
            LOGGER.log(Level.WARNING, "Error applying global UI fixes", e);
        }
    }

    /**
     * Applies specific fixes to a MainFrame instance.
     * These fixes need to be applied after the frame is created but before it's shown.
     * Note: Critical decoration settings like setUndecorated MUST be done in the MainFrame constructor.
     *
     * @param frame The MainFrame instance to fix
     */
    public static void applyFixesToFrame(MainFrame frame) {
        LOGGER.log(Level.INFO, "Applying fixes to MainFrame");

        try {
            // Apply basic frame fixes
            fixFrameMinimumSize(frame);
            // fixWindowDecorations(frame); // Decoration fixes might need to happen within MainFrame

            // Platform-specific frame fixes
            if (PlatformDetector.isMacOS()) {
                applyMacOSFrameFixes(frame);
            } else if (PlatformDetector.isWindows()) {
                applyWindowsFrameFixes(frame);
            } else if (PlatformDetector.isLinux()) {
                applyLinuxFrameFixes(frame);
            }

            // Apply shape and background AFTER the frame is potentially sized/packed in MainFrame
            // This might still need adjustment depending on MainFrame's logic.
            applyShapeAndBackground(frame);


        } catch (Exception e) {
            // Log the exception but don't re-throw, allow other fixes to potentially proceed
            LOGGER.log(Level.WARNING, "Error applying fixes to frame: " + e.getMessage(), e);
        }
    }

    /**
     * Fixes text antialiasing for all components.
     */
    private static void fixTextAntialiasing() {
        // Enable antialiasing for text
        // Consider using RenderingHints on individual components for finer control if needed
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        LOGGER.fine("Text antialiasing settings applied.");
    }

    /**
     * Ensures the frame has a proper minimum size.
     */
    private static void fixFrameMinimumSize(JFrame frame) {
        // Define a reasonable minimum size
        Dimension minimumSize = new Dimension(700, 500);
        // Get current minimum size safely
        Dimension currentMinSize = frame.getMinimumSize();

        // Check if current minimum size is smaller than desired
        if (currentMinSize == null || currentMinSize.width < minimumSize.width || currentMinSize.height < minimumSize.height) {
            // Only set if necessary
            frame.setMinimumSize(minimumSize);
            LOGGER.fine("Frame minimum size set to " + minimumSize.width + "x" + minimumSize.height);
        }
    }

    /**
     * Applies custom shape and background for rounded corners and transparency.
     * This should be called after the frame has its definitive size.
     * WARNING: This might still cause issues if called before the frame is properly sized.
     * Consider calling this from within MainFrame after pack() or setSize().
     *
     * @param frame The JFrame to apply shape and background to.
     */
    private static void applyShapeAndBackground(JFrame frame) {
        // Ensure the frame is not displayable OR is already undecorated before applying shape/background
        // This is a safety check, the core issue is calling setUndecorated too late.
        if (!frame.isDisplayable() || frame.isUndecorated()) {
            try {
                // Check if frame has valid dimensions
                if (frame.getWidth() > 0 && frame.getHeight() > 0) {
                    // Apply custom shape for rounded corners
                    // The radius (e.g., 12, 12) can be adjusted
                    frame.setShape(new java.awt.geom.RoundRectangle2D.Double(
                            0, 0, frame.getWidth(), frame.getHeight(), 12, 12));
                    LOGGER.fine("Applied rounded rectangle shape to frame.");

                    // Make background transparent for custom drawing within the frame's content pane
                    frame.setBackground(new Color(0, 0, 0, 0));
                    LOGGER.fine("Set frame background to transparent.");
                } else {
                    LOGGER.warning("Skipped applying shape/background: Frame dimensions are zero.");
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error applying shape or background: " + e.getMessage(), e);
            }
        } else {
            LOGGER.warning("Skipped applying shape/background: Frame is displayable and decorated.");
        }
    }

    /**
     * Applies macOS-specific UI fixes.
     */
    private static void applyMacOSFixes() {
        // macOS-specific system properties
        System.setProperty("apple.laf.useScreenMenuBar", "true"); // Use the global menu bar
        System.setProperty("apple.awt.application.appearance", "system"); // Match system appearance (Light/Dark)
        System.setProperty("apple.awt.application.name", AppConfig.APP_NAME); // Set app name in menu bar
        // System.setProperty("apple.awt.transparentTitleBar", "true"); // If using native title bar with transparency

        // Fix potential focus issues on buttons in macOS with some L&Fs
        // UIManager.put("Button.focusInputMap", new UIDefaults.LazyInputMap(new Object[] {
        //         "SPACE", "pressed",
        //         "released SPACE", "released"
        // }));
        LOGGER.fine("Applied macOS global UI fixes.");
    }

    /**
     * Applies Windows-specific UI fixes.
     */
    private static void applyWindowsFixes() {
        // Potentially improve high DPI scaling (may depend on Java version and L&F)
        // System.setProperty("sun.java2d.uiScale.enabled", "true"); // Often default in modern JREs

        // Use system font settings (can sometimes improve rendering)
        // System.setProperty("swing.useSystemFontSettings", "true"); // May conflict with L&F fonts
        LOGGER.fine("Applied Windows global UI fixes (if any).");
    }

    /**
     * Applies Linux-specific UI fixes.
     */
    private static void applyLinuxFixes() {
        // Fix input method issues on some Linux distros (e.g., with IBus)
        // System.setProperty("java.awt.im.style", "on-the-spot");

        // Prevent potential GTK theme rendering issues with bold fonts in metal L&F (less relevant with FlatLaf)
        // UIManager.put("swing.boldMetal", Boolean.FALSE);
        LOGGER.fine("Applied Linux global UI fixes (if any).");
    }

    /**
     * Applies macOS-specific fixes to a MainFrame.
     */
    private static void applyMacOSFrameFixes(MainFrame frame) {
        // Ensure the root pane allows full keyboard access if needed
        // frame.getRootPane().putClientProperty("apple.awt.fullKeyboardAccess", Boolean.TRUE);
        LOGGER.fine("Applied macOS frame-specific fixes.");
    }

    /**
     * Applies Windows-specific fixes to a MainFrame.
     */
    private static void applyWindowsFrameFixes(MainFrame frame) {
        // Hint for potential native shadow effects (may depend on L&F and OS)
        // frame.getRootPane().putClientProperty("Window.shadow", Boolean.TRUE);
        LOGGER.fine("Applied Windows frame-specific fixes.");
    }

    /**
     * Applies Linux-specific fixes to a MainFrame.
     */
    private static void applyLinuxFrameFixes(MainFrame frame) {
        // Explicitly set window type if needed for specific window managers
        // frame.setType(Window.Type.NORMAL);
        LOGGER.fine("Applied Linux frame-specific fixes.");
    }

}
