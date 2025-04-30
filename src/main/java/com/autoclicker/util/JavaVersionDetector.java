package com.autoclicker.util;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class to detect Java version and environment details.
 * Useful for diagnostics when the auto-clicker has issues.
 */
public class JavaVersionDetector {
    private static final Logger LOGGER = Logger.getLogger(JavaVersionDetector.class.getName());

    /**
     * Get detailed information about the Java environment.
     * @return String containing Java environment details
     */
    public static String getJavaInfo() {
        StringBuilder info = new StringBuilder();

        info.append("Java Environment Information:\n");
        info.append("--------------------------\n");

        // Java version
        info.append("Java Version: ").append(System.getProperty("java.version")).append("\n");
        info.append("Java Vendor: ").append(System.getProperty("java.vendor")).append("\n");
        info.append("Java Home: ").append(System.getProperty("java.home")).append("\n");

        // OS information
        info.append("\nOS Information:\n");
        info.append("OS Name: ").append(System.getProperty("os.name")).append("\n");
        info.append("OS Version: ").append(System.getProperty("os.version")).append("\n");
        info.append("OS Arch: ").append(System.getProperty("os.arch")).append("\n");

        // Memory information
        Runtime runtime = Runtime.getRuntime();
        info.append("\nMemory Information:\n");
        info.append("Max Memory: ").append(formatSize(runtime.maxMemory())).append("\n");
        info.append("Total Memory: ").append(formatSize(runtime.totalMemory())).append("\n");
        info.append("Free Memory: ").append(formatSize(runtime.freeMemory())).append("\n");

        // Class path
        info.append("\nClass Path:\n");
        info.append(System.getProperty("java.class.path").replace(File.pathSeparator, "\n")).append("\n");

        return info.toString();
    }

    /**
     * Check if the current Java environment is compatible with the application.
     * @return true if compatible, false otherwise
     */
    public static boolean isCompatibleJavaVersion() {
        try {
            String version = System.getProperty("java.version");

            // Parse major version
            int majorVersion;
            if (version.startsWith("1.")) {
                // Old version format (1.8.x)
                majorVersion = Integer.parseInt(version.substring(2, 3));
            } else {
                // New version format (9.x, 11.x, etc.)
                int dotIndex = version.indexOf('.');
                majorVersion = Integer.parseInt(dotIndex > 0 ? version.substring(0, dotIndex) : version);
            }

            // For this application, require Java 8 or higher
            return majorVersion >= 8;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to verify Java version", e);
            return false;
        }
    }

    /**
     * Format memory size in human-readable format.
     * @param bytes memory size in bytes
     * @return formatted string
     */
    private static String formatSize(long bytes) {
        final long kilobyte = 1024;
        final long megabyte = kilobyte * 1024;
        final long gigabyte = megabyte * 1024;

        if (bytes >= gigabyte) {
            return String.format("%.2f GB", (float) bytes / gigabyte);
        } else if (bytes >= megabyte) {
            return String.format("%.2f MB", (float) bytes / megabyte);
        } else if (bytes >= kilobyte) {
            return String.format("%.2f KB", (float) bytes / kilobyte);
        } else {
            return bytes + " bytes";
        }
    }

    /**
     * For testing purposes.
     */
    public static void main(String[] args) {
        System.out.println(getJavaInfo());
        System.out.println("Compatible: " + isCompatibleJavaVersion());
    }
}