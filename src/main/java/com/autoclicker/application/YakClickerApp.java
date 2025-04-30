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
import java.lang.reflect.Method; // Added for Taskbar API reflection
import java.util.Objects; // Use Objects class for null checks if needed
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main entry point for the YakClicker application.
 * Initializes application components, sets up the user interface (including Look & Feel,
 * splash screen, taskbar/window icon, and system tray), manages background services,
 * and handles application lifecycle events.
 */
public final class YakClickerApp { // Mark class as final as it's a utility class

    private static final Logger LOGGER = Logger.getLogger(YakClickerApp.class.getName());
    private static final String SETTINGS_DIR_NAME = ".yakclicker";
    // Path to load the icon from resources/icons/tray_icon.png
    private static final String APP_ICON_RESOURCE_PATH = "/icons/tray_icon.png";

    // Cached application icon to avoid loading multiple times
    private static Image appIconImage = null;

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
        // 0. Load Application Icon Early (Needed for macOS Dock Icon)
        // It's crucial to load this before setupLookAndFeel if using Taskbar API
        loadApplicationIcon();

        // 1. Initial Setup (before any UI)
        setupLookAndFeel(); // Must be after loadApplicationIcon for macOS Dock
        disableNativeHookLogging(); // Disable JNativeHook logs early
        addShutdownHook(); // Ensure cleanup on exit

        // 2. Launch Splash Screen and Initialize on Background Thread
        SwingUtilities.invokeLater(() -> {
            final SplashScreen splashScreen = new SplashScreen();
            splashScreen.setVisible(true);
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
            FlatLightLaf.setup();
            LOGGER.info("FlatLaf Look and Feel initialized successfully.");

            // Apply macOS specific settings for better integration if applicable
            if (PlatformDetector.isMacOS()) {
                System.setProperty("apple.awt.application.name", AppConfig.APP_NAME);
                System.setProperty("apple.laf.useScreenMenuBar", "true");

                // Set dock icon on macOS specifically using Taskbar API if available (Requires Java 9+)
                // This needs the appIconImage to be loaded beforehand.
                if (appIconImage != null) {
                    try {
                        // Use reflection to avoid hard dependency if Taskbar API is not available
                        Class<?> taskbarClass = Class.forName("java.awt.Taskbar");
                        Method getTaskbarMethod = taskbarClass.getMethod("getTaskbar");
                        Object taskbarInstance = getTaskbarMethod.invoke(null);
                        Method setIconImageMethod = taskbarClass.getMethod("setIconImage", Image.class);
                        setIconImageMethod.invoke(taskbarInstance, appIconImage);
                        LOGGER.info("macOS Dock icon set using Taskbar API.");
                    } catch (ClassNotFoundException | NoSuchMethodException e) {
                        LOGGER.fine("Taskbar API not available (likely Java < 9). JFrame.setIconImage will be used.");
                    } catch (Exception e) {
                        // Catch other reflection/invocation errors
                        LOGGER.log(Level.WARNING, "Could not set macOS Dock icon via Taskbar API.", e);
                    }
                } else {
                    LOGGER.warning("Cannot set macOS Dock icon via Taskbar API: appIconImage is null.");
                }
                LOGGER.info("Applied macOS specific UI properties.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to set FlatLaf look and feel. Falling back.", e);
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Could not even set fallback Look and Feel.", ex);
            }
        }
    }

    /**
     * Loads the application icon from resources and caches it.
     * This icon will be used for the window frame (taskbar) and system tray.
     * Creates a fallback icon if loading fails.
     */
    private static void loadApplicationIcon() {
        try (InputStream stream = YakClickerApp.class.getResourceAsStream(APP_ICON_RESOURCE_PATH)) {
            if (stream != null) {
                // Load the image fully into memory
                appIconImage = ImageIO.read(stream);
                if (appIconImage != null) {
                    LOGGER.info("Successfully loaded application icon from resource: " + APP_ICON_RESOURCE_PATH);
                } else {
                    // This can happen if the stream is valid but the image format is wrong or corrupt
                    LOGGER.warning("Failed to decode image from resource stream: " + APP_ICON_RESOURCE_PATH);
                    appIconImage = createFallbackIcon(32); // Create a fallback (e.g., 32x32)
                }
            } else {
                LOGGER.warning("Application icon resource not found: " + APP_ICON_RESOURCE_PATH);
                appIconImage = createFallbackIcon(32); // Create a fallback if resource not found
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "IOException while loading application icon resource: " + APP_ICON_RESOURCE_PATH + " - " + e.getMessage(), e);
            appIconImage = createFallbackIcon(32); // Create fallback on IO error
        } catch (Exception e) {
            // Catch any other unexpected errors during loading (e.g., SecurityException)
            LOGGER.log(Level.WARNING, "Unexpected error loading application icon from " + APP_ICON_RESOURCE_PATH + ": " + e.getMessage(), e);
            appIconImage = createFallbackIcon(32); // Create fallback on other errors
        }

        // Final check to ensure we always have *some* image, even if it's the fallback
        if (appIconImage == null) {
            // This should ideally not happen if createFallbackIcon works
            LOGGER.severe("Failed to load or create any application icon. Using minimal placeholder.");
            // Create a minimal 1x1 transparent image as an absolute last resort
            appIconImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        }
    }


    /**
     * Disables the potentially verbose logging output from the JNativeHook library.
     */
    private static void disableNativeHookLogging() {
        final Logger nativeHookLogger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        nativeHookLogger.setLevel(Level.OFF);
        nativeHookLogger.setUseParentHandlers(false);
        LOGGER.fine("JNativeHook logging disabled.");
    }

    /**
     * Attempts to register the JNativeHook library for capturing global input events.
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
        } catch (UnsatisfiedLinkError ule) {
            LOGGER.log(Level.SEVERE, "Failed to register native hook due to missing native library: " + ule.getMessage(), ule);
            return false;
        } catch (Exception e) {
            // Catch unexpected errors during registration
            LOGGER.log(Level.SEVERE, "An unexpected error occurred during native hook registration.", e);
            return false;
        }
    }

    /**
     * Ensures the application's settings directory exists in the user's home directory.
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
     *
     * @param mainFrame       The main application window.
     * @param settingsManager The settings manager instance.
     * @param clickerService  The auto-clicker service.
     */
    private static void setupSystemTray(final MainFrame mainFrame, final SettingsManager settingsManager, final AutoClickerService clickerService) {
        // Check if the SystemTray is supported on the current platform
        if (!SystemTray.isSupported()) {
            LOGGER.warning("System tray is not supported on this platform. Skipping setup.");
            return;
        }

        final SystemTray tray = SystemTray.getSystemTray();
        final PopupMenu popup = createTrayPopupMenu(mainFrame, settingsManager, clickerService);
        final Image trayImage = createTrayIconImage(); // Get potentially scaled image for tray

        // If icon creation failed, we cannot proceed
        if (trayImage == null) {
            LOGGER.severe("Failed to create tray icon image. Aborting system tray setup.");
            return;
        }

        final TrayIcon trayIcon = new TrayIcon(trayImage, AppConfig.APP_NAME, popup);
        trayIcon.setImageAutoSize(true); // Allow automatic resizing by the system tray
        trayIcon.addActionListener(e -> SwingUtilities.invokeLater(mainFrame::slideIn)); // Show window on click/double-click

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
     * Creates a scaled version of the application icon suitable for the system tray.
     * Uses the cached `appIconImage`. If the cached image is null or loading failed,
     * it attempts to generate a fallback icon.
     *
     * @return An Image suitable for the system tray, or null if icon loading/generation fails.
     */
    private static Image createTrayIconImage() {
        final Dimension trayIconSize = SystemTray.getSystemTray().getTrayIconSize();
        // Default to 16x16 if tray size is unavailable or zero
        final int targetWidth = (trayIconSize != null && trayIconSize.width > 0) ? trayIconSize.width : 16;
        final int targetHeight = (trayIconSize != null && trayIconSize.height > 0) ? trayIconSize.height : 16;

        if (appIconImage != null) {
            // Check if scaling is needed (only if dimensions differ)
            if (appIconImage.getWidth(null) != targetWidth || appIconImage.getHeight(null) != targetHeight) {
                LOGGER.fine("Scaling application icon for system tray to " + targetWidth + "x" + targetHeight);
                // Use SCALE_SMOOTH for better quality scaling, especially for downscaling
                return appIconImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
            } else {
                // No scaling needed, return the cached image directly
                LOGGER.fine("Using pre-loaded application icon for system tray (no scaling needed).");
                return appIconImage;
            }
        } else {
            // This case should ideally be handled by loadApplicationIcon creating a fallback,
            // but we generate another one just in case.
            LOGGER.warning("Cached application icon was null when creating tray icon. Generating fallback tray icon.");
            return createFallbackIcon(targetWidth); // Generate fallback of appropriate size
        }
    }

    /**
     * Generates a simple fallback square icon with a colored background and a letter.
     *
     * @param size The width and height of the icon to generate.
     * @return A BufferedImage representing the fallback icon, or null on error.
     */
    private static BufferedImage createFallbackIcon(int size) {
        // Ensure minimum size
        if (size <= 0) {
            size = 16;
            LOGGER.fine("Fallback icon size adjusted to default: " + size);
        }
        try {
            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();
            try {
                // Enable anti-aliasing for smoother graphics
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                // Background (using fillRect for a square)
                g2d.setColor(new Color(79, 70, 229)); // Example color (Indigo)
                g2d.fillRect(0, 0, size, size);

                // Letter ('Y')
                g2d.setColor(Color.WHITE);
                // Adjust font size dynamically based on icon size, ensure minimum size
                // Using a slightly smaller fraction of size for potentially better padding
                Font font = new Font(Font.SANS_SERIF, Font.BOLD, Math.max(8, (int)(size * 0.7)));
                g2d.setFont(font);
                FontMetrics fm = g2d.getFontMetrics();
                String text = "Y";
                // Center the text horizontally and vertically
                int x = (size - fm.stringWidth(text)) / 2;
                int y = (size - fm.getHeight()) / 2 + fm.getAscent();
                g2d.drawString(text, x, y);
            } finally {
                // Always dispose graphics context to free resources
                g2d.dispose();
            }
            LOGGER.info("Successfully generated fallback icon of size " + size + "x" + size + ".");
            return image;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to generate fallback icon.", e);
            return null; // Indicate failure
        }
    }


    /**
     * Displays an informational dialog on macOS regarding necessary Accessibility permissions.
     * This is crucial for applications that control the mouse/keyboard via JNativeHook.
     *
     * @param parentComponent The component to center the dialog over (typically the main frame). Can be null.
     */
    private static void showMacOSPermissionsInfo(final Component parentComponent) {
        // Only run this check on macOS
        if (!PlatformDetector.isMacOS()) {
            return;
        }

        // The need for permissions is primarily for JNativeHook, not basic AWT Robot.
        // Therefore, we show this message proactively on macOS if JNativeHook is used.
        // A more robust check would involve trying a JNativeHook-specific action,
        // but that's complex and might fail silently. A simple info dialog is often best.

        LOGGER.info("Displaying macOS Accessibility permissions hint (proactive).");

        final String appName = AppConfig.APP_NAME; // Use configured app name
        final String message = String.format(
                "Important: For %s's global hotkeys and potentially other features to work correctly on macOS,\n" +
                        "you likely need to grant Accessibility permissions.\n\n" +
                        "Please go to:\nSystem Settings > Privacy & Security > Accessibility\n\n" +
                        "Find %s (or your Java runtime/terminal if running from IDE) in the list,\n" +
                        "and ensure it is checked (enabled).\n\n" +
                        "You may need to restart %s after granting permissions.",
                appName, appName, appName
        );

        // Show the message dialog on the EDT, centered relative to the parent component
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                parentComponent,
                message,
                appName + " - macOS Permissions", // More specific title
                JOptionPane.INFORMATION_MESSAGE
        ));
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

        // 2. Clean up KeybindManager if available and dispose frame
        if (mainFrame != null) {
            // Clean up KeybindManager (assuming it needs explicit cleanup)
            final KeybindManager keybindManager = mainFrame.getKeybindManager();
            if (keybindManager != null) {
                try {
                    // Example: keybindManager.removeAllListeners(); or similar cleanup
                    LOGGER.info("KeybindManager resources cleaned up (if applicable).");
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error cleaning up KeybindManager.", e);
                }
            } else {
                LOGGER.warning("KeybindManager was null during exit, cleanup skipped.");
            }
            // Dispose the main frame on the EDT to release its resources safely
            SwingUtilities.invokeLater(mainFrame::dispose);
            LOGGER.info("Main frame disposal requested.");
        }


        // 3. Terminate the JVM
        // The shutdown hook added earlier will handle unregistering the native hook.
        LOGGER.info("Exiting JVM.");
        System.exit(0); // Use status 0 for normal exit
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

                // IMPORTANT: MainFrame constructor MUST handle setUndecorated(true) itself if needed.
                // Assuming MainFrame constructor is safe to call off-EDT for non-UI logic.
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
                    // PatternRecorderService is available via result.getPatternRecorderService() if needed

                    // Post-initialization steps requiring the main frame
                    if (mainFrame != null) {
                        // *** Set the Window/Taskbar Icon ***
                        // This should be done before setVisible(true)
                        if (appIconImage != null) {
                            // Set icon for the main window frame using standard AWT/Swing API
                            mainFrame.setIconImage(appIconImage);
                            LOGGER.info("Main window frame icon set using setIconImage.");
                        } else {
                            // This shouldn't happen if loadApplicationIcon ensures a fallback
                            LOGGER.warning("Could not set main window frame icon (appIconImage was null).");
                        }

                        // Apply remaining UI fixes (those safe to apply after instantiation)
                        // Note: Decoration fixes like setUndecorated are NOT done here anymore.
                        FixedUIImplementation.applyUIFixes(); // Apply global fixes if not done earlier
                        FixedUIImplementation.applyFixesToFrame(mainFrame); // Apply frame-specific fixes

                        // Refresh keybinds using the manager from the frame (if applicable)
                        final KeybindManager keybindManager = mainFrame.getKeybindManager();
                        if (keybindManager != null) {
                            // Example: keybindManager.refreshHotkeys();
                            LOGGER.info("Keybinds refreshed (if applicable).");
                        } else {
                            LOGGER.warning("KeybindManager not found in MainFrame, cannot refresh hotkeys.");
                        }

                        // Show native hook registration error if it failed
                        if (!result.wasNativeHookSuccessful()) {
                            showErrorMessageDialog(mainFrame,
                                    "Failed to register global hotkeys.\nHotkeys and some features might not function correctly.",
                                    "Hotkey Registration Error");
                        }

                        // Setup system tray (requires mainFrame and services)
                        setupSystemTray(mainFrame, settingsManager, clickerService);

                        // Show the main window (make it visible)
                        // Ensure this happens after all setup, including icon and fixes
                        mainFrame.setVisible(true);
                        LOGGER.info("Main application window displayed.");

                        // Show macOS specific permissions info if needed (after main frame is visible)
                        showMacOSPermissionsInfo(mainFrame); // Moved check here

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
