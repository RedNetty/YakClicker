package com.autoclicker.util;

/**
 * Utility class to detect the current operating system and platform-specific details.
 */
public class PlatformDetector {

    // Operating system types
    public enum OS {
        WINDOWS,
        MACOS,
        LINUX,
        OTHER
    }

    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();

    /**
     * Detects the current operating system.
     * @return The detected operating system
     */
    public static OS getOperatingSystem() {
        if (OS_NAME.contains("win")) {
            return OS.WINDOWS;
        } else if (OS_NAME.contains("mac")) {
            return OS.MACOS;
        } else if (OS_NAME.contains("nix") || OS_NAME.contains("nux") || OS_NAME.contains("aix")) {
            return OS.LINUX;
        } else {
            return OS.OTHER;
        }
    }

    /**
     * Checks if the current OS is macOS.
     * @return true if running on macOS, false otherwise
     */
    public static boolean isMacOS() {
        return getOperatingSystem() == OS.MACOS;
    }

    /**
     * Checks if the current OS is Windows.
     * @return true if running on Windows, false otherwise
     */
    public static boolean isWindows() {
        return getOperatingSystem() == OS.WINDOWS;
    }

    /**
     * Checks if the current OS is Linux.
     * @return true if running on Linux, false otherwise
     */
    public static boolean isLinux() {
        return getOperatingSystem() == OS.LINUX;
    }

    /**
     * Gets the operating system name and version for display.
     * @return A string containing OS name and version
     */
    public static String getOSNameAndVersion() {
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        return osName + " " + osVersion;
    }

    /**
     * Gets the Java version.
     * @return The Java version string
     */
    public static String getJavaVersion() {
        return System.getProperty("java.version");
    }

    /**
     * Gets the Java vendor.
     * @return The Java vendor string
     */
    public static String getJavaVendor() {
        return System.getProperty("java.vendor");
    }
}