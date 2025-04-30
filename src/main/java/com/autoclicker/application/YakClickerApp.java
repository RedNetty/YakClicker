package com.autoclicker.application;

import com.autoclicker.config.AppConfig;
import com.autoclicker.service.click.AutoClickerService;
import com.autoclicker.service.keybind.KeybindManager;
import com.autoclicker.service.pattern.PatternRecorderService;
import com.autoclicker.storage.SettingsManager;
import com.autoclicker.ui.frame.FixedUIImplementation;
import com.autoclicker.ui.frame.MainFrame;
import com.autoclicker.ui.frame.SplashScreen;
import com.autoclicker.ui.theme.UIThemeManager;
import com.autoclicker.util.PlatformDetector;
import com.formdev.flatlaf.FlatLightLaf;
import com.github.kwhat.jnativehook.GlobalScreen; // Use specific import
import com.github.kwhat.jnativehook.NativeHookException; // Use specific import

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects; // Use Objects class for null checks if needed
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main entry point for the YakClicker application.
 * Initializes application components, sets up the user interface (including Look & Feel,
 * splash screen, and system tray), manages background services, and handles application lifecycle events.
 */
public final class YakClickerApp { // Mark class as final as it's a utility class

    private static final Logger LOGGER = Logger.getLogger(YakClickerApp.class.getName());
    private static final String SETTINGS_DIR_NAME = ".yakclicker";
    private static final String TRAY_ICON_RESOURCE_PATH = "/icon.png"; // Ensure this path is correct

    // Prevent instantiation of this utility class
    private YakClickerApp() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * The main entry point of the application.
     *
     * @param args Command line arguments (not currently used).
     */
    public static void main(final String[] args) {
        // 1. Initial Setup (before any UI)
        setupLookAndFeel();
        disableNativeHookLogging(); // Disable JNativeHook logs early
        addShutdownHook(); // Ensure cleanup on exit

        // 2. Launch Splash Screen and Initialize on Background Thread
        // Use invokeLater to ensure UI operations start on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            final SplashScreen splashScreen = new SplashScreen();
            splashScreen.setVisible(true);

            // Use SwingWorker for background initialization to keep the UI responsive
            final ApplicationInitializer worker = new ApplicationInitializer(splashScreen);
            worker.execute();
        });
    }

    /**
     * Sets up the FlatLaf look and feel for a modern UI appearance.
     * Includes macOS-specific properties for better integration.
     */
    private static void setupLookAndFeel() {
        try {
            FlatLightLaf.setup(); // Simple setup, good default theme handling
            LOGGER.info("FlatLaf Look and Feel initialized successfully.");

            // Apply macOS specific settings for better integration if applicable
            if (PlatformDetector.isMacOS()) {
                System.setProperty("apple.awt.application.name", AppConfig.APP_NAME);
                System.setProperty("apple.laf.useScreenMenuBar", "true");
                LOGGER.info("Applied macOS specific UI properties.");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to set FlatLaf look and feel. Falling back.", e);
            // Attempt to fall back to a cross-platform L&F as a last resort
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Could not even set fallback Look and Feel.", ex);
                // Application might look dated, but should still function
            }
        }
    }

    /**
     * Disables the potentially verbose logging output from the JNativeHook library.
     * Call this early before registering the hook.
     */
    private static void disableNativeHookLogging() {
        // Get the logger for JNativeHook and set its level to OFF
        final Logger nativeHookLogger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        nativeHookLogger.setLevel(Level.OFF);
        // Prevent messages from propagating to the root logger
        nativeHookLogger.setUseParentHandlers(false);
        LOGGER.fine("JNativeHook logging disabled.");
    }

    /**
     * Attempts to register the JNativeHook library for capturing global input events.
     * Logs success or failure. Does not show UI dialogs directly.
     *
     * @return true if the hook was registered successfully, false otherwise.
     */
    private static boolean tryRegisterNativeHook() {
        // Unregister first if already registered (e.g., during a restart/refresh)
        if (GlobalScreen.isNativeHookRegistered()) {
            try {
                GlobalScreen.unregisterNativeHook();
                LOGGER.info("Unregistered existing native hook before re-registering.");
            } catch (NativeHookException e) {
                LOGGER.log(Level.WARNING, "Could not unregister existing native hook.", e);
                // Proceed with registration attempt anyway
            }
        }

        // Attempt registration
        try {
            GlobalScreen.registerNativeHook();
            LOGGER.info("Native hook registered successfully.");
            return true;
        } catch (NativeHookException ex) {
            LOGGER.log(Level.SEVERE, "Failed to register native hook: " + ex.getMessage(), ex);
            return false;
        } catch (Exception e) {
            // Catch unexpected errors during registration
            LOGGER.log(Level.SEVERE, "An unexpected error occurred during native hook registration.", e);
            return false;
        }
    }

    /**
     * Ensures the application's settings directory exists in the user's home directory.
     * Creates it if it's missing.
     */
    private static void ensureSettingsDirectory() {
        final File settingsDir = new File(System.getProperty("user.home"), SETTINGS_DIR_NAME);
        if (!settingsDir.exists()) {
            LOGGER.info("Settings directory not found, creating: " + settingsDir.getAbsolutePath());
            if (settingsDir.mkdirs()) {
                LOGGER.info("Settings directory created successfully.");
            } else {
                // This is usually not critical, but good to warn about
                LOGGER.warning("Could not create settings directory: " + settingsDir.getAbsolutePath());
            }
        } else {
            LOGGER.fine("Settings directory already exists: " + settingsDir.getAbsolutePath());
        }
    }

    /**
     * Configures and adds the application icon to the system tray if supported.
     * Includes menu items for basic application control.
     *
     * @param mainFrame       The main application window, used for actions like 'Show'.
     * @param settingsManager The settings manager instance, needed for the exit action.
     * @param clickerService  The auto-clicker service, controlled by tray menu items.
     */
    private static void setupSystemTray(final MainFrame mainFrame, final SettingsManager settingsManager, final AutoClickerService clickerService) {
        // Check if the SystemTray is supported on the current platform
        if (!SystemTray.isSupported()) {
            LOGGER.warning("System tray is not supported on this platform. Skipping setup.");
            return;
        }

        final SystemTray tray = SystemTray.getSystemTray();
        final PopupMenu popup = createTrayPopupMenu(mainFrame, settingsManager, clickerService);
        final Image iconImage = createTrayIconImage(); // Load or generate the icon

        // If icon creation failed, we cannot proceed
        if (iconImage == null) {
            LOGGER.severe("Failed to create tray icon image. Aborting system tray setup.");
            return;
        }

        final TrayIcon trayIcon = new TrayIcon(iconImage, AppConfig.APP_NAME, popup);
        // Ensure the icon resizes automatically if needed
        trayIcon.setImageAutoSize(true);

        // Add an action listener for double-clicks (or single clicks on some platforms)
        // Typically, this should show the main application window
        trayIcon.addActionListener(e -> SwingUtilities.invokeLater(mainFrame::slideIn));

        try {
            tray.add(trayIcon);
            LOGGER.info(AppConfig.APP_NAME + " added to system tray successfully.");
        } catch (AWTException e) {
            LOGGER.log(Level.SEVERE, "Could not add icon to system tray.", e);
        } catch (Exception e) {
            // Catch any other unexpected errors during tray setup
            LOGGER.log(Level.SEVERE, "An unexpected error occurred during system tray setup.", e);
        }
    }

    /**
     * Creates the popup menu for the system tray icon.
     *
     * @param mainFrame       The main application frame.
     * @param settingsManager The settings manager.
     * @param clickerService  The clicker service.
     * @return The configured PopupMenu.
     */
    private static PopupMenu createTrayPopupMenu(final MainFrame mainFrame, final SettingsManager settingsManager, final AutoClickerService clickerService) {
        final PopupMenu popup = new PopupMenu();

        // --- Menu Items ---
        final MenuItem showItem = new MenuItem(AppConfig.TRAY_MENU_SHOW);
        showItem.addActionListener(e -> SwingUtilities.invokeLater(mainFrame::slideIn)); // Ensure UI updates on EDT

        final MenuItem startItem = new MenuItem(AppConfig.TRAY_MENU_START);
        startItem.addActionListener(e -> {
            clickerService.startClicking();
            SwingUtilities.invokeLater(mainFrame::updateStatusDisplay); // Ensure UI updates on EDT
        });

        final MenuItem stopItem = new MenuItem(AppConfig.TRAY_MENU_STOP);
        stopItem.addActionListener(e -> {
            clickerService.stopClicking();
            SwingUtilities.invokeLater(mainFrame::updateStatusDisplay); // Ensure UI updates on EDT
        });

        final MenuItem exitItem = new MenuItem(AppConfig.TRAY_MENU_EXIT);
        // Use the dedicated exit method for proper cleanup
        exitItem.addActionListener(e -> exitApplication(settingsManager, mainFrame));

        // Add items to popup menu
        popup.add(showItem);
        popup.addSeparator();
        popup.add(startItem);
        popup.add(stopItem);
        popup.addSeparator();
        popup.add(exitItem);

        return popup;
    }


    /**
     * Attempts to load the tray icon from application resources.
     * If loading fails, it generates a simple fallback icon.
     *
     * @return An Image suitable for the system tray, or null if both loading and generation fail.
     */
    private static Image createTrayIconImage() {
        // 1. Attempt to load the icon from embedded resources
        try (InputStream stream = YakClickerApp.class.getResourceAsStream(TRAY_ICON_RESOURCE_PATH)) {
            if (stream != null) {
                final BufferedImage loadedImage = ImageIO.read(stream);
                // Optional: Scale if necessary (System tray icons are typically small, e.g., 16x16 or 32x32)
                // Consider getting preferred size: SystemTray.getSystemTray().getTrayIconSize()
                final Dimension trayIconSize = SystemTray.getSystemTray().getTrayIconSize();
                final int targetWidth = (trayIconSize != null) ? trayIconSize.width : 16;
                final int targetHeight = (trayIconSize != null) ? trayIconSize.height : 16;

                if (loadedImage.getWidth() != targetWidth || loadedImage.getHeight() != targetHeight) {
                    LOGGER.fine("Scaling loaded tray icon to " + targetWidth + "x" + targetHeight);
                    return loadedImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
                }
                LOGGER.info("Successfully loaded tray icon from resource: " + TRAY_ICON_RESOURCE_PATH);
                return loadedImage; // Return original if size matches
            } else {
                LOGGER.warning("Tray icon resource not found: " + TRAY_ICON_RESOURCE_PATH);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "IOException while loading tray icon resource: " + e.getMessage(), e);
        } catch (Exception e) {
            // Catch unexpected errors during loading/scaling
            LOGGER.log(Level.WARNING, "Unexpected error loading tray icon: " + e.getMessage(), e);
        }

        // 2. Fallback: Generate a simple placeholder icon if loading failed
        LOGGER.info("Generating a fallback tray icon.");
        try {
            final Dimension trayIconSize = SystemTray.getSystemTray().getTrayIconSize();
            final int size = (trayIconSize != null) ? Math.min(trayIconSize.width, trayIconSize.height) : 16; // Use preferred size if available
            final BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            final Graphics2D g2d = image.createGraphics();
            try {
                // Enable anti-aliasing for smoother graphics
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Draw a simple background circle
                g2d.setColor(new Color(79, 70, 229)); // Example color (Indigo)
                g2d.fillOval(0, 0, size, size);
                // Draw a character (e.g., 'Y' for YakClicker) in the center
                g2d.setColor(Color.WHITE);
                final Font font = new Font("SansSerif", Font.BOLD, Math.max(8, size - 4)); // Adjust font size
                g2d.setFont(font);
                final FontMetrics fm = g2d.getFontMetrics();
                final String text = "Y";
                final int x = (size - fm.stringWidth(text)) / 2;
                final int y = (size - fm.getHeight()) / 2 + fm.getAscent();
                g2d.drawString(text, x, y);
            } finally {
                // Always dispose graphics context to free resources
                g2d.dispose();
            }
            LOGGER.info("Successfully generated fallback tray icon.");
            return image;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed even to generate fallback tray icon.", e);
            return null; // Indicate complete failure
        }
    }

    /**
     * Displays an informational dialog on macOS regarding necessary Accessibility permissions.
     * This is crucial for applications that control the mouse/keyboard.
     *
     * @param parentComponent The component to center the dialog over (typically the main frame). Can be null.
     */
    private static void showMacOSPermissionsInfo(final Component parentComponent) {
        // A simple check using Robot can sometimes indicate missing permissions,
        // but it's not a definitive test for CGEvent posting capabilities used by JNativeHook.
        // The primary purpose here is to inform the user proactively.
        LOGGER.info("Checking for macOS Accessibility permissions hint...");
        boolean likelyNeedsPermissions = false;
        try {
            // Attempting to create a Robot might fail if basic permissions are missing.
            new Robot();
            LOGGER.info("Robot test passed, basic Accessibility permissions seem granted.");
        } catch (AWTException | SecurityException e) {
            LOGGER.warning("Robot test failed, likely indicating missing Accessibility permissions: " + e.getMessage());
            likelyNeedsPermissions = true;
        }

        // Only show the dialog if the Robot test failed OR if we want to always show it on macOS
        // (adjust condition as needed)
        if (likelyNeedsPermissions || PlatformDetector.isMacOS()) { // Consider showing always on macOS first run?
            final String message = String.format(
                    "Important: For %s to work correctly on macOS, you might need to grant Accessibility permissions.\n\n" +
                            "Please go to:\nSystem Settings > Privacy & Security > Accessibility\n\n" +
                            "Ensure %s (or your Java environment) is listed and enabled.\n\n" +
                            "Without this permission, %s may not be able to control the mouse/keyboard or detect global hotkeys.",
                    AppConfig.APP_NAME, AppConfig.APP_NAME, AppConfig.APP_NAME
            );

            // Show the message dialog on the EDT, centered relative to the parent component
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    parentComponent,
                    message,
                    "macOS Permissions Notice",
                    JOptionPane.INFORMATION_MESSAGE
            ));
        }
    }

    /**
     * Displays a standardized error message dialog, ensuring it runs on the EDT.
     * Used for critical errors, often during initialization.
     *
     * @param parentComponent The component to center the dialog over (can be null).
     * @param message         The error message to display.
     * @param title           The title for the error dialog window.
     */
    private static void showErrorMessageDialog(final Component parentComponent, final String message, final String title) {
        // Ensure this UI operation happens on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                parentComponent,
                message,
                title,
                JOptionPane.ERROR_MESSAGE));
    }

    /**
     * Adds a JVM shutdown hook to perform essential cleanup tasks when the application exits,
     * such as unregistering the native hook.
     */
    private static void addShutdownHook() {
        final Thread shutdownThread = new Thread(() -> {
            LOGGER.info("Shutdown hook initiated...");
            // Attempt to cleanly unregister the native hook
            if (GlobalScreen.isNativeHookRegistered()) {
                try {
                    LOGGER.info("Unregistering native hook during shutdown...");
                    GlobalScreen.unregisterNativeHook();
                    LOGGER.info("Native hook unregistered successfully.");
                } catch (NativeHookException e) {
                    // Log error but don't prevent shutdown
                    LOGGER.log(Level.SEVERE, "Error unregistering native hook during shutdown.", e);
                }
            }
            // Add any other essential cleanup here (e.g., closing files, releasing resources)
            LOGGER.info("Shutdown hook finished.");
        }, "YakClicker-ShutdownHook"); // Give the thread a descriptive name

        Runtime.getRuntime().addShutdownHook(shutdownThread);
        LOGGER.fine("Application shutdown hook registered.");
    }

    /**
     * Handles the application exit process gracefully.
     * Saves settings, cleans up resources, and terminates the JVM.
     *
     * @param settingsManager The settings manager instance to save settings. Can be null.
     * @param mainFrame       The main frame, used to access components like KeybindManager for cleanup. Can be null.
     */
    private static void exitApplication(final SettingsManager settingsManager, final MainFrame mainFrame) {
        LOGGER.info("Initiating application exit sequence...");

        // 1. Save settings if available
        if (settingsManager != null) {
            try {
                settingsManager.saveSettings();
                LOGGER.info("Application settings saved successfully.");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to save settings during exit.", e);
                // Continue exit process despite save failure
            }
        } else {
            LOGGER.warning("SettingsManager was null during exit, settings not saved.");
        }

        // 2. Clean up KeybindManager if available
        if (mainFrame != null) {
            final KeybindManager keybindManager = mainFrame.getKeybindManager();
            if (keybindManager != null) {
                try {

                    LOGGER.info("KeybindManager resources cleaned up.");
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error cleaning up KeybindManager.", e);
                }
            } else {
                LOGGER.warning("KeybindManager was null during exit, cleanup skipped.");
            }
            // Dispose the main frame to release its resources
            SwingUtilities.invokeLater(mainFrame::dispose);
        }


        // 3. Terminate the JVM
        // The shutdown hook added earlier will handle unregistering the native hook.
        LOGGER.info("Exiting JVM.");
        System.exit(0);
    }

    // --- Inner Class for Background Initialization ---

    /**
     * SwingWorker implementation to handle application initialization in the background,
     * preventing the UI from freezing during potentially long-running tasks.
     * Updates the UI on the EDT once initialization is complete or if an error occurs.
     */
    private static class ApplicationInitializer extends SwingWorker<InitializationResult, Void> {
        private final SplashScreen splashScreen; // Keep reference to close it later

        // Constructor
        public ApplicationInitializer(final SplashScreen splashScreen) {
            this.splashScreen = Objects.requireNonNull(splashScreen, "SplashScreen cannot be null");
        }

        /**
         * Performs the core application initialization tasks in a background thread.
         * This includes loading settings, initializing services, and creating the main frame.
         *
         * @return An InitializationResult containing the created components or an error.
         */
        @Override
        protected InitializationResult doInBackground() throws Exception {
            LOGGER.info("Starting background initialization...");
            SettingsManager settingsManager = null;
            AutoClickerService clickerService = null;
            PatternRecorderService patternRecorderService = null;
            MainFrame mainFrame = null;
            boolean nativeHookOk = false;

            try {
                // 1. Ensure settings directory exists
                ensureSettingsDirectory();

                // 2. Load settings and initialize profiles
                settingsManager = new SettingsManager();
                settingsManager.loadSettings();
                settingsManager.initProfiles();
                LOGGER.info("Settings loaded and profiles initialized.");

                // 3. Initialize UI Theme Manager based on loaded settings
                UIThemeManager.getInstance().setDarkMode(settingsManager.isDarkMode());
                LOGGER.info("UI Theme Manager initialized.");

                // 4. Create core services
                clickerService = new AutoClickerService(settingsManager);
                patternRecorderService = new PatternRecorderService(settingsManager, clickerService);
                LOGGER.info("Core services (Clicker, Pattern Recorder) created.");

                // 5. Create the main UI frame (but don't show it yet)
                // Pass final references or effectively final ones to the constructor
                final SettingsManager finalSettingsManager = settingsManager;
                final AutoClickerService finalClickerService = clickerService;
                final PatternRecorderService finalPatternRecorderService = patternRecorderService;
                // Create frame on EDT if it involves complex UI setup, otherwise here is fine
                // Assuming MainFrame constructor is safe to call off-EDT
                mainFrame = new MainFrame(finalClickerService, finalSettingsManager, finalPatternRecorderService);
                LOGGER.info("MainFrame created.");

                // 6. Attempt to register the native hook for global keybinds
                nativeHookOk = tryRegisterNativeHook();
                // Note: UI feedback about hook failure happens in done()

                LOGGER.info("Background initialization tasks completed.");
                return new InitializationResult(mainFrame, settingsManager, clickerService, patternRecorderService, nativeHookOk);

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Critical error during background initialization.", e);
                // Wrap the exception to pass it to the done() method
                return new InitializationResult(e); // Store the exception
            }
        }

        /**
         * Executes on the Event Dispatch Thread (EDT) after doInBackground() completes.
         * Handles the results of the initialization, shows the main frame, sets up the tray,
         * displays error messages, and closes the splash screen.
         */
        @Override
        protected void done() {
            InitializationResult result = null;
            try {
                result = get(); // Retrieve the result (or exception) from doInBackground()

                if (result.isSuccess()) {
                    // Initialization was successful
                    final MainFrame mainFrame = result.getMainFrame();
                    final SettingsManager settingsManager = result.getSettingsManager();
                    final AutoClickerService clickerService = result.getClickerService();
                    // PatternRecorderService is created but might not be needed directly here

                    // Post-initialization steps requiring the main frame
                    if (mainFrame != null) {
                        // Apply UI fixes (if any are needed)
                        FixedUIImplementation.applyUIFixes();
                        FixedUIImplementation.applyFixesToFrame(mainFrame);

                        // Refresh keybinds using the manager from the frame
                        final KeybindManager keybindManager = mainFrame.getKeybindManager();
                        if (keybindManager != null) {
                            LOGGER.info("Keybinds refreshed.");
                        } else {
                            LOGGER.warning("KeybindManager not found in MainFrame, cannot refresh hotkeys.");
                        }

                        // Show native hook registration error if it failed
                        if (!result.wasNativeHookSuccessful()) {
                            showErrorMessageDialog(mainFrame,
                                    "Failed to register global hotkeys.\nHotkeys will not function.",
                                    "Hotkey Registration Error");
                        }

                        // Setup system tray (requires mainFrame and services)
                        setupSystemTray(mainFrame, settingsManager, clickerService);

                        // Show the main window
                        mainFrame.setVisible(true);
                        LOGGER.info("Main application window displayed.");

                        // Show macOS specific info if needed (after main frame is visible)
                        if (PlatformDetector.isMacOS()) {
                            showMacOSPermissionsInfo(mainFrame);
                        }

                    } else {
                        // This case should ideally not happen if result.isSuccess() is true
                        LOGGER.severe("Initialization reported success, but MainFrame is null.");
                        showErrorMessageDialog(splashScreen, "Initialization failed unexpectedly (null MainFrame).", "Initialization Error");
                        System.exit(1); // Critical failure
                    }

                } else {
                    // Initialization failed, exception is available
                    final Exception error = result.getError();
                    final String errorMessage = "Error initializing application: " + (error != null ? error.getMessage() : "Unknown error");
                    LOGGER.log(Level.SEVERE, "Initialization failed in background thread.", error);
                    // Show error dialog, parented to splash screen if main frame wasn't created
                    showErrorMessageDialog(splashScreen, errorMessage, "Application Initialization Error");
                    System.exit(1); // Exit on critical initialization failure
                }

            } catch (InterruptedException | ExecutionException e) {
                // Handle exceptions from the SwingWorker framework itself (e.g., cancellation, execution error)
                final Throwable cause = (e instanceof ExecutionException) ? e.getCause() : e;
                LOGGER.log(Level.SEVERE, "Error retrieving result from SwingWorker.", cause);
                showErrorMessageDialog(splashScreen, "A critical error occurred during application startup: " + cause.getMessage(), "Startup Error");
                System.exit(1); // Exit on critical failure
            } catch (Exception e) {
                // Catch any other unexpected errors during the done() method
                LOGGER.log(Level.SEVERE, "An unexpected error occurred during UI finalization.", e);
                showErrorMessageDialog(splashScreen, "An unexpected error occurred: " + e.getMessage(), "Error");
                System.exit(1); // Exit on critical failure
            }
            finally {
                // Always close the splash screen, regardless of success or failure
                if (splashScreen != null) {
                    splashScreen.dispose();
                    LOGGER.fine("Splash screen disposed.");
                }
            }
        }
    }

    /**
     * Helper class to hold the results of the background initialization.
     * Can store either the successfully created components or the exception that occurred.
     */
    private static class InitializationResult {
        private final MainFrame mainFrame;
        private final SettingsManager settingsManager;
        private final AutoClickerService clickerService;
        private final PatternRecorderService patternRecorderService;
        private final boolean nativeHookSuccessful;
        private final Exception error;

        // Constructor for success case
        InitializationResult(MainFrame mainFrame, SettingsManager settingsManager, AutoClickerService clickerService, PatternRecorderService patternRecorderService, boolean nativeHookSuccessful) {
            this.mainFrame = mainFrame; // Can be null if creation failed but wasn't the main error
            this.settingsManager = Objects.requireNonNull(settingsManager, "SettingsManager cannot be null on success");
            this.clickerService = Objects.requireNonNull(clickerService, "ClickerService cannot be null on success");
            this.patternRecorderService = Objects.requireNonNull(patternRecorderService, "PatternRecorderService cannot be null on success");
            this.nativeHookSuccessful = nativeHookSuccessful;
            this.error = null;
        }

        // Constructor for failure case
        InitializationResult(Exception error) {
            this.mainFrame = null;
            this.settingsManager = null;
            this.clickerService = null;
            this.patternRecorderService = null;
            this.nativeHookSuccessful = false; // Assume hook failed if initialization failed overall
            this.error = Objects.requireNonNull(error, "Error cannot be null on failure");
        }

        boolean isSuccess() {
            return error == null;
        }

        MainFrame getMainFrame() {
            return mainFrame;
        }

        SettingsManager getSettingsManager() {
            return settingsManager;
        }

        AutoClickerService getClickerService() {
            return clickerService;
        }

        PatternRecorderService getPatternRecorderService() {
            return patternRecorderService;
        }

        boolean wasNativeHookSuccessful() {
            return nativeHookSuccessful;
        }

        Exception getError() {
            return error;
        }
    }
}
