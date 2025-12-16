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
import java.awt.geom.Ellipse2D; // Keep for potential future use, though not strictly needed now
import java.util.ArrayList;
import java.util.List;

/**
 * A quick action bar that provides easy access to common functions.
 * Redesigned with modern UI styling for better usability.
 * Uses standard text characters for buttons for maximum compatibility.
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
    private static final int BUTTON_SIZE = 42; // Keep size consistent
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

        // Create action buttons with standard text characters
        startStopButton = createActionButton("S", "Start/Stop Clicking", themeManager.getColor("success")); // S for Start
        pauseResumeButton = createActionButton("P", "Pause/Resume Clicking", themeManager.getColor("warning")); // P for Pause
        increaseSpeedButton = createActionButton("+", "Increase CPS", themeManager.getColor("primary")); // + is safe
        decreaseSpeedButton = createActionButton("-", "Decrease CPS", themeManager.getColor("primary")); // - is safe
        recordButton = createActionButton("O", "Record Pattern", themeManager.getColor("danger")); // O for Record
        profilesButton = createActionButton("U", "Select Profile", themeManager.getColor("secondary")); // U for User/Profile

        // Add action listeners
        startStopButton.addActionListener(e -> toggleClicking());
        pauseResumeButton.addActionListener(e -> togglePause());
        increaseSpeedButton.addActionListener(e -> increaseCPS());
        decreaseSpeedButton.addActionListener(e -> decreaseCPS());
        recordButton.addActionListener(e -> openPatternRecorder());
        profilesButton.addActionListener(e -> openProfilesPanel()); // Ensure "Profiles" tab exists or handle error

        // Add buttons to panel
        add(startStopButton);
        add(pauseResumeButton);
        add(increaseSpeedButton);
        add(decreaseSpeedButton);
        add(recordButton);
        add(profilesButton);

        // Add separator (optional visual element)
        // JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        // separator.setPreferredSize(new Dimension(getWidth() - PANEL_PADDING * 2, 1));
        // separator.setForeground(themeManager.getColor("border")); // Use foreground for separator color
        // separator.setBackground(themeManager.getColor("border"));
        // add(separator); // Consider if separator is needed with this layout

        // Add description label (optional)
        // JLabel descriptionLabel = new JLabel("Quick Access Toolbar");
        // themeManager.styleLabel(descriptionLabel, "secondary");
        // add(descriptionLabel); // Consider if label is needed

        // Update button states initially
        updateButtonStates();
    }

    /**
     * Creates an action button with modern styling.
     */
    private ActionButton createActionButton(String text, String tooltip, Color color) {
        ActionButton button = new ActionButton(text, color);
        button.setToolTipText(tooltip);
        // Set preferred size to maintain circle-like appearance
        button.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
        actionButtons.add(button);
        return button;
    }

    /**
     * Updates button states based on current application state.
     */
    public void updateButtonStates() {
        if (clickerService == null) return; // Prevent NPE if service not ready

        boolean isRunning = clickerService.isRunning();
        boolean isPaused = clickerService.isPaused();

        // Start/Stop Button: Text remains 'S', color and tooltip change
        startStopButton.setText("S"); // Always 'S'
        startStopButton.setToolTipText(isRunning ? "Stop Clicking" : "Start Clicking");
        startStopButton.setColor(isRunning ?
                UIThemeManager.getInstance().getColor("danger") : // Red when running (Stop)
                UIThemeManager.getInstance().getColor("success")); // Green when stopped (Start)

        // Pause/Resume Button: Text changes between 'P' and 'R'
        pauseResumeButton.setText(isPaused ? "R" : "P"); // R for Resume, P for Pause
        pauseResumeButton.setToolTipText(isPaused ? "Resume Clicking" : "Pause Clicking");
        pauseResumeButton.setEnabled(isRunning); // Can only pause/resume if running
        // Keep pause/resume button color consistent (e.g., warning color)
        pauseResumeButton.setColor(UIThemeManager.getInstance().getColor("warning"));


        // Other buttons are generally always enabled unless specific logic dictates otherwise
        increaseSpeedButton.setEnabled(true);
        decreaseSpeedButton.setEnabled(true);
        recordButton.setEnabled(parentFrame.getPatternRecorderService() != null); // Enable only if service exists
        profilesButton.setEnabled(true); // Assuming profiles always available for now
    }

    /**
     * Applies the current theme colors.
     */
    public void applyThemeColors() {
        UIThemeManager themeManager = UIThemeManager.getInstance();
        setBackground(themeManager.getColor("background"));

        // Update button colors based on their function/state
        for (ActionButton button : actionButtons) {
            // Re-apply theme to update hover/pressed/disabled colors derived from base color
            button.updateTheme(themeManager.isDarkMode());
        }

        // Explicitly set colors that depend on state *after* general theme update
        updateButtonStates(); // This re-applies state-dependent colors like start/stop

        repaint();
    }

    /**
     * Toggles clicking on and off.
     */
    private void toggleClicking() {
        if (clickerService == null) return;
        clickerService.toggleClicking();
        updateButtonStates();
        if (parentFrame != null) {
            parentFrame.updateStatusDisplay();
        }
    }

    /**
     * Toggles pause state.
     */
    private void togglePause() {
        if (clickerService == null || !clickerService.isRunning()) return;

        if (clickerService.isPaused()) {
            clickerService.resume();
        } else {
            clickerService.pause();
        }
        updateButtonStates();
        if (parentFrame != null) {
            parentFrame.updateStatusDisplay();
        }
    }

    /**
     * Increases CPS by 1.
     */
    private void increaseCPS() {
        if (settingsManager == null) return;
        double currentCps = settingsManager.getCPS();
        // Define a reasonable maximum CPS if needed
        double maxCps = 1000.0; // Example max
        double newCps = Math.min(currentCps + 1.0, maxCps);
        settingsManager.setCPS(newCps);

        // Save settings
        settingsManager.saveSettings();

        // Update UI
        if (parentFrame != null) {
            parentFrame.updateControlPanel();
            parentFrame.updateStatusDisplay();
        }
    }

    /**
     * Decreases CPS by 1.
     */
    private void decreaseCPS() {
        if (settingsManager == null) return;
        double currentCps = settingsManager.getCPS();
        // Ensure CPS doesn't go below a minimum (e.g., 0.1)
        double minCps = 0.1;
        double newCps = Math.max(currentCps - 1.0, minCps);
        settingsManager.setCPS(newCps);

        // Save settings
        settingsManager.saveSettings();

        // Update UI
        if (parentFrame != null) {
            parentFrame.updateControlPanel();
            parentFrame.updateStatusDisplay();
        }
    }

    /**
     * Opens the pattern recorder tab.
     */
    private void openPatternRecorder() {
        if (parentFrame == null) return;
        JTabbedPane tabbedPane = parentFrame.getTabbedPane();
        if (tabbedPane != null) {
            boolean found = false;
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                if ("Patterns".equalsIgnoreCase(tabbedPane.getTitleAt(i))) {
                    tabbedPane.setSelectedIndex(i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                System.err.println("Warning: 'Patterns' tab not found.");
                // Optionally show a message to the user
            }
        }
    }

    /**
     * Opens the profiles tab.
     * NOTE: Assumes a tab named "Profiles" exists. Add error handling if not.
     */
    private void openProfilesPanel() {
        if (parentFrame == null) return;
        JTabbedPane tabbedPane = parentFrame.getTabbedPane();
        if (tabbedPane != null) {
            boolean found = false;
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                // Make comparison case-insensitive for robustness
                if ("Profiles".equalsIgnoreCase(tabbedPane.getTitleAt(i))) {
                    tabbedPane.setSelectedIndex(i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                System.err.println("Warning: 'Profiles' tab not found.");
                // Optionally show a message to the user via JOptionPane
                // JOptionPane.showMessageDialog(parentFrame,
                //     "Profiles functionality not available (Tab not found).",
                //     "Not Available", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    /**
     * Custom round action button with modern styling.
     * Uses text characters instead of icons.
     */
    private class ActionButton extends JButton {
        private Color baseColor;
        private Color hoverColor;
        private Color pressedColor;
        private Color disabledColor;
        private boolean isDarkMode;

        public ActionButton(String text, Color color) {
            super(text); // Use the text passed in
            this.baseColor = color;
            this.isDarkMode = UIThemeManager.getInstance().isDarkMode();
            updateColors();

            // Configure button appearance
            // Use a common, reliable font. Bold makes the single letters stand out more.
            setFont(new Font("Arial", Font.BOLD, 16)); // Increased font size slightly
            setFocusPainted(false);
            setBorderPainted(false); // Important for custom painting
            setContentAreaFilled(false); // We are filling the area ourselves
            setOpaque(false); // Make component transparent for custom painting

            // Add hover and press effects
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (isEnabled()) {
                        // No need to setBackground here, paintComponent handles hover state
                        repaint(); // Trigger repaint to show hover effect
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (isEnabled()) {
                        // No need to setBackground here
                        repaint(); // Trigger repaint to remove hover effect
                    }
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (isEnabled()) {
                        // No need to setBackground here
                        repaint(); // Trigger repaint to show pressed effect
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (isEnabled()) {
                        // No need to setBackground here
                        repaint(); // Trigger repaint to show hover/normal state
                    }
                }
            });
        }

        /**
         * Updates the button's base color.
         */
        public void setColor(Color color) {
            if (color == null) {
                // Assign a default color if null is passed
                color = UIManager.getColor("Button.background");
                if (color == null) color = Color.GRAY; // Absolute fallback
            }
            this.baseColor = color;
            updateColors(); // Recalculate derived colors
            repaint();
        }

        /**
         * Updates theme-based colors. Call this when the theme changes.
         */
        public void updateTheme(boolean isDarkMode) {
            this.isDarkMode = isDarkMode;
            updateColors(); // Recalculate derived colors based on new theme mode
            repaint();
        }

        /**
         * Recalculates hover, pressed, and disabled colors based on baseColor and theme mode.
         */
        private void updateColors() {
            if (baseColor == null) { // Ensure baseColor is not null
                setColor(UIManager.getColor("Button.background") != null ? UIManager.getColor("Button.background") : Color.GRAY);
            }
            // Calculate hover and pressed colors dynamically
            if (isDarkMode) {
                // In dark mode, usually lighten for hover/press
                hoverColor = UIThemeManager.lighten(baseColor, 0.25f); // More noticeable lighten
                pressedColor = UIThemeManager.lighten(baseColor, 0.15f);
            } else {
                // In light mode, usually darken for hover/press
                hoverColor = UIThemeManager.darken(baseColor, 0.10f);
                pressedColor = UIThemeManager.darken(baseColor, 0.20f); // More noticeable darken
            }

            // Disabled color is a more muted/transparent version
            disabledColor = new Color(
                    baseColor.getRed(),
                    baseColor.getGreen(),
                    baseColor.getBlue(),
                    80); // Reduced alpha for disabled state

            // Set foreground color based on background brightness for better contrast
            // Using a simple luminance calculation
            double luminance = (0.299 * baseColor.getRed() + 0.587 * baseColor.getGreen() + 0.114 * baseColor.getBlue()) / 255.0;
            // Use white text on dark backgrounds, black text on light backgrounds
            setForeground(luminance < 0.5 ? Color.WHITE : Color.BLACK);

            // No need to call setBackground here, paintComponent handles it
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            ButtonModel model = getModel();
            int diameter = Math.min(getWidth(), getHeight()) - 2; // Leave space for border/shadow
            int x = (getWidth() - diameter) / 2;
            int y = (getHeight() - diameter) / 2;

            // Determine background color based on state
            Color bgColor;
            if (!isEnabled()) {
                bgColor = disabledColor;
            } else if (model.isPressed()) {
                bgColor = pressedColor;
            } else if (model.isRollover()) {
                bgColor = hoverColor;
            } else {
                bgColor = baseColor;
            }

            // --- Optional: Subtle Shadow (especially nice in light mode) ---
            if (!isDarkMode && isEnabled()) {
                g2d.setColor(new Color(0, 0, 0, 30)); // Soft shadow color
                g2d.fillOval(x + 1, y + 1, diameter, diameter); // Offset shadow slightly
            }

            // --- Draw the main button circle with Gradient ---
            Paint originalPaint = g2d.getPaint();
            if (isEnabled()) {
                // Adjust gradient direction and colors for a subtle 3D effect
                Color topColor = UIThemeManager.lighten(bgColor, 0.1f);
                Color bottomColor = UIThemeManager.darken(bgColor, 0.1f);
                g2d.setPaint(new GradientPaint(x, y, topColor, x, y + diameter, bottomColor));
            } else {
                g2d.setColor(bgColor); // Use flat color when disabled
            }
            g2d.fillOval(x, y, diameter, diameter);
            g2d.setPaint(originalPaint); // Restore original paint

            // --- Optional: Draw Border ---
            if (isEnabled()) {
                // Border color slightly darker/lighter than the main background
                g2d.setColor(isDarkMode ? UIThemeManager.lighten(bgColor, 0.15f) : UIThemeManager.darken(bgColor, 0.15f));
                g2d.setStroke(new BasicStroke(1f));
                g2d.drawOval(x, y, diameter, diameter);
            }

            g2d.dispose();

            // Let the superclass paint the text centered
            // The text color (foreground) was set in updateColors()
            super.paintComponent(g);
        }

        // Override paintBorder to ensure no default border is drawn
        @Override
        protected void paintBorder(Graphics g) {
            // Do nothing, we handle border drawing in paintComponent
        }

        // Ensure the button is treated as circular for hit detection (optional but good)
        @Override
        public boolean contains(int x, int y) {
            int radius = Math.min(getWidth(), getHeight()) / 2;
            // Check if the point is within the circle
            return Point.distance(x, y, getWidth() / 2.0, getHeight() / 2.0) < radius;
        }
    }
}
