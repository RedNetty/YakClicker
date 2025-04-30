package com.autoclicker.ui.theme;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter; // Import MouseAdapter
import java.awt.event.MouseEvent;   // Import MouseEvent
import java.util.HashMap;
import java.util.Map;
import java.util.Objects; // For null checks

/**
 * Central theme management for the YakClicker application.
 * Provides consistent styling (colors, fonts, borders) across UI components.
 * Supports light and dark modes.
 */
public class UIThemeManager {
    // --- Singleton Instance ---
    private static UIThemeManager instance;

    // --- Theme Storage ---
    private final Map<String, Color> colors = new HashMap<>();
    private final Map<String, Font> fonts = new HashMap<>();
    private final Map<String, Border> borders = new HashMap<>();

    // --- Current State ---
    private boolean isDarkMode = true; // Default to dark mode

    // --- Public Constants ---

    // Spacing (used for padding and margins)
    public static final int SPACING_SMALL = 4;
    public static final int SPACING_MEDIUM = 8;
    public static final int SPACING_LARGE = 16;
    public static final int SPACING_XLARGE = 24;

    // Corner Radii (used for rounded components)
    public static final int CORNER_RADIUS_SMALL = 4;
    public static final int CORNER_RADIUS_MEDIUM = 8;
    public static final int CORNER_RADIUS_LARGE = 12;

    /**
     * Private constructor for Singleton pattern.
     */
    private UIThemeManager() {
        initializeTheme();
    }

    /**
     * Gets the singleton instance of the UIThemeManager.
     * Ensures thread-safe initialization.
     *
     * @return The singleton UIThemeManager instance.
     */
    public static synchronized UIThemeManager getInstance() {
        if (instance == null) {
            instance = new UIThemeManager();
        }
        return instance;
    }

    /**
     * Initializes theme resources (colors, fonts, borders).
     */
    private void initializeTheme() {
        initializeColors();
        initializeFonts();
        initializeBorders();
    }

    /**
     * Initializes the color palette for both light and dark modes.
     */
    private void initializeColors() {
        // --- Core Palette ---
        colors.put("primary", new Color(79, 70, 229));    // Indigo-600
        colors.put("primary_light", new Color(99, 102, 241)); // Indigo-500
        colors.put("primary_dark", new Color(67, 56, 202)); // Indigo-700
        colors.put("secondary", new Color(139, 92, 246)); // Purple-500

        // --- Semantic Colors ---
        colors.put("success", new Color(34, 197, 94));    // Green-500
        colors.put("danger", new Color(239, 68, 68));     // Red-500
        colors.put("warning", new Color(234, 179, 8));    // Yellow-500
        colors.put("info", new Color(14, 165, 233));      // Sky-500

        // --- Light Mode UI ---
        colors.put("background_light", new Color(249, 250, 251)); // Gray-50
        colors.put("surface_light", Color.WHITE);                 // White
        colors.put("card_light", Color.WHITE);                    // White
        colors.put("border_light", new Color(229, 231, 235));     // Gray-200
        colors.put("input_background_light", Color.WHITE);        // White
        colors.put("text_primary_light", new Color(17, 24, 39));   // Gray-900
        colors.put("text_secondary_light", new Color(107, 114, 128)); // Gray-500
        colors.put("text_disabled_light", new Color(156, 163, 175)); // Gray-400
        colors.put("selection_background_light", lighten(getColor("primary"), 0.7f)); // Very light primary
        colors.put("selection_foreground_light", getColor("primary_dark")); // Dark primary text

        // --- Dark Mode UI ---
        colors.put("background_dark", new Color(17, 24, 39));     // Gray-900
        colors.put("surface_dark", new Color(31, 41, 55));      // Gray-800
        colors.put("card_dark", new Color(31, 41, 55));         // Gray-800
        colors.put("border_dark", new Color(55, 65, 81));       // Gray-700
        colors.put("input_background_dark", new Color(55, 65, 81)); // Gray-700
        colors.put("text_primary_dark", new Color(249, 250, 251)); // Gray-50
        colors.put("text_secondary_dark", new Color(156, 163, 175)); // Gray-400
        colors.put("text_disabled_dark", new Color(107, 114, 128)); // Gray-500
        colors.put("selection_background_dark", getColor("primary_dark")); // Dark primary
        colors.put("selection_foreground_dark", Color.WHITE); // White text
    }

    /**
     * Initializes fonts, attempting to use system-appropriate defaults.
     */
    private void initializeFonts() {
        // Determine the base font family based on OS
        String baseFontFamily = getSystemDefaultFontFamily();

        // Define standard font styles
        fonts.put("heading_large", new Font(baseFontFamily, Font.BOLD, 20));
        fonts.put("heading_medium", new Font(baseFontFamily, Font.BOLD, 16));
        fonts.put("heading_small", new Font(baseFontFamily, Font.BOLD, 14));

        fonts.put("body_regular", new Font(baseFontFamily, Font.PLAIN, 13));
        fonts.put("body_bold", new Font(baseFontFamily, Font.BOLD, 13));
        fonts.put("body_medium", new Font(baseFontFamily, Font.PLAIN, 16)); // Larger body text

        fonts.put("small_regular", new Font(baseFontFamily, Font.PLAIN, 11));
        fonts.put("small_bold", new Font(baseFontFamily, Font.BOLD, 11));

        // Fonts for specific UI elements
        fonts.put("button", new Font(baseFontFamily, Font.BOLD, 13));
        fonts.put("label", new Font(baseFontFamily, Font.PLAIN, 13));
        fonts.put("tab", new Font(baseFontFamily, Font.PLAIN, 13));
        fonts.put("input", new Font(baseFontFamily, Font.PLAIN, 13)); // Font for text fields, etc.
    }

    /**
     * Initializes borders for different UI elements and modes.
     */
    private void initializeBorders() {
        // --- Compound Borders with Padding ---

        // Card borders (line + padding)
        borders.put("card_light", createCompoundBorder(
                getColor("border_light"), 1,
                SPACING_MEDIUM, SPACING_LARGE, SPACING_MEDIUM, SPACING_LARGE
        ));
        borders.put("card_dark", createCompoundBorder(
                getColor("border_dark"), 1,
                SPACING_MEDIUM, SPACING_LARGE, SPACING_MEDIUM, SPACING_LARGE
        ));

        // Input field borders (line + padding)
        borders.put("input_light", createCompoundBorder(
                getColor("border_light"), 1,
                SPACING_SMALL, SPACING_MEDIUM, SPACING_SMALL, SPACING_MEDIUM
        ));
        borders.put("input_dark", createCompoundBorder(
                getColor("border_dark"), 1,
                SPACING_SMALL, SPACING_MEDIUM, SPACING_SMALL, SPACING_MEDIUM
        ));

        // --- Simple Padding Borders ---
        borders.put("padding_small", createPaddingBorder(SPACING_SMALL));
        borders.put("padding_medium", createPaddingBorder(SPACING_MEDIUM));
        borders.put("padding_large", createPaddingBorder(SPACING_LARGE));
    }

    // --- Helper Methods for Initialization ---

    /**
     * Creates a compound border with a line and empty padding.
     */
    private Border createCompoundBorder(Color lineColor, int thickness, int top, int left, int bottom, int right) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(lineColor, thickness),
                BorderFactory.createEmptyBorder(top, left, bottom, right)
        );
    }

    /**
     * Creates an empty border with uniform padding.
     */
    private Border createPaddingBorder(int padding) {
        return BorderFactory.createEmptyBorder(padding, padding, padding, padding);
    }

    /**
     * Checks if a specific font family is available on the system.
     *
     * @param fontFamilyName The name of the font family to check.
     * @return true if the font is available, false otherwise.
     */
    private boolean isFontAvailable(String fontFamilyName) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] availableFonts = ge.getAvailableFontFamilyNames();
        for (String name : availableFonts) {
            if (name.equalsIgnoreCase(fontFamilyName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Attempts to get a suitable default system font based on the OS.
     * Falls back to Java's logical "Dialog" font if system fonts aren't found.
     *
     * @return The name of the font family to use.
     */
    private String getSystemDefaultFontFamily() {
        String os = System.getProperty("os.name", "generic").toLowerCase();
        String fontFamily = "Dialog"; // Default fallback

        if (os.contains("win")) {
            if (isFontAvailable("Segoe UI")) {
                fontFamily = "Segoe UI";
            } else if (isFontAvailable("Arial")) {
                fontFamily = "Arial";
            }
        } else if (os.contains("mac")) {
            // On macOS, "San Francisco" is the default, but Java often maps "Helvetica Neue" or others.
            // "Dialog" often resolves well on macOS. Let's try common ones.
            if (isFontAvailable("San Francisco") || isFontAvailable("SF Pro Text")) {
                fontFamily = "San Francisco"; // Or "SF Pro Text" if preferred and available
            } else if (isFontAvailable("Helvetica Neue")) {
                fontFamily = "Helvetica Neue";
            } else if (isFontAvailable("Arial")) {
                fontFamily = "Arial";
            }
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            // Linux has diverse defaults, try common ones
            if (isFontAvailable("Ubuntu")) {
                fontFamily = "Ubuntu";
            } else if (isFontAvailable("Noto Sans")) {
                fontFamily = "Noto Sans";
            } else if (isFontAvailable("DejaVu Sans")) {
                fontFamily = "DejaVu Sans";
            } else if (isFontAvailable("Arial")) {
                fontFamily = "Arial";
            }
        }
        // If no specific font found, "Dialog" (the initial value) will be used.
        System.out.println("UIThemeManager: Using font family - " + fontFamily);
        return fontFamily;
    }


    // --- Theme Mode Management ---

    /**
     * Sets the current theme mode (light or dark).
     *
     * @param darkMode true to activate dark mode, false for light mode.
     */
    public void setDarkMode(boolean darkMode) {
        this.isDarkMode = darkMode;
        // Optionally: Could trigger a global UI update here if needed,
        // e.g., by iterating through open windows or using UIManager defaults.
        // For now, components are expected to re-apply theme when needed.
    }

    /**
     * Checks if dark mode is currently active.
     *
     * @return true if dark mode is active, false otherwise.
     */
    public boolean isDarkMode() {
        return isDarkMode;
    }

    // --- Resource Getters ---

    /**
     * Gets a color by its semantic name (e.g., "primary", "background").
     * Automatically returns the correct color variant for the current theme mode.
     *
     * @param name The semantic name of the color.
     * @return The corresponding Color object, or a default color (magenta) if the name is invalid.
     */
    public Color getColor(String name) {
        Objects.requireNonNull(name, "Color name cannot be null");
        String themeSuffix = isDarkMode ? "_dark" : "_light";
        Color color = null;

        // Check for theme-dependent names first
        switch (name) {
            case "background":
            case "surface":
            case "card":
            case "border":
            case "text_primary":
            case "text_secondary":
            case "text_disabled":
            case "input_background":
            case "selection_background":
            case "selection_foreground":
                color = colors.get(name + themeSuffix);
                break;
            default:
                // Check if it's a direct color name (e.g., "primary", "danger")
                color = colors.get(name);
                // If not found directly, check if it's a theme-specific name missed by the switch
                if (color == null) {
                    color = colors.get(name + themeSuffix);
                }
                break;
        }


        if (color == null) {
            System.err.println("Warning: UIThemeManager - Color not found for name: " + name);
            return Color.MAGENTA; // Return a noticeable default for missing colors
        }
        return color;
    }

    /**
     * Gets a font by its semantic name (e.g., "body_regular", "heading_medium").
     *
     * @param name The semantic name of the font.
     * @return The corresponding Font object, or a default "Dialog" font if the name is invalid.
     */
    public Font getFont(String name) {
        Objects.requireNonNull(name, "Font name cannot be null");
        Font font = fonts.get(name);
        if (font == null) {
            System.err.println("Warning: UIThemeManager - Font not found for name: " + name);
            return new Font("Dialog", Font.PLAIN, 12); // Default fallback font
        }
        return font;
    }

    /**
     * Gets a border by its semantic name (e.g., "card", "input", "padding_medium").
     * Automatically returns the correct border variant for the current theme mode.
     *
     * @param name The semantic name of the border.
     * @return The corresponding Border object, or null if the name is invalid.
     */
    public Border getBorder(String name) {
        Objects.requireNonNull(name, "Border name cannot be null");
        String themeSuffix = isDarkMode ? "_dark" : "_light";
        Border border = null;

        switch (name) {
            case "card":
            case "input":
                border = borders.get(name + themeSuffix);
                break;
            default:
                // Check for direct border names (e.g., "padding_small")
                border = borders.get(name);
                // If not found directly, check if it's a theme-specific name missed by the switch
                if (border == null) {
                    border = borders.get(name + themeSuffix);
                }
                break;
        }

        if (border == null) {
            System.err.println("Warning: UIThemeManager - Border not found for name: " + name);
            // Return a simple empty border as a fallback instead of null
            return BorderFactory.createEmptyBorder();
        }
        return border;
    }

    // --- Component Styling Methods ---

    /**
     * Applies standard theme styling to a JButton.
     * Includes background, foreground (with contrast check), font, padding, and hover/press effects.
     *
     * @param button The JButton to style.
     * @param type   The semantic type of the button (e.g., "primary", "secondary", "danger"). Determines the base color.
     */
    public void styleButton(JButton button, String type) {
        Objects.requireNonNull(button, "Button cannot be null");
        Objects.requireNonNull(type, "Button type cannot be null");

        Color baseColor = getColor(type); // Get base color (e.g., "primary", "danger")
        if (baseColor == Color.MAGENTA) { // Check if color lookup failed
            System.err.println("Warning: Invalid button type '" + type + "'. Using primary color.");
            baseColor = getColor("primary");
        }

        // Determine text color for good contrast
        Color textColor = getTextColorForBackground(baseColor);

        // Apply base styles
        button.setBackground(baseColor);
        button.setForeground(textColor);
        button.setFont(getFont("button"));
        button.setFocusPainted(false); // Remove focus border
        button.setBorderPainted(false); // Use background color instead of border
        button.setOpaque(true); // Ensure background color is visible
        // Apply padding using a border
        button.setBorder(BorderFactory.createEmptyBorder(SPACING_MEDIUM, SPACING_LARGE, SPACING_MEDIUM, SPACING_LARGE));

        // --- Add Hover/Press Effects ---
        // Calculate hover and pressed colors based on theme mode
        final Color hoverColor = isDarkMode ? lighten(baseColor, 0.15f) : darken(baseColor, 0.1f);
        final Color pressedColor = isDarkMode ? lighten(baseColor, 0.1f) : darken(baseColor, 0.2f);
        final Color finalBaseColor = baseColor; // Final variable for use in listener

        // Remove previous listeners added by this method to prevent duplicates
        for (java.awt.event.MouseListener ml : button.getMouseListeners()) {
            if (ml instanceof ButtonHoverListener) { // Identify our specific listener
                button.removeMouseListener(ml);
            }
        }

        // Add the new listener
        button.addMouseListener(new ButtonHoverListener(finalBaseColor, hoverColor, pressedColor));
    }

    /**
     * Applies theme styling to a JPanel to make it look like a card.
     * Sets background and border based on the current theme.
     *
     * @param panel The JPanel to style.
     */
    public void styleCardPanel(JPanel panel) {
        Objects.requireNonNull(panel, "Panel cannot be null");
        panel.setBackground(getColor("card"));
        panel.setBorder(getBorder("card"));
        panel.setOpaque(true); // Ensure card background is painted
    }

    /**
     * Applies theme styling to a JLabel.
     * Sets font and foreground color based on the specified type.
     *
     * @param label The JLabel to style.
     * @param type  The semantic type of the label (e.g., "primary", "secondary", "heading_medium").
     */
    public void styleLabel(JLabel label, String type) {
        Objects.requireNonNull(label, "Label cannot be null");
        Objects.requireNonNull(type, "Label type cannot be null");

        Font font;
        Color color;

        switch (type) {
            case "heading_large":
                font = getFont("heading_large");
                color = getColor("text_primary");
                break;
            case "heading_medium":
                font = getFont("heading_medium");
                color = getColor("text_primary");
                break;
            case "heading_small":
                font = getFont("heading_small");
                color = getColor("text_primary");
                break;
            case "secondary":
                font = getFont("body_regular");
                color = getColor("text_secondary");
                break;
            case "disabled":
                font = getFont("body_regular");
                color = getColor("text_disabled");
                break;
            case "primary": // Style like primary text
            default:
                font = getFont("body_regular");
                color = getColor("text_primary");
                break;
        }
        label.setFont(font);
        label.setForeground(color);
    }

    /**
     * Applies basic theme styling to a JTabbedPane.
     * Sets background, foreground, font, and removes the default content border.
     * Note: Styling selected tabs requires more complex UI delegate manipulation.
     *
     * @param tabbedPane The JTabbedPane to style.
     */
    public void styleTabbedPane(JTabbedPane tabbedPane) {
        Objects.requireNonNull(tabbedPane, "TabbedPane cannot be null");

        // Set overall background and foreground for the tab area
        tabbedPane.setBackground(getColor("surface")); // Use surface color for tab area background
        tabbedPane.setForeground(getColor("text_secondary")); // Default text color for unselected tabs
        tabbedPane.setFont(getFont("tab"));

        // Remove the default border around the content area for a cleaner look
        UIManager.put("TabbedPane.contentBorderInsets", new Insets(0, 0, 0, 0));
        UIManager.put("TabbedPane.tabsOverlapBorder", true); // Modern look

        // Optionally set padding for tabs
        // UIManager.put("TabbedPane.tabInsets", new Insets(SPACING_SMALL, SPACING_LARGE, SPACING_SMALL, SPACING_LARGE));

        // Basic selected tab color (might be overridden by Look and Feel)
        // UIManager.put("TabbedPane.selected", getColor("background")); // Selected tab background
        // UIManager.put("TabbedPane.selectedForeground", getColor("text_primary")); // Selected tab text

        // Force update UI if Look and Feel changes might affect it
        SwingUtilities.updateComponentTreeUI(tabbedPane);

        // Add padding around the entire component if desired
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(SPACING_MEDIUM, SPACING_MEDIUM, 0, SPACING_MEDIUM));
    }

    /**
     * Applies theme styling to text components like JTextField, JTextArea, JPasswordField.
     * Sets background, foreground, selection colors, font, and border.
     *
     * @param component The JTextComponent to style.
     */
    public void styleTextComponent(javax.swing.text.JTextComponent component) {
        Objects.requireNonNull(component, "Text component cannot be null");

        component.setBackground(getColor("input_background"));
        component.setForeground(getColor("text_primary"));
        component.setCaretColor(getColor("text_primary")); // Make caret visible
        component.setFont(getFont("input"));
        component.setBorder(getBorder("input"));

        // Set selection colors
        component.setSelectionColor(getColor("selection_background"));
        component.setSelectedTextColor(getColor("selection_foreground"));

        // Add padding within the component (handled by the 'input' border)
    }


    // --- Color Utility Methods ---

    /**
     * Calculates whether white or black text provides better contrast against a given background color.
     * Uses a simple luminance calculation.
     *
     * @param background The background color.
     * @return Color.WHITE or Color.BLACK.
     */
    public static Color getTextColorForBackground(Color background) {
        Objects.requireNonNull(background, "Background color cannot be null");
        // Calculate luminance (simplified formula)
        double luminance = (0.299 * background.getRed() + 0.587 * background.getGreen() + 0.114 * background.getBlue()) / 255.0;
        // Return black for light backgrounds, white for dark backgrounds
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }


    /**
     * Lightens a given color by a specified factor.
     *
     * @param color  The original color.
     * @param factor The factor to lighten by (0.0 to 1.0). 0.0 means no change, 1.0 means white.
     * @return The lightened color.
     */
    public static Color lighten(Color color, float factor) {
        Objects.requireNonNull(color, "Color cannot be null");
        factor = Math.max(0.0f, Math.min(1.0f, factor)); // Clamp factor
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        r = Math.min(255, r + Math.round((255 - r) * factor));
        g = Math.min(255, g + Math.round((255 - g) * factor));
        b = Math.min(255, b + Math.round((255 - b) * factor));

        return new Color(r, g, b, color.getAlpha());
    }

    /**
     * Darkens a given color by a specified factor.
     *
     * @param color  The original color.
     * @param factor The factor to darken by (0.0 to 1.0). 0.0 means no change, 1.0 means black.
     * @return The darkened color.
     */
    public static Color darken(Color color, float factor) {
        Objects.requireNonNull(color, "Color cannot be null");
        factor = Math.max(0.0f, Math.min(1.0f, factor)); // Clamp factor
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        r = Math.max(0, r - Math.round(r * factor));
        g = Math.max(0, g - Math.round(g * factor));
        b = Math.max(0, b - Math.round(b * factor));

        return new Color(r, g, b, color.getAlpha());
    }

    // --- Private Inner Class for Button Hover Effects ---

    /**
     * MouseAdapter specifically for handling button hover and press effects
     * according to the theme manager's styling.
     */
    private static class ButtonHoverListener extends MouseAdapter {
        private final Color baseColor;
        private final Color hoverColor;
        private final Color pressedColor;

        ButtonHoverListener(Color baseColor, Color hoverColor, Color pressedColor) {
            this.baseColor = baseColor;
            this.hoverColor = hoverColor;
            this.pressedColor = pressedColor;
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            JButton button = (JButton) e.getSource();
            if (button.isEnabled()) {
                button.setBackground(hoverColor);
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            JButton button = (JButton) e.getSource();
            if (button.isEnabled()) {
                // Reset to base color only if not currently pressed
                if (!button.getModel().isPressed()) {
                    button.setBackground(baseColor);
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            JButton button = (JButton) e.getSource();
            if (button.isEnabled()) {
                button.setBackground(pressedColor);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            JButton button = (JButton) e.getSource();
            if (button.isEnabled()) {
                // If mouse is still over the button, set hover color, otherwise base color
                if (button.contains(e.getPoint())) {
                    button.setBackground(hoverColor);
                } else {
                    button.setBackground(baseColor);
                }
            }
        }
    }
}
