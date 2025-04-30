package com.autoclicker.ui.panel;

import com.autoclicker.ui.frame.MainFrame;
import com.autoclicker.ui.theme.UIThemeManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

/**
 * A visual indicator panel that shows clicking activity.
 * Provides visual feedback when clicking is active with animated dots.
 * Redesigned with modern UI styling.
 *
 * Fixed potential flashing issue by ensuring double buffering is enabled.
 */
public class ClickIndicatorPanel extends JPanel {
    private static final int MAX_DOTS = 20;
    private static final int DOT_SIZE = 6;
    private static final int DOT_LIFE_MS = 400; // Lifespan of a dot in milliseconds
    private static final int PANEL_CORNER_RADIUS = 12; // Corner radius for the panel background
    private static final float DOT_SPAWN_CHANCE = 0.3f; // Chance to spawn a new dot each frame if active
    private static final int ANIMATION_INTERVAL_MS = 16; // ~60 FPS animation update rate

    private volatile boolean isActive = false; // Use volatile for thread safety if accessed from other threads
    private final Queue<ClickDot> dots = new LinkedList<>();
    private final Timer animationTimer;
    private final Random random = new Random();

    // Theme colors - initialized in constructor
    private Color indicatorColor;
    private Color backgroundColor;
    private Color borderColor;
    private Color activeColor;
    private Color inactiveColor;
    private Color statusTextColor; // Added for better theme control

    /**
     * Represents a single animated dot with position, creation time, and alpha (transparency).
     */
    private static class ClickDot {
        float x, y; // Position
        float alpha = 1.0f; // Current transparency (1.0 = fully opaque, 0.0 = fully transparent)
        long createdTime; // Timestamp when the dot was created

        ClickDot(float x, float y) {
            this.x = x;
            this.y = y;
            this.createdTime = System.currentTimeMillis();
        }

        /**
         * Updates the dot's alpha based on its age relative to DOT_LIFE_MS.
         * Alpha decreases linearly over time.
         */
        void update() {
            long age = System.currentTimeMillis() - createdTime;
            // Calculate alpha: starts at 1.0 and decreases to 0.0 over DOT_LIFE_MS
            alpha = 1.0f - (float) age / DOT_LIFE_MS;
            // Clamp alpha value to be between 0.0 and 1.0
            if (alpha < 0) alpha = 0;
            if (alpha > 1.0f) alpha = 1.0f; // Should not happen with current logic, but safe practice
        }

        /**
         * Checks if the dot has faded out completely (alpha is zero or less).
         * @return true if the dot's alpha is <= 0, false otherwise.
         */
        boolean isDead() {
            return alpha <= 0;
        }
    }

    /**
     * Creates a new click indicator panel.
     * Initializes components, theme colors, and starts the animation timer.
     */
    public ClickIndicatorPanel() {
        // Set preferred and minimum sizes for the panel
        setPreferredSize(new Dimension(50, 120));
        setMinimumSize(new Dimension(30, 80));

        // *** Explicitly enable double buffering ***
        // Although JPanel is double-buffered by default, being explicit can help in some complex UI scenarios.
        setDoubleBuffered(true);

        // Initialize theme colors using the UIThemeManager
        applyThemeColors(null); // Apply default theme initially

        // Animation timer setup
        // This timer runs on the Swing Event Dispatch Thread (EDT), which is crucial for GUI updates.
        animationTimer = new Timer(ANIMATION_INTERVAL_MS, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateAnimation(); // Perform animation logic
                repaint(); // Schedule a repaint of the component
            }
        });
        animationTimer.start();

        // Set panel to non-opaque to allow drawing a custom rounded background
        // The background underneath this panel might show through if not fully covered by paintComponent.
        setOpaque(false);
    }

    /**
     * Updates the state of all dots and adds new ones if active.
     * This method is called by the animation timer.
     */
    private void updateAnimation() {
        // Update existing dots and remove dead ones
        // Using removeIf is efficient for removing elements while iterating
        dots.removeIf(dot -> {
            dot.update(); // Update dot's alpha
            return dot.isDead(); // Return true if the dot should be removed
        });

        // Add new dots randomly if the indicator is active and below the max count
        if (isActive && dots.size() < MAX_DOTS && random.nextFloat() < DOT_SPAWN_CHANCE) {
            addClickDot();
        }
    }

    /**
     * Sets the active state of the indicator (e.g., when clicking starts or stops).
     *
     * @param active true to set the indicator to active state, false for inactive.
     */
    public void setActive(boolean active) {
        // Only update if the state actually changes
        if (this.isActive != active) {
            this.isActive = active;
            // Consider if an initial dot should be added immediately upon activation
            // if (active) {
            //     addClickDot();
            // }
            // Repaint needed to update the status indicator (color/text) immediately
            repaint();
        }
    }

    /**
     * Adds a single new click dot at a random position within the panel boundaries.
     * Ensures the maximum number of dots is not exceeded.
     */
    public void addClickDot() {
        if (dots.size() < MAX_DOTS) {
            // Calculate random position within the panel's bounds
            // Adds some randomness around the center
            float centerX = getWidth() * 0.5f;
            float centerY = getHeight() * 0.5f;
            float spreadX = getWidth() * 0.4f; // Spread dots horizontally
            float spreadY = getHeight() * 0.4f; // Spread dots vertically

            float x = centerX + (random.nextFloat() - 0.5f) * spreadX;
            float y = centerY + (random.nextFloat() - 0.5f) * spreadY;

            dots.add(new ClickDot(x, y));
        }
    }

    /**
     * Updates the color scheme based on the current theme provided by UIThemeManager.
     * This should be called when the application theme changes.
     *
     * @param parentFrame Optional reference to the main frame (can be null for initial setup).
     */
    public void applyThemeColors(MainFrame parentFrame) {
        // Use the singleton instance of the theme manager
        UIThemeManager themeManager = UIThemeManager.getInstance();

        // Get colors from the theme manager using defined keys
        indicatorColor = themeManager.getColor("primary");
        backgroundColor = themeManager.getColor("card");
        borderColor = themeManager.getColor("border");
        activeColor = themeManager.getColor("success");
        inactiveColor = themeManager.getColor("danger");
        statusTextColor = themeManager.getColor("text_secondary"); // Use secondary text color for status

        // Adjust indicator color for dark mode if needed for contrast
        if (themeManager.isDarkMode()) {
            // Example: Lighten the primary color slightly in dark mode for the dots
            indicatorColor = UIThemeManager.lighten(indicatorColor, 0.2f);
        }

        // Trigger a repaint to apply the new colors
        repaint();
    }

    /**
     * Overrides the paintComponent method to draw the custom look of the indicator panel.
     * This includes the rounded background, status indicator, text, and animated dots.
     *
     * @param g The Graphics object provided by the Swing painting system.
     */
    @Override
    protected void paintComponent(Graphics g) {
        // It's crucial to call super.paintComponent first, especially when non-opaque or extending standard components.
        // However, since we are drawing a custom background covering the whole area and setOpaque(false),
        // its primary role here is to potentially clean up from previous renders if needed.
        super.paintComponent(g);

        // Use Graphics2D for better rendering control (antialiasing, shapes, alpha)
        Graphics2D g2d = (Graphics2D) g.create(); // Create a copy to avoid modifying the original Graphics context

        try {
            // Enable antialiasing for smooth shapes and text
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON); // For text

            // 1. Draw Rounded Panel Background
            g2d.setColor(backgroundColor);
            // Create a rounded rectangle shape matching the panel size
            RoundRectangle2D roundedPanel = new RoundRectangle2D.Double(
                    0, 0, getWidth() - 1, getHeight() - 1, // Subtract 1 to keep border fully visible
                    PANEL_CORNER_RADIUS, PANEL_CORNER_RADIUS);
            g2d.fill(roundedPanel);

            // 2. Draw Panel Border
            g2d.setColor(borderColor);
            g2d.draw(roundedPanel); // Draw the outline of the rounded rectangle

            // 3. Draw Status Indicator (Small Circle)
            int centerX = getWidth() / 2;
            int statusIndicatorY = 20; // Y position for the status indicator circle
            int indicatorSize = 8; // Diameter of the status circle
            // Choose color based on the 'isActive' state
            g2d.setColor(isActive ? activeColor : inactiveColor);
            // Draw the filled circle centered horizontally
            g2d.fillOval(centerX - indicatorSize / 2, statusIndicatorY - indicatorSize / 2,
                    indicatorSize, indicatorSize);

            // 4. Draw Status Text ("ACTIVE" / "IDLE")
            g2d.setColor(statusTextColor); // Use themed text color
            g2d.setFont(UIThemeManager.getInstance().getFont("body_bold")); // Use themed bold font
            String statusText = isActive ? "ACTIVE" : "IDLE";
            FontMetrics fm = g2d.getFontMetrics();
            // Calculate text position to center it horizontally below the indicator
            int textWidth = fm.stringWidth(statusText);
            int textX = centerX - textWidth / 2;
            int textY = statusIndicatorY + 15; // Position text below the indicator circle
            g2d.drawString(statusText, textX, textY);

            // 5. Draw Animated Click Dots
            // Iterate through the queue of active dots
            for (ClickDot dot : dots) {
                // Calculate colors based on the dot's alpha for fade effect
                // Glow color (more transparent)
                Color glowColor = new Color(
                        indicatorColor.getRed(),
                        indicatorColor.getGreen(),
                        indicatorColor.getBlue(),
                        (int) (100 * dot.alpha) // Lower alpha for glow effect
                );
                // Main dot color (less transparent)
                Color dotColor = new Color(
                        indicatorColor.getRed(),
                        indicatorColor.getGreen(),
                        indicatorColor.getBlue(),
                        (int) (255 * dot.alpha) // Higher alpha for the main dot
                );

                // Draw outer glow (larger, more transparent circle)
                g2d.setColor(glowColor);
                float glowSize = DOT_SIZE * 1.5f; // Glow is slightly larger than the dot
                // Create an ellipse shape for the glow
                Ellipse2D.Float glow = new Ellipse2D.Float(
                        dot.x - glowSize / 2, // Center the glow around the dot's x
                        dot.y - glowSize / 2, // Center the glow around the dot's y
                        glowSize, glowSize
                );
                g2d.fill(glow); // Draw the filled glow

                // Draw main dot (smaller, less transparent circle)
                g2d.setColor(dotColor);
                // Create an ellipse shape for the main dot
                Ellipse2D.Float circle = new Ellipse2D.Float(
                        dot.x - DOT_SIZE / 2, // Center the dot's x
                        dot.y - DOT_SIZE / 2, // Center the dot's y
                        DOT_SIZE, DOT_SIZE
                );
                g2d.fill(circle); // Draw the filled main dot
            }

        } finally {
            // Dispose of the graphics copy to release system resources
            g2d.dispose();
        }
    }

    /**
     * Overrides setEnabled to also control the active state and clear dots when disabled.
     *
     * @param enabled true to enable the component, false to disable it.
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        // If the panel is disabled, force it to the inactive state and clear visual feedback
        if (!enabled) {
            setActive(false); // Set state to inactive
            dots.clear();     // Remove all existing dots
            repaint();        // Repaint to reflect the disabled state immediately
        }
        // Optionally, change appearance further when disabled (e.g., gray out colors)
        // This would typically be handled within paintComponent by checking isEnabled()
    }

    /**
     * Cleans up resources, specifically stopping the animation timer,
     * when the panel is no longer needed (e.g., window closing).
     */
    public void cleanup() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        dots.clear(); // Clear any remaining dots
    }
}