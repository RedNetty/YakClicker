package com.autoclicker.ui.component;

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
 */
public class ClickIndicator extends JPanel {
    private static final int MAX_DOTS = 20;
    private static final int DOT_SIZE = 6;
    private static final int DOT_LIFE_MS = 400;
    private static final int PANEL_CORNER_RADIUS = 12;

    private boolean isActive = false;
    private final Queue<ClickDot> dots = new LinkedList<>();
    private final Timer animationTimer;
    private final Random random = new Random();

    // Theme colors
    private Color indicatorColor;
    private Color backgroundColor;
    private Color borderColor;
    private Color activeColor;
    private Color inactiveColor;

    /**
     * Represents a single dot in the animation.
     */
    private static class ClickDot {
        float x, y;
        float alpha = 1.0f;
        long createdTime;

        ClickDot(float x, float y) {
            this.x = x;
            this.y = y;
            this.createdTime = System.currentTimeMillis();
        }

        /**
         * Updates the dot's alpha based on its age.
         */
        void update() {
            long age = System.currentTimeMillis() - createdTime;
            alpha = 1.0f - (float) age / DOT_LIFE_MS;
            if (alpha < 0) alpha = 0;
        }

        /**
         * Checks if the dot should be removed.
         */
        boolean isDead() {
            return alpha <= 0;
        }
    }

    /**
     * Creates a new click indicator panel.
     */
    public ClickIndicator() {
        setPreferredSize(new Dimension(50, 120));
        setMinimumSize(new Dimension(30, 80));

        // Initialize with default colors
        UIThemeManager themeManager = UIThemeManager.getInstance();
        indicatorColor = themeManager.getColor("primary");
        backgroundColor = themeManager.getColor("card");
        borderColor = themeManager.getColor("border");
        activeColor = themeManager.getColor("success");
        inactiveColor = themeManager.getColor("danger");

        // Animation timer
        animationTimer = new Timer(16, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Update existing dots
                dots.removeIf(dot -> {
                    dot.update();
                    return dot.isDead();
                });

                // Add new dots if active
                if (isActive && dots.size() < MAX_DOTS && random.nextFloat() < 0.3f) {
                    addClickDot();
                }

                repaint();
            }
        });
        animationTimer.start();

        // Set panel to non-opaque for proper background painting
        setOpaque(false);
    }

    /**
     * Sets the active state of the indicator.
     */
    public void setActive(boolean active) {
        if (this.isActive != active) {
            this.isActive = active;
            if (active) {
                addClickDot(); // Add initial dot when activated
            }
        }
    }

    /**
     * Adds a single click dot to indicate a click event.
     */
    public void addClickDot() {
        if (dots.size() < MAX_DOTS) {
            float x = (float) (getWidth() * 0.5 + (random.nextFloat() - 0.5) * getWidth() * 0.5);
            float y = (float) (getHeight() * 0.5 + (random.nextFloat() - 0.5) * getHeight() * 0.5);
            dots.add(new ClickDot(x, y));
        }
    }

    /**
     * Updates the color scheme based on the current theme.
     */
    public void applyThemeColors() {
        UIThemeManager themeManager = UIThemeManager.getInstance();
        indicatorColor = themeManager.getColor("primary");
        backgroundColor = themeManager.getColor("card");
        borderColor = themeManager.getColor("border");
        activeColor = themeManager.getColor("success");
        inactiveColor = themeManager.getColor("danger");

        // Set darker indicator color for better contrast in dark mode
        if (themeManager.isDarkMode()) {
            indicatorColor = UIThemeManager.lighten(indicatorColor, 0.2f);
        }

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw rounded panel background
            g2d.setColor(backgroundColor);
            RoundRectangle2D roundedPanel = new RoundRectangle2D.Double(
                    0, 0, getWidth(), getHeight(), PANEL_CORNER_RADIUS, PANEL_CORNER_RADIUS);
            g2d.fill(roundedPanel);

            // Draw border
            g2d.setColor(borderColor);
            g2d.draw(roundedPanel);

            // Draw status indicator
            int centerX = getWidth() / 2;
            int centerY = 20;
            int indicatorSize = 8;

            g2d.setColor(isActive ? activeColor : inactiveColor);
            g2d.fillOval(centerX - indicatorSize/2, centerY - indicatorSize/2,
                    indicatorSize, indicatorSize);

            // Draw status text
            g2d.setFont(UIThemeManager.getInstance().getFont("body_bold"));
            String statusText = isActive ? "ACTIVE" : "IDLE";
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(statusText, centerX - fm.stringWidth(statusText) / 2, centerY + 15);

            // Draw click dots with glow effect
            for (ClickDot dot : dots) {
                // Draw outer glow
                Color glowColor = new Color(
                        indicatorColor.getRed(),
                        indicatorColor.getGreen(),
                        indicatorColor.getBlue(),
                        (int) (100 * dot.alpha)
                );
                g2d.setColor(glowColor);
                float glowSize = DOT_SIZE * 1.5f;
                Ellipse2D.Float glow = new Ellipse2D.Float(
                        dot.x - glowSize/2,
                        dot.y - glowSize/2,
                        glowSize,
                        glowSize
                );
                g2d.fill(glow);

                // Draw main dot
                Color dotColor = new Color(
                        indicatorColor.getRed(),
                        indicatorColor.getGreen(),
                        indicatorColor.getBlue(),
                        (int) (255 * dot.alpha)
                );
                g2d.setColor(dotColor);

                Ellipse2D.Float circle = new Ellipse2D.Float(
                        dot.x - DOT_SIZE/2,
                        dot.y - DOT_SIZE/2,
                        DOT_SIZE,
                        DOT_SIZE
                );
                g2d.fill(circle);
            }

        } finally {
            g2d.dispose();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) {
            isActive = false;
            dots.clear();
        }
    }

    /**
     * Cleans up resources when the panel is no longer needed.
     */
    public void cleanup() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
    }
}