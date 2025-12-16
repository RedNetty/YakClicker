package com.autoclicker.util;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * Utility class providing common UI-related methods used across the application.
 * Centralizes UI styling and component creation to ensure consistency.
 */
public class UIUtils {

    // Common UI constants
    public static final int PADDING_SMALL = 5;
    public static final int PADDING_MEDIUM = 10;
    public static final int PADDING_LARGE = 15;
    public static final int CORNER_RADIUS = 8;

    // Common fonts
    private static final Font HEADER_FONT = new Font("Arial", Font.BOLD, 14);
    private static final Font BODY_FONT = new Font("Arial", Font.PLAIN, 13);
    private static final Font BUTTON_FONT = new Font("Arial", Font.BOLD, 13);

    /**
     * Creates a panel with card-like styling.
     *
     * @param isDarkMode Whether dark mode is active
     * @param bgColor The background color
     * @param borderColor The border color
     * @return A styled JPanel
     */
    public static JPanel createCardPanel(boolean isDarkMode, Color bgColor, Color borderColor) {
        JPanel panel = new JPanel();
        panel.setBackground(bgColor);

        // Different border styling based on theme
        if (isDarkMode) {
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(borderColor, 1),
                    BorderFactory.createEmptyBorder(PADDING_MEDIUM, PADDING_LARGE, PADDING_MEDIUM, PADDING_LARGE)
            ));
        } else {
            // Light mode has subtle shadow effect
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(0,0,0, 30)),
                            BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(0,0,0, 15))
                    ),
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(borderColor, 1),
                            BorderFactory.createEmptyBorder(PADDING_MEDIUM, PADDING_LARGE, PADDING_MEDIUM, PADDING_LARGE)
                    )
            ));
        }

        return panel;
    }

    /**
     * Creates a section header label with appropriate styling.
     *
     * @param text The header text
     * @param textColor The text color
     * @return A styled JLabel
     */
    public static JLabel createSectionHeader(String text, Color textColor) {
        JLabel header = new JLabel(text);
        header.setFont(HEADER_FONT);
        header.setForeground(textColor);
        header.setBorder(BorderFactory.createEmptyBorder(PADDING_SMALL, 0, PADDING_SMALL, 0));
        return header;
    }

    /**
     * Creates a styled button with hover effects.
     *
     * @param text The button text
     * @param color The base button color
     * @param isDarkMode Whether dark mode is active
     * @return A styled JButton
     */
    public static JButton createStyledButton(String text, Color color, boolean isDarkMode) {
        JButton button = new JButton(text);
        styleButton(button, color, isDarkMode);
        return button;
    }

    /**
     * Applies styling to an existing button.
     *
     * @param button The button to style
     * @param color The base button color
     * @param isDarkMode Whether dark mode is active
     */
    public static void styleButton(JButton button, Color color, boolean isDarkMode) {
        // Determine text color based on background brightness
        double luminance = (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue()) / 255;
        Color textColor = luminance > 0.5 ? Color.BLACK : Color.WHITE;

        button.setFont(BUTTON_FONT);
        button.setBackground(color);
        button.setForeground(textColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createEmptyBorder(PADDING_SMALL, PADDING_MEDIUM, PADDING_SMALL, PADDING_MEDIUM));

        // Remove existing mouse listeners to prevent duplicates
        for (MouseListener ml : button.getMouseListeners()) {
            if (ml instanceof MouseAdapter && ml.getClass().isAnonymousClass()) {
                button.removeMouseListener(ml);
            }
        }

        // Add hover effect
        Color hoverColor = isDarkMode ? lighten(color, 0.2f) : darken(color, 0.1f);
        Color pressedColor = isDarkMode ? lighten(color, 0.1f) : darken(color, 0.2f);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(hoverColor);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(color);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(pressedColor);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (button.isEnabled()) {
                    if (button.contains(e.getPoint())) {
                        button.setBackground(hoverColor);
                    } else {
                        button.setBackground(color);
                    }
                }
            }
        });
    }

    /**
     * Creates a panel with a title and content.
     *
     * @param title The title text
     * @param content The content panel
     * @param titleColor The title text color
     * @return A panel with title and content
     */
    public static JPanel createTitledPanel(String title, JPanel content, Color titleColor) {
        JPanel panel = new JPanel(new BorderLayout(0, PADDING_SMALL));
        panel.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(HEADER_FONT);
        titleLabel.setForeground(titleColor);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Creates a styled information card with an icon and text.
     *
     * @param title The card title
     * @param message The message text
     * @param icon The icon text (emoji)
     * @param bgColor The background color
     * @param borderColor The border color
     * @param titleColor The title text color
     * @param messageColor The message text color
     * @return A styled info card panel
     */
    public static JPanel createInfoCard(String title, String message, String icon,
                                        Color bgColor, Color borderColor,
                                        Color titleColor, Color messageColor) {
        JPanel card = new JPanel(new BorderLayout(PADDING_MEDIUM, PADDING_SMALL));
        card.setBackground(bgColor);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, 1),
                BorderFactory.createEmptyBorder(PADDING_MEDIUM, PADDING_LARGE, PADDING_MEDIUM, PADDING_LARGE)
        ));

        // Icon and title panel
        JPanel headerPanel = new JPanel(new BorderLayout(PADDING_SMALL, 0));
        headerPanel.setOpaque(false);

        if (icon != null && !icon.isEmpty()) {
            JLabel iconLabel = new JLabel(icon);
            iconLabel.setFont(new Font("Dialog", Font.PLAIN, 24));
            headerPanel.add(iconLabel, BorderLayout.WEST);
        }

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 14));
        titleLabel.setForeground(titleColor);
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        // Message
        JLabel messageLabel = new JLabel("<html>" + message + "</html>");
        messageLabel.setFont(BODY_FONT);
        messageLabel.setForeground(messageColor);

        card.add(headerPanel, BorderLayout.NORTH);
        card.add(messageLabel, BorderLayout.CENTER);

        return card;
    }

    /**
     * Styles a JComboBox component.
     *
     * @param comboBox The combo box to style
     * @param bgColor The background color
     * @param fgColor The foreground (text) color
     * @param borderColor The border color
     * @param selectionBg The selection background color
     * @param selectionFg The selection foreground color
     */
    public static void styleComboBox(JComboBox<?> comboBox, Color bgColor, Color fgColor,
                                     Color borderColor, Color selectionBg, Color selectionFg) {
        comboBox.setBackground(bgColor);
        comboBox.setForeground(fgColor);
        comboBox.setFont(BODY_FONT);
        comboBox.setBorder(BorderFactory.createLineBorder(borderColor));

        // Set renderer for items
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected,
                                                          boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (c instanceof JLabel) {
                    JLabel label = (JLabel) c;
                    label.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

                    if (isSelected) {
                        label.setBackground(selectionBg);
                        label.setForeground(selectionFg);
                    } else {
                        label.setBackground(bgColor);
                        label.setForeground(fgColor);
                    }
                }

                return c;
            }
        });
    }

    /**
     * Centers a component on screen.
     *
     * @param component The component to center
     */
    public static void centerOnScreen(Component component) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension componentSize = component.getSize();

        int x = (screenSize.width - componentSize.width) / 2;
        int y = (screenSize.height - componentSize.height) / 2;

        component.setLocation(x, y);
    }

    /**
     * Configures a JTable with consistent styling.
     *
     * @param table The table to style
     * @param bgColor Background color
     * @param fgColor Foreground color
     * @param headerBgColor Header background color
     * @param gridColor Grid line color
     */
    public static void styleTable(JTable table, Color bgColor, Color fgColor,
                                  Color headerBgColor, Color gridColor) {
        table.setBackground(bgColor);
        table.setForeground(fgColor);
        table.setFont(BODY_FONT);
        table.setGridColor(gridColor);
        table.setRowHeight(25);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);

        // Style header
        table.getTableHeader().setBackground(headerBgColor);
        table.getTableHeader().setForeground(fgColor);
        table.getTableHeader().setFont(new Font("Dialog", Font.BOLD, 12));

        // Center-align all cells
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }

    /**
     * Styles a checkbox with consistent appearance.
     *
     * @param checkBox The checkbox to style
     * @param textColor The text color
     */
    public static void styleCheckBox(JCheckBox checkBox, Color textColor) {
        checkBox.setFont(BODY_FONT);
        checkBox.setForeground(textColor);
        checkBox.setOpaque(false);
        checkBox.setFocusPainted(false);
    }

    /**
     * Styles a spinner component with consistent appearance.
     *
     * @param spinner The spinner to style
     * @param bgColor Background color
     * @param fgColor Foreground color
     * @param borderColor Border color
     */
    public static void styleSpinner(JSpinner spinner, Color bgColor, Color fgColor, Color borderColor) {
        spinner.setFont(BODY_FONT);

        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JFormattedTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
            textField.setFont(BODY_FONT);
            textField.setForeground(fgColor);
            textField.setBackground(bgColor);
            textField.setBorder(BorderFactory.createLineBorder(borderColor));
        }
    }

    /**
     * Wraps a component in a JScrollPane with consistent styling.
     *
     * @param component The component to wrap
     * @param bgColor Background color for the viewport
     * @return A styled JScrollPane containing the component
     */
    public static JScrollPane createScrollPane(Component component, Color bgColor) {
        JScrollPane scrollPane = new JScrollPane(component);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(bgColor);

        return scrollPane;
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
     * Creates a responsive border that adapts to container size.
     * Useful for components that need to maintain proportion in their padding.
     *
     * @param container The container component
     * @param topPercent Top padding percent of container height
     * @param leftPercent Left padding percent of container width
     * @param bottomPercent Bottom padding percent of container height
     * @param rightPercent Right padding percent of container width
     * @return A border with proportional padding
     */
    public static Border createResponsiveBorder(JComponent container,
                                                float topPercent, float leftPercent,
                                                float bottomPercent, float rightPercent) {
        int top = Math.round(container.getHeight() * topPercent);
        int left = Math.round(container.getWidth() * leftPercent);
        int bottom = Math.round(container.getHeight() * bottomPercent);
        int right = Math.round(container.getWidth() * rightPercent);

        return new EmptyBorder(top, left, bottom, right);
    }

    /**
     * Fixed implementation of setting opacity that works across platforms.
     * Some platforms have issues with component opacity.
     *
     * @param component The component to set opacity for
     * @param isOpaque Whether the component should be opaque
     */
    public static void setComponentOpacity(JComponent component, boolean isOpaque) {
        component.setOpaque(isOpaque);

        // For macOS, we sometimes need additional fixes
        if (PlatformDetector.isMacOS()) {
            if (!isOpaque) {
                component.putClientProperty("apple.awt.transparentBackground", Boolean.TRUE);
            } else {
                component.putClientProperty("apple.awt.transparentBackground", Boolean.FALSE);
            }
        }
    }

    /**
     * Creates a horizontal separator with consistent styling.
     *
     * @param width Preferred width (or 0 for full width)
     * @param color Separator color
     * @return A styled separator component
     */
    public static JSeparator createHorizontalSeparator(int width, Color color) {
        JSeparator separator = new JSeparator(JSeparator.HORIZONTAL);
        separator.setForeground(color);

        if (width > 0) {
            separator.setPreferredSize(new Dimension(width, 1));
            separator.setMaximumSize(new Dimension(width, 1));
        } else {
            separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        }

        return separator;
    }

    /**
     * Applies consistent tooltip styling to a component.
     *
     * @param component The component to style tooltip for
     * @param text The tooltip text
     */
    public static void setStyledTooltip(JComponent component, String text) {
        component.setToolTipText(text);

        // Global UI manager settings for tooltips (can be called once at app startup)
        UIManager.put("ToolTip.background", new Color(50, 50, 50));
        UIManager.put("ToolTip.foreground", Color.WHITE);
        UIManager.put("ToolTip.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 70, 70)),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
    }

    /**
     * Gets system-appropriate UI scaling factor for high DPI displays.
     *
     * @return The scaling factor (typically 1.0 to 2.0)
     */
    public static double getUIScaleFactor() {
        GraphicsDevice screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        GraphicsConfiguration config = screen.getDefaultConfiguration();

        // This is better than Toolkit.getScreenResolution() which doesn't handle multiple monitors well
        return config.getDefaultTransform().getScaleX();
    }

    /**
     * Scale an integer value by the system's UI scaling factor.
     * Useful for maintaining consistent sizing on high-DPI displays.
     *
     * @param value The value to scale
     * @return The scaled value
     */
    public static int scaleForDPI(int value) {
        return (int) Math.round(value * getUIScaleFactor());
    }
}