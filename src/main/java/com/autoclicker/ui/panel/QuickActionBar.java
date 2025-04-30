package com.autoclicker.ui.panel;

import com.autoclicker.service.click.AutoClickerService;
import com.autoclicker.storage.SettingsManager;
import com.autoclicker.ui.frame.MainFrame;
import com.autoclicker.ui.theme.UIThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;

/**
 * A quick action bar that provides easy access to common functions.
 * Redesigned with modern UI styling for better usability.
 */
public class QuickActionBar extends JPanel {
    private final AutoClickerService clickerService;
    private final SettingsManager settingsManager;
    private final MainFrame parentFrame;

    private final List<ActionButton> actionButtons = new ArrayList<>();

    // Quick action buttons
    private ActionButton startStopButton;
    private ActionButton pauseResumeButton;
    private ActionButton increaseSpeedButton;
    private ActionButton decreaseSpeedButton;
    private ActionButton recordButton;
    private ActionButton profilesButton;

    // Constants
    private static final int BUTTON_SIZE = 42;
    private static final int BUTTON_MARGIN = 8;
    private static final int PANEL_PADDING = 12;

    /**
     * Creates a new quick action bar.
     */
    public QuickActionBar(AutoClickerService clickerService, SettingsManager settingsManager, MainFrame parentFrame) {
        this.clickerService = clickerService;
        this.settingsManager = settingsManager;
        this.parentFrame = parentFrame;

        initializeComponents();
    }

    /**
     * Initializes the component.
     */
    private void initializeComponents() {
        UIThemeManager themeManager = UIThemeManager.getInstance();

        setLayout(new FlowLayout(FlowLayout.CENTER, BUTTON_MARGIN, BUTTON_MARGIN));
        setBorder(new EmptyBorder(PANEL_PADDING, PANEL_PADDING, PANEL_PADDING / 2, PANEL_PADDING));
        setBackground(themeManager.getColor("background"));

        // Create action buttons with modern styling
        startStopButton = createActionButton("▶", "Start/Stop Clicking", themeManager.getColor("success"));
        pauseResumeButton = createActionButton("⏸", "Pause/Resume Clicking", themeManager.getColor("warning"));
        increaseSpeedButton = createActionButton("+", "Increase CPS", themeManager.getColor("primary"));
        decreaseSpeedButton = createActionButton("-", "Decrease CPS", themeManager.getColor("primary"));
        recordButton = createActionButton("⏺", "Record Pattern", themeManager.getColor("danger"));
        profilesButton = createActionButton("👤", "Select Profile", themeManager.getColor("secondary"));

        // Add action listeners
        startStopButton.addActionListener(e -> toggleClicking());
        pauseResumeButton.addActionListener(e -> togglePause());
        increaseSpeedButton.addActionListener(e -> increaseCPS());
        decreaseSpeedButton.addActionListener(e -> decreaseCPS());
        recordButton.addActionListener(e -> openPatternRecorder());
        profilesButton.addActionListener(e -> openProfilesPanel());

        // Add buttons to panel
        add(startStopButton);
        add(pauseResumeButton);
        add(increaseSpeedButton);
        add(decreaseSpeedButton);
        add(recordButton);
        add(profilesButton);

        // Add separator
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setPreferredSize(new Dimension(getWidth() - PANEL_PADDING * 2, 1));
        separator.setBackground(themeManager.getColor("border"));

        // Add description label
        JLabel descriptionLabel = new JLabel("Quick Access Toolbar");
        themeManager.styleLabel(descriptionLabel, "secondary");

        // Update button states
        updateButtonStates();
    }

    /**
     * Creates an action button with modern styling.
     */
    private ActionButton createActionButton(String text, String tooltip, Color color) {
        ActionButton button = new ActionButton(text, color);
        button.setToolTipText(tooltip);
        button.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
        actionButtons.add(button);
        return button;
    }

    /**
     * Updates button states based on current application state.
     */
    public void updateButtonStates() {
        boolean isRunning = clickerService.isRunning();
        boolean isPaused = clickerService.isPaused();

        startStopButton.setText(isRunning ? "⏹" : "▶");
        startStopButton.setToolTipText(isRunning ? "Stop Clicking" : "Start Clicking");
        startStopButton.setColor(isRunning ?
                UIThemeManager.getInstance().getColor("danger") :
                UIThemeManager.getInstance().getColor("success"));

        pauseResumeButton.setText(isPaused ? "▶" : "⏸");
        pauseResumeButton.setToolTipText(isPaused ? "Resume Clicking" : "Pause Clicking");
        pauseResumeButton.setEnabled(isRunning);

        increaseSpeedButton.setEnabled(true);
        decreaseSpeedButton.setEnabled(true);
    }

    /**
     * Applies the current theme colors.
     */
    public void applyThemeColors() {
        UIThemeManager themeManager = UIThemeManager.getInstance();
        setBackground(themeManager.getColor("background"));

        // Update button colors
        for (ActionButton button : actionButtons) {
            button.updateTheme(themeManager.isDarkMode());
        }

        // Update specific button colors
        if (startStopButton != null) {
            boolean isRunning = clickerService != null && clickerService.isRunning();
            startStopButton.setColor(isRunning ?
                    themeManager.getColor("danger") :
                    themeManager.getColor("success"));
        }

        repaint();
    }

    /**
     * Toggles clicking on and off.
     */
    private void toggleClicking() {
        clickerService.toggleClicking();
        updateButtonStates();
        parentFrame.updateStatusDisplay();
    }

    /**
     * Toggles pause state.
     */
    private void togglePause() {
        if (clickerService.isRunning()) {
            if (clickerService.isPaused()) {
                clickerService.resume();
            } else {
                clickerService.pause();
            }
            updateButtonStates();
            parentFrame.updateStatusDisplay();
        }
    }

    /**
     * Increases CPS by 1.
     */
    private void increaseCPS() {
        double currentCps = settingsManager.getCPS();
        double newCps = Math.min(currentCps + 1.0, 500.0);
        settingsManager.setCPS(newCps);

        // Save settings
        settingsManager.saveSettings();

        // Update UI
        parentFrame.updateControlPanel();
        parentFrame.updateStatusDisplay();
    }

    /**
     * Decreases CPS by 1.
     */
    private void decreaseCPS() {
        double currentCps = settingsManager.getCPS();
        double newCps = Math.max(currentCps - 1.0, 0.1);
        settingsManager.setCPS(newCps);

        // Save settings
        settingsManager.saveSettings();

        // Update UI
        parentFrame.updateControlPanel();
        parentFrame.updateStatusDisplay();
    }

    /**
     * Opens the pattern recorder tab.
     */
    private void openPatternRecorder() {
        JTabbedPane tabbedPane = parentFrame.getTabbedPane();
        if (tabbedPane != null) {
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                if (tabbedPane.getTitleAt(i).equals("Patterns")) {
                    tabbedPane.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    /**
     * Opens the profiles tab.
     */
    private void openProfilesPanel() {
        JTabbedPane tabbedPane = parentFrame.getTabbedPane();
        if (tabbedPane != null) {
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                if (tabbedPane.getTitleAt(i).equals("Profiles")) {
                    tabbedPane.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    /**
     * Custom round action button with modern styling.
     */
    private class ActionButton extends JButton {
        private Color baseColor;
        private Color hoverColor;
        private Color pressedColor;
        private Color disabledColor;
        private boolean isDarkMode;

        public ActionButton(String text, Color color) {
            super(text);
            this.baseColor = color;
            this.isDarkMode = UIThemeManager.getInstance().isDarkMode();
            updateColors();

            // Configure button appearance
            setFont(new Font("Arial", Font.BOLD, 16));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);

            // Add hover and press effects
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (isEnabled()) {
                        setBackground(hoverColor);
                    }
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (isEnabled()) {
                        setBackground(baseColor);
                    }
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (isEnabled()) {
                        setBackground(pressedColor);
                    }
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (isEnabled()) {
                        if (contains(e.getPoint())) {
                            setBackground(hoverColor);
                        } else {
                            setBackground(baseColor);
                        }
                    }
                    repaint();
                }
            });
        }

        /**
         * Updates the button's color.
         */
        public void setColor(Color color) {
            this.baseColor = color;
            updateColors();
            repaint();
        }

        /**
         * Updates theme-based colors.
         */
        public void updateTheme(boolean isDarkMode) {
            this.isDarkMode = isDarkMode;
            updateColors();
            repaint();
        }

        /**
         * Updates derived colors.
         */
        private void updateColors() {
            // Calculate hover and pressed colors
            if (isDarkMode) {
                hoverColor = UIThemeManager.lighten(baseColor, 0.2f);
                pressedColor = UIThemeManager.lighten(baseColor, 0.1f);
            } else {
                hoverColor = UIThemeManager.lighten(baseColor, 0.1f);
                pressedColor = UIThemeManager.darken(baseColor, 0.1f);
            }

            // Disabled color is always a muted version
            disabledColor = new Color(
                    baseColor.getRed(),
                    baseColor.getGreen(),
                    baseColor.getBlue(),
                    100);

            // Set foreground color based on background brightness
            double luminance = (0.299 * baseColor.getRed() + 0.587 * baseColor.getGreen() + 0.114 * baseColor.getBlue()) / 255;
            setForeground(luminance > 0.5 ? Color.BLACK : Color.WHITE);

            // Set background
            setBackground(baseColor);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();

            // Enable anti-aliasing
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Draw shadow (only in light mode)
            if (!isDarkMode && isEnabled()) {
                g2d.setColor(new Color(0, 0, 0, 20));
                g2d.fillOval(2, 2, getWidth() - 3, getHeight() - 3);
            }

            // Draw button background
            if (isEnabled()) {
                g2d.setColor(getBackground());
            } else {
                g2d.setColor(disabledColor);
            }

            // Fill with gradient for more depth
            Paint originalPaint = g2d.getPaint();
            if (isEnabled()) {
                Color topColor = UIThemeManager.lighten(getBackground(), isDarkMode ? 0.1f : 0.05f);
                Color bottomColor = UIThemeManager.darken(getBackground(), isDarkMode ? 0.05f : 0.1f);
                g2d.setPaint(new GradientPaint(0, 0, topColor, 0, getHeight(), bottomColor));
            }

            g2d.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
            g2d.setPaint(originalPaint);

            // Draw border
            if (isEnabled()) {
                g2d.setColor(isDarkMode ?
                        UIThemeManager.lighten(getBackground(), 0.1f) :
                        UIThemeManager.darken(getBackground(), 0.1f));
            } else {
                g2d.setColor(new Color(150, 150, 150, 100));
            }
            g2d.setStroke(new BasicStroke(1f));
            g2d.drawOval(0, 0, getWidth() - 1, getHeight() - 1);

            g2d.dispose();

            // Paint the text and icon
            super.paintComponent(g);
        }
    }
}