package com.autoclicker.ui.theme;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Central theme management for the application.
 * Provides consistent styling across all components and screens.
 */
public class UIThemeManager {
    // Singleton instance
    private static UIThemeManager instance;

    // Theme colors
    private final Map<String, Color> colors = new HashMap<>();

    // Font definitions
    private final Map<String, Font> fonts = new HashMap<>();

    // Border definitions
    private final Map<String, Border> borders = new HashMap<>();

    // Current theme mode
    private boolean isDarkMode = true;

    // Spacing constants
    public static final int SPACING_SMALL = 4;
    public static final int SPACING_MEDIUM = 8;
    public static final int SPACING_LARGE = 16;
    public static final int SPACING_XLARGE = 24;

    // Radius constants
    public static final int CORNER_RADIUS_SMALL = 4;
    public static final int CORNER_RADIUS_MEDIUM = 8;
    public static final int CORNER_RADIUS_LARGE = 12;

    /**
     * Private constructor to prevent direct instantiation.
     */
    private UIThemeManager() {
        initializeTheme();
    }

    /**
     * Gets the singleton instance.
     */
    public static synchronized UIThemeManager getInstance() {
        if (instance == null) {
            instance = new UIThemeManager();
        }
        return instance;
    }

    /**
     * Initializes the theme with default values.
     */
    private void initializeTheme() {
        // ---------- COLORS ----------

        // Primary colors
        colors.put("primary", new Color(79, 70, 229));    // Indigo
        colors.put("primary_light", new Color(99, 102, 241)); // Lighter indigo
        colors.put("primary_dark", new Color(67, 56, 202)); // Darker indigo

        // Secondary colors
        colors.put("secondary", new Color(139, 92, 246)); // Purple

        // Accent colors
        colors.put("success", new Color(34, 197, 94));    // Green
        colors.put("danger", new Color(239, 68, 68));     // Red
        colors.put("warning", new Color(234, 179, 8));    // Yellow
        colors.put("info", new Color(14, 165, 233));      // Sky blue

        // UI backgrounds - Light Mode
        colors.put("background_light", new Color(249, 250, 251)); // Very light gray
        colors.put("surface_light", Color.WHITE);         // Pure white
        colors.put("card_light", Color.WHITE);            // Card background
        colors.put("border_light", new Color(226, 232, 240)); // Light border

        // UI backgrounds - Dark Mode
        colors.put("background_dark", new Color(17, 24, 39)); // Very dark blue/gray
        colors.put("surface_dark", new Color(31, 41, 55));  // Dark slate gray
        colors.put("card_dark", new Color(31, 41, 55));     // Card background
        colors.put("border_dark", new Color(55, 65, 81));   // Dark border

        // Text colors - Light Mode
        colors.put("text_primary_light", new Color(17, 24, 39)); // Near black
        colors.put("text_secondary_light", new Color(107, 114, 128)); // Medium gray
        colors.put("text_disabled_light", new Color(156, 163, 175)); // Light gray

        // Text colors - Dark Mode
        colors.put("text_primary_dark", new Color(249, 250, 251)); // Off white
        colors.put("text_secondary_dark", new Color(156, 163, 175)); // Light gray
        colors.put("text_disabled_dark", new Color(107, 114, 128)); // Darker gray

        // Input field colors
        colors.put("input_background_light", Color.WHITE);
        colors.put("input_background_dark", new Color(55, 65, 81));

        // ---------- FONTS ----------

        // Base font family
        String fontFamily = "Segoe UI"; // Modern UI font for Windows

        // Fall back to system fonts if Segoe UI is not available
        if (!isFontAvailable(fontFamily)) {
            fontFamily = getSystemFontFamily();
        }

        // Heading fonts
        fonts.put("heading_large", new Font(fontFamily, Font.BOLD, 20));
        fonts.put("heading_medium", new Font(fontFamily, Font.BOLD, 16));
        fonts.put("heading_small", new Font(fontFamily, Font.BOLD, 14));

        // Body text fonts
        fonts.put("body_regular", new Font(fontFamily, Font.PLAIN, 13));
        fonts.put("body_bold", new Font(fontFamily, Font.BOLD, 13));
        fonts.put("body_medium", new Font(fontFamily, Font.PLAIN, 16));

        // Small text fonts
        fonts.put("small_regular", new Font(fontFamily, Font.PLAIN, 11));
        fonts.put("small_bold", new Font(fontFamily, Font.BOLD, 11));

        // UI elements
        fonts.put("button", new Font(fontFamily, Font.BOLD, 13));
        fonts.put("label", new Font(fontFamily, Font.PLAIN, 13));
        fonts.put("tab", new Font(fontFamily, Font.PLAIN, 13));

        // ---------- BORDERS ----------

        // Card borders
        borders.put("card_light", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(getColor("border_light"), 1),
                BorderFactory.createEmptyBorder(SPACING_MEDIUM, SPACING_LARGE, SPACING_MEDIUM, SPACING_LARGE)
        ));

        borders.put("card_dark", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(getColor("border_dark"), 1),
                BorderFactory.createEmptyBorder(SPACING_MEDIUM, SPACING_LARGE, SPACING_MEDIUM, SPACING_LARGE)
        ));

        // Input field borders
        borders.put("input_light", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(getColor("border_light"), 1),
                BorderFactory.createEmptyBorder(SPACING_SMALL, SPACING_MEDIUM, SPACING_SMALL, SPACING_MEDIUM)
        ));

        borders.put("input_dark", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(getColor("border_dark"), 1),
                BorderFactory.createEmptyBorder(SPACING_SMALL, SPACING_MEDIUM, SPACING_SMALL, SPACING_MEDIUM)
        ));

        // Simple padding borders
        borders.put("padding_small", BorderFactory.createEmptyBorder(
                SPACING_SMALL, SPACING_SMALL, SPACING_SMALL, SPACING_SMALL
        ));

        borders.put("padding_medium", BorderFactory.createEmptyBorder(
                SPACING_MEDIUM, SPACING_MEDIUM, SPACING_MEDIUM, SPACING_MEDIUM
        ));

        borders.put("padding_large", BorderFactory.createEmptyBorder(
                SPACING_LARGE, SPACING_LARGE, SPACING_LARGE, SPACING_LARGE
        ));
    }

    /**
     * Sets the current theme mode.
     *
     * @param darkMode true for dark mode, false for light mode
     */
    public void setDarkMode(boolean darkMode) {
        this.isDarkMode = darkMode;
    }

    /**
     * Checks if dark mode is active.
     *
     * @return true if dark mode is active
     */
    public boolean isDarkMode() {
        return isDarkMode;
    }

    /**
     * Gets a color by name, adjusted for the current theme mode if applicable.
     *
     * @param name The color name
     * @return The Color object, or null if not found
     */
    public Color getColor(String name) {
        // Handle theme-specific colors
        if (name.equals("background")) {
            return isDarkMode ? colors.get("background_dark") : colors.get("background_light");
        } else if (name.equals("surface")) {
            return isDarkMode ? colors.get("surface_dark") : colors.get("surface_light");
        } else if (name.equals("card")) {
            return isDarkMode ? colors.get("card_dark") : colors.get("card_light");
        } else if (name.equals("border")) {
            return isDarkMode ? colors.get("border_dark") : colors.get("border_light");
        } else if (name.equals("text_primary")) {
            return isDarkMode ? colors.get("text_primary_dark") : colors.get("text_primary_light");
        } else if (name.equals("text_secondary")) {
            return isDarkMode ? colors.get("text_secondary_dark") : colors.get("text_secondary_light");
        } else if (name.equals("text_disabled")) {
            return isDarkMode ? colors.get("text_disabled_dark") : colors.get("text_disabled_light");
        } else if (name.equals("input_background")) {
            return isDarkMode ? colors.get("input_background_dark") : colors.get("input_background_light");
        }

        // Return the exact named color
        return colors.get(name);
    }

    /**
     * Gets a font by name.
     *
     * @param name The font name
     * @return The Font object, or a default font if not found
     */
    public Font getFont(String name) {
        Font font = fonts.get(name);
        return font != null ? font : new Font("Dialog", Font.PLAIN, 12);
    }

    /**
     * Gets a border by name, adjusted for the current theme mode if applicable.
     *
     * @param name The border name
     * @return The Border object, or null if not found
     */
    public Border getBorder(String name) {
        // Handle theme-specific borders
        if (name.equals("card")) {
            return isDarkMode ? borders.get("card_dark") : borders.get("card_light");
        } else if (name.equals("input")) {
            return isDarkMode ? borders.get("input_dark") : borders.get("input_light");
        }

        // Return the exact named border
        return borders.get(name);
    }

    /**
     * Applies theme styling to a button.
     *
     * @param button The button to style
     * @param type The button type (primary, secondary, success, danger, warning)
     */
    public void styleButton(JButton button, String type) {
        Color bgColor;
        Color textColor;

        // Set background color based on type
        switch (type) {
            case "primary":
                bgColor = getColor("primary");
                break;
            case "secondary":
                bgColor = getColor("secondary");
                break;
            case "success":
                bgColor = getColor("success");
                break;
            case "danger":
                bgColor = getColor("danger");
                break;
            case "warning":
                bgColor = getColor("warning");
                break;
            default:
                bgColor = getColor("primary");
                break;
        }

        // Determine text color based on background brightness
        double luminance = (0.299 * bgColor.getRed() + 0.587 * bgColor.getGreen() + 0.114 * bgColor.getBlue()) / 255;
        textColor = luminance > 0.5 ? Color.BLACK : Color.WHITE;

        // Apply styling
        button.setBackground(bgColor);
        button.setForeground(textColor);
        button.setFont(getFont("button"));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createEmptyBorder(SPACING_MEDIUM, SPACING_LARGE, SPACING_MEDIUM, SPACING_LARGE));

        // Add hover effect
        final Color baseColor = bgColor;
        final Color hoverColor = isDarkMode ? lighten(bgColor, 0.2f) : darken(bgColor, 0.1f);
        final Color pressedColor = isDarkMode ? lighten(bgColor, 0.1f) : darken(bgColor, 0.2f);

        // Remove existing mouse listeners to prevent duplicates
        for (java.awt.event.MouseListener ml : button.getMouseListeners()) {
            if (ml.getClass().getName().contains("UIThemeManager")) {
                button.removeMouseListener(ml);
            }
        }

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(hoverColor);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(baseColor);
                }
            }

            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(pressedColor);
                }
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (button.isEnabled()) {
                    if (button.contains(e.getPoint())) {
                        button.setBackground(hoverColor);
                    } else {
                        button.setBackground(baseColor);
                    }
                }
            }
        });
    }

    /**
     * Applies theme styling to a panel as a card.
     *
     * @param panel The panel to style
     */
    public void styleCardPanel(JPanel panel) {
        panel.setBackground(getColor("card"));
        panel.setBorder(getBorder("card"));
    }

    /**
     * Applies theme styling to a label.
     *
     * @param label The label to style
     * @param type The label type (primary, secondary, heading, etc.)
     */
    public void styleLabel(JLabel label, String type) {
        switch (type) {
            case "heading_large":
                label.setFont(getFont("heading_large"));
                label.setForeground(getColor("text_primary"));
                break;
            case "heading_medium":
                label.setFont(getFont("heading_medium"));
                label.setForeground(getColor("text_primary"));
                break;
            case "heading_small":
                label.setFont(getFont("heading_small"));
                label.setForeground(getColor("text_primary"));
                break;
            case "secondary":
                label.setFont(getFont("body_regular"));
                label.setForeground(getColor("text_secondary"));
                break;
            default:
                label.setFont(getFont("body_regular"));
                label.setForeground(getColor("text_primary"));
                break;
        }
    }

    /**
     * Applies theme styling to a tab in a JTabbedPane.
     *
     * @param tabbedPane The tabbed pane
     */
    public void styleTabbedPane(JTabbedPane tabbedPane) {
        tabbedPane.setFont(getFont("tab"));
        tabbedPane.setBackground(getColor("background"));
        tabbedPane.setForeground(getColor("text_primary"));
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(
                SPACING_MEDIUM, SPACING_MEDIUM, 0, SPACING_MEDIUM));
    }

    /**
     * Lightens a color by a specified factor.
     *
     * @param color The color to lighten
     * @param factor The factor to lighten by (0-1)
     * @return The lightened color
     */
    public static Color lighten(Color color, float factor) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        r = Math.min(255, r + Math.round((255 - r) * factor));
        g = Math.min(255, g + Math.round((255 - g) * factor));
        b = Math.min(255, b + Math.round((255 - b) * factor));

        return new Color(r, g, b, color.getAlpha());
    }

    /**
     * Darkens a color by a specified factor.
     *
     * @param color The color to darken
     * @param factor The factor to darken by (0-1)
     * @return The darkened color
     */
    public static Color darken(Color color, float factor) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        r = Math.max(0, r - Math.round(r * factor));
        g = Math.max(0, g - Math.round(g * factor));
        b = Math.max(0, b - Math.round(b * factor));

        return new Color(r, g, b, color.getAlpha());
    }

    /**
     * Checks if a font is available on the system.
     *
     * @param fontName The font name to check
     * @return true if the font is available
     */
    private boolean isFontAvailable(String fontName) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fontNames = ge.getAvailableFontFamilyNames();
        for (String name : fontNames) {
            if (name.equalsIgnoreCase(fontName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the appropriate system font family based on the OS.
     *
     * @return The system font family name
     */
    private String getSystemFontFamily() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("mac")) {
            return "SF Pro Text"; // macOS system font
        } else if (os.contains("win")) {
            return "Segoe UI"; // Windows system font
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            return "Ubuntu"; // Common Linux font
        } else {
            return "Dialog"; // Java default font
        }
    }
}