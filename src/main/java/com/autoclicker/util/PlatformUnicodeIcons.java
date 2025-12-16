package com.autoclicker.util;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.nio.charset.StandardCharsets;

/**
 * Provides platform-specific Unicode icons with robust font handling.
 */
public class PlatformUnicodeIcons {
    // Enum for cross-platform OS detection
    public enum OS {
        WINDOWS,
        MACOS,
        LINUX,
        UNKNOWN
    }

    /**
     * Detects the current operating system.
     * @return The detected operating system
     */
    public static OS getOperatingSystem() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return OS.WINDOWS;
        } else if (osName.contains("mac")) {
            return OS.MACOS;
        } else if (osName.contains("nux")) {
            return OS.LINUX;
        }
        return OS.UNKNOWN;
    }

    /**
     * Finds a font that can render emoji characters.
     * @return A font capable of rendering emoji, or a fallback font
     */
    public static Font findEmojiSupportedFont() {
        // List of fonts known to support emoji
        String[] emojiSupportedFonts = {
                "Segoe UI Emoji", // Windows
                "Apple Color Emoji", // macOS
                "Noto Color Emoji", // Linux/Cross-platform
                "Android Emoji", // Fallback
                "Twemoji", // Twitter's emoji font
                "EmojiOne", // Another emoji font
                "Segoe UI Symbol", // Windows symbol font
                "Arial Unicode MS" // Older Windows fallback
        };

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] availableFonts = ge.getAvailableFontFamilyNames();

        // First pass: look for exact emoji font matches
        for (String fontName : emojiSupportedFonts) {
            for (String availableFont : availableFonts) {
                if (availableFont.equalsIgnoreCase(fontName)) {
                    return new Font(availableFont, Font.PLAIN, 32);
                }
            }
        }

        // Second pass: look for fonts containing "emoji" or similar
        for (String availableFont : availableFonts) {
            String lowercaseFont = availableFont.toLowerCase();
            if (lowercaseFont.contains("emoji") ||
                    lowercaseFont.contains("color") ||
                    lowercaseFont.contains("symbol")) {
                return new Font(availableFont, Font.PLAIN, 32);
            }
        }

        // Fallback to system default font
        return new Font("Dialog", Font.PLAIN, 32);
    }

    /**
     * Returns a platform-specific Unicode icon with fallback mechanisms.
     * @return Unicode platform icon as a String
     */
    public static String getPlatformIcon() {
        OS os = getOperatingSystem();
        switch (os) {
            case WINDOWS:
                // Technologist emoji (U+1F9D1 U+200D U+1F4BB)
                // Fallback to Windows-specific character if emoji fails
                return "\uD83E\uDDD1\u200D\uD83D\uDCBB";
            case MACOS:
                // Apple emoji (U+1F34E)
                return "\uD83C\uDF4E";
            case LINUX:
                // Penguin emoji (U+1F427)
                return "\uD83D\uDC27";
            default:
                // Computer emoji (U+1F4BB)
                return "\uD83D\uDCBB";
        }
    }

    /**
     * Gets a platform-specific emoji with enhanced rendering support.
     * @return Rendering-friendly platform emoji
     */
    public static EmojiRenderInfo getPlatformEmojiRenderInfo() {
        OS os = getOperatingSystem();
        Font emojiFont = findEmojiSupportedFont();

        switch (os) {
            case WINDOWS:
                return new EmojiRenderInfo(
                        "\uD83E\uDDD1\u200D\uD83D\uDCBB", // Technologist emoji
                        emojiFont
                );
            case MACOS:
                return new EmojiRenderInfo(
                        "\uD83C\uDF4E", // Apple emoji
                        emojiFont
                );
            case LINUX:
                return new EmojiRenderInfo(
                        "\uD83D\uDC27", // Penguin emoji
                        emojiFont
                );
            default:
                return new EmojiRenderInfo(
                        "\uD83D\uDCBB", // Computer emoji
                        emojiFont
                );
        }
    }

    /**
     * Wrapper class to return both emoji string and appropriate rendering font.
     */
    public static class EmojiRenderInfo {
        private final String emoji;
        private final Font font;

        public EmojiRenderInfo(String emoji, Font font) {
            this.emoji = emoji;
            this.font = font;
        }

        public String getEmoji() {
            return emoji;
        }

        public Font getFont() {
            return font;
        }
    }

    /**
     * Demonstrates Unicode platform icon handling.
     * @param args Command-line arguments (not used)
     */
    public static void main(String[] args) {
        OS currentOS = getOperatingSystem();
        EmojiRenderInfo renderInfo = getPlatformEmojiRenderInfo();

        System.out.println("Current Operating System: " + currentOS);
        System.out.println("Platform Icon: " + renderInfo.getEmoji());
        System.out.println("Rendering Font: " + renderInfo.getFont().getFontName());

        // Additional Unicode representation details
        System.out.println("\nUnicode Details:");
        System.out.println("Hex Code Points:");
        for (int cp : renderInfo.getEmoji().codePoints().toArray()) {
            System.out.printf("U+%X ", cp);
        }
    }
}