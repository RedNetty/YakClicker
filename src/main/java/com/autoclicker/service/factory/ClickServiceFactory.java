package com.autoclicker.service.factory;

import com.autoclicker.core.service.ClickService;
import com.autoclicker.service.click.DefaultClickService;
import com.autoclicker.service.click.linux.LinuxClickService;
import com.autoclicker.service.click.macos.MacOSClickService;
import com.autoclicker.service.click.windows.WindowsClickService;
import com.autoclicker.storage.SettingsManager;
import com.autoclicker.util.PlatformDetector;
import com.sun.jna.Platform;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory class for creating the appropriate {@link ClickService} implementation
 * based on the current operating system and application settings.
 * Ensures a functional ClickService is always returned, falling back to defaults if necessary.
 */
public final class ClickServiceFactory { // Make class final as it only has static methods

    private static final Logger LOGGER = Logger.getLogger(ClickServiceFactory.class.getName());

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private ClickServiceFactory() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Creates and returns the appropriate click service for the current platform.
     * <p>
     * It prioritizes platform-specific implementations if enabled in settings and available
     * on the current OS. Otherwise, it falls back to a default, cross-platform implementation.
     *
     * @param settingsManager The settings manager instance (must not be null).
     * @return A platform-specific or default click service implementation. Never null under normal circumstances.
     * @throws IllegalStateException if even the default fallback service cannot be created.
     */
    public static ClickService createClickService(final SettingsManager settingsManager) {
        // Use final for parameters that shouldn't be reassigned.
        if (settingsManager == null) {
            // Adding null check for robustness, although caller should ensure non-null.
            LOGGER.log(Level.SEVERE, "SettingsManager provided to ClickServiceFactory is null!");
            // Or throw IllegalArgumentException("SettingsManager cannot be null");
            // For now, falling back to default might be safer if null settings are possible downstream
            return createFallbackService(null, "SettingsManager was null");
        }

        // Option 1: Platform-specific optimizations are disabled by settings
        if (!settingsManager.isPlatformSpecificSettings()) {
            LOGGER.info("Platform-specific optimizations disabled by settings. Using standard Java Robot implementation.");
            // Directly attempt to return the default service. If this fails, the fallback logic below handles it.
            return tryCreateDefaultService(settingsManager);
        }

        // Option 2: Try platform-specific service (optimizations enabled)
        LOGGER.fine("Platform-specific optimizations enabled. Detecting OS...");
        ClickService platformService = tryCreatePlatformSpecificService(settingsManager);
        if (platformService != null) {
            // Successfully created platform-specific service
            return platformService;
        }

        // Option 3: Fallback - Platform detection failed, specific service failed, or OS not supported
        LOGGER.warning("Could not create platform-specific service (or OS not supported/detected). Falling back to standard Java Robot implementation.");
        return tryCreateDefaultService(settingsManager);
    }

    /**
     * Attempts to create a platform-specific ClickService based on OS detection.
     *
     * @param settingsManager The settings manager.
     * @return The specific ClickService, or null if detection/creation fails or OS is not supported.
     */
    private static ClickService tryCreatePlatformSpecificService(final SettingsManager settingsManager) {
        try {
            // Combined JNA Platform and custom PlatformDetector checks for robustness
            if (Platform.isMac() || PlatformDetector.isMacOS()) {
                LOGGER.info("Detected macOS, creating macOS-optimized native click service.");
                return new MacOSClickService(settingsManager);
            } else if (Platform.isWindows() || PlatformDetector.isWindows()) {
                LOGGER.info("Detected Windows, creating Windows-optimized click service.");
                return new WindowsClickService(settingsManager);
            } else if (Platform.isLinux() || PlatformDetector.isLinux()) {
                LOGGER.info("Detected Linux, creating Linux-optimized click service.");
                return new LinuxClickService(settingsManager);
            } else {
                LOGGER.info("Current OS not recognized for specific optimizations (e.g., Solaris, AIX, etc.).");
                return null; // OS not explicitly supported for optimization
            }
        } catch (Exception | LinkageError e) {
            // Catch Exception for general issues and LinkageError for native library problems (common with JNA)
            LOGGER.log(Level.WARNING, "Failed to initialize platform-specific click service implementation.", e);
            return null; // Signal failure to create the specific service
        }
    }

    /**
     * Attempts to create the default ClickService.
     * This serves as the primary option when optimizations are off, and as the final fallback.
     *
     * @param settingsManager The settings manager.
     * @return The default ClickService.
     * @throws IllegalStateException if the default service cannot be created.
     */
    private static ClickService tryCreateDefaultService(final SettingsManager settingsManager) {
        try {
            return new DefaultClickService(settingsManager);
        } catch (Exception e) {
            // If even the default service fails, it's a critical issue.
            LOGGER.log(Level.SEVERE, "CRITICAL: Failed to create the default fallback ClickService!", e);
            // Propagate this as a runtime exception, as the application likely cannot function.
            throw new IllegalStateException("Could not create the default ClickService implementation", e);
        }
    }

    /**
     * Helper method for creating fallback service when prerequisites fail (e.g., null settings).
     * Included mainly for handling the null SettingsManager case added for robustness.
     *
     * @param settingsManager The settings manager (might be null here).
     * @param reason Reason for falling back to this method.
     * @return A default click service.
     * @throws IllegalStateException if the default service cannot be created.
     */
    private static ClickService createFallbackService(final SettingsManager settingsManager, String reason) {
        LOGGER.warning("Invoking fallback service creation due to: " + reason);
        // This re-uses the robust default creation logic
        return tryCreateDefaultService(settingsManager);
    }
}