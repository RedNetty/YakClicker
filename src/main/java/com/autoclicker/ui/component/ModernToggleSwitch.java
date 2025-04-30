package com.autoclicker.ui.component;

import com.autoclicker.ui.theme.UIThemeManager;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * A modern toggle switch component that provides a sleek alternative to checkboxes.
 * Features smooth animation and customizable colors.
 */
public class ModernToggleSwitch extends JPanel {
    // Visual properties
    private boolean selected = false;
    private boolean animated = true;
    private float position = 0.0f;
    private int animationDuration = 150; // milliseconds
    private long animationStartTime;
    private Timer animationTimer;

    // UI configuration
    private static final int DEFAULT_WIDTH = 40;
    private static final int DEFAULT_HEIGHT = 20;
    private static final int TRACK_HEIGHT = 20;
    private static final int THUMB_SIZE = 16;
    private static final int THUMB_MARGIN = 2;

    // Colors
    private Color trackColorOn;
    private Color trackColorOff;
    private Color thumbColorOn;
    private Color thumbColorOff;

    // Label
    private String text;
    private Font font;
    private Color textColor;

    // Listeners
    private final List<ChangeListener> changeListeners = new ArrayList<>();

    /**
     * Creates a new toggle switch with default settings.
     */
    public ModernToggleSwitch() {
        this(null);
    }

    /**
     * Creates a new toggle switch with the specified label text.
     *
     * @param text The label text
     */
    public ModernToggleSwitch(String text) {
        this.text = text;

        // Use BorderLayout to properly position the toggle and text
        setLayout(new BorderLayout(8, 0));

        // Set initial colors from UIThemeManager
        UIThemeManager themeManager = UIThemeManager.getInstance();
        trackColorOn = themeManager.getColor("primary");
        trackColorOff = themeManager.isDarkMode() ?
                new Color(55, 65, 81) : // Dark gray for dark mode
                new Color(226, 232, 240); // Light gray for light mode
        thumbColorOn = Color.WHITE;
        thumbColorOff = Color.WHITE;
        textColor = themeManager.getColor("text_primary");
        font = themeManager.getFont("body_regular");

        // Create toggle panel
        JPanel togglePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintToggle(g);
            }
        };
        togglePanel.setOpaque(false);
        togglePanel.setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
        togglePanel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Add label if provided
        if (text != null && !text.isEmpty()) {
            JLabel label = new JLabel(text);
            label.setFont(font);
            label.setForeground(textColor);
            add(label, BorderLayout.CENTER);
        }

        // Add toggle panel to the left
        add(togglePanel, BorderLayout.WEST);

        // Make component focusable for keyboard navigation
        setFocusable(true);

        // Initialize animation timer
        animationTimer = new Timer(16, e -> {
            long elapsed = System.currentTimeMillis() - animationStartTime;
            float progress = Math.min(1.0f, (float) elapsed / animationDuration);

            // Apply easing function for smoother animation
            float easedProgress = easeInOut(progress);

            if (selected) {
                position = easedProgress;
            } else {
                position = 1.0f - easedProgress;
            }

            if (progress >= 1.0f) {
                animationTimer.stop();
                position = selected ? 1.0f : 0.0f;
            }

            repaint();
        });

        // Add mouse and key listeners
        togglePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggle();
            }
        });

        // Add key listener for accessibility (space to toggle)
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    toggle();
                }
            }
        });

        // Set background transparent
        setOpaque(false);
    }

    /**
     * Paints the toggle switch.
     */
    private void paintToggle(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = DEFAULT_WIDTH;
        int height = TRACK_HEIGHT;

        // Draw track (background)
        Color trackColor = interpolateColor(trackColorOff, trackColorOn, position);
        g2d.setColor(trackColor);

        RoundRectangle2D track = new RoundRectangle2D.Float(
                0, 0, width, height, height, height);
        g2d.fill(track);

        // Draw thumb (knob)
        Color thumbColor = interpolateColor(thumbColorOff, thumbColorOn, position);
        g2d.setColor(thumbColor);

        int thumbX = Math.round(THUMB_MARGIN + position * (width - THUMB_SIZE - 2 * THUMB_MARGIN));
        int thumbY = (height - THUMB_SIZE) / 2;
        g2d.fillOval(thumbX, thumbY, THUMB_SIZE, THUMB_SIZE);

        g2d.dispose();
    }

    /**
     * Applies easing function to create smoother animation.
     */
    private float easeInOut(float t) {
        return t < 0.5f ? 2 * t * t : 1 - (float)Math.pow(-2 * t + 2, 2) / 2;
    }

    /**
     * Gets the selected state.
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Sets the selected state.
     */
    public void setSelected(boolean selected) {
        if (this.selected != selected) {
            this.selected = selected;

            // Start animation
            if (animated) {
                startAnimation();
            } else {
                position = selected ? 1.0f : 0.0f;
                repaint();
            }

            // Notify listeners
            fireStateChanged();
        }
    }

    /**
     * Toggles the selected state.
     */
    public void toggle() {
        setSelected(!selected);
    }

    /**
     * Gets the label text.
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the label text.
     */
    public void setText(String text) {
        this.text = text;
        removeAll();

        // Create toggle panel
        JPanel togglePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintToggle(g);
            }
        };
        togglePanel.setOpaque(false);
        togglePanel.setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
        togglePanel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Add mouse listener to toggle panel
        togglePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggle();
            }
        });

        // Add label if text is provided
        if (text != null && !text.isEmpty()) {
            JLabel label = new JLabel(text);
            label.setFont(font);
            label.setForeground(textColor);
            add(label, BorderLayout.CENTER);
        }

        // Add toggle panel to the left
        add(togglePanel, BorderLayout.WEST);

        revalidate();
        repaint();
    }

    /**
     * Sets whether animation is enabled.
     */
    public void setAnimated(boolean animated) {
        this.animated = animated;
    }

    /**
     * Sets the animation duration.
     */
    public void setAnimationDuration(int duration) {
        this.animationDuration = duration;
    }

    /**
     * Sets the colors for the toggle switch.
     */
    public void setColors(Color trackOn, Color trackOff, Color thumbOn, Color thumbOff) {
        this.trackColorOn = trackOn;
        this.trackColorOff = trackOff;
        this.thumbColorOn = thumbOn;
        this.thumbColorOff = thumbOff;
        repaint();
    }

    /**
     * Updates colors based on the current theme.
     */
    public void updateThemeColors() {
        UIThemeManager themeManager = UIThemeManager.getInstance();
        trackColorOn = themeManager.getColor("primary");
        trackColorOff = themeManager.isDarkMode() ?
                new Color(55, 65, 81) : // Dark gray for dark mode
                new Color(226, 232, 240); // Light gray for light mode
        textColor = themeManager.getColor("text_primary");

        // Update label if present
        Component[] components = getComponents();
        for (Component component : components) {
            if (component instanceof JLabel) {
                JLabel label = (JLabel) component;
                label.setForeground(textColor);
                label.setFont(themeManager.getFont("body_regular"));
            }
        }

        repaint();
    }

    /**
     * Starts the animation for toggling.
     */
    private void startAnimation() {
        if (animationTimer.isRunning()) {
            animationTimer.stop();
        }

        animationStartTime = System.currentTimeMillis();
        animationTimer.start();
    }

    /**
     * Interpolates between two colors.
     */
    private Color interpolateColor(Color c1, Color c2, float alpha) {
        float beta = 1 - alpha;
        int r = Math.round(beta * c1.getRed() + alpha * c2.getRed());
        int g = Math.round(beta * c1.getGreen() + alpha * c2.getGreen());
        int b = Math.round(beta * c1.getBlue() + alpha * c2.getBlue());
        int a = Math.round(beta * c1.getAlpha() + alpha * c2.getAlpha());
        return new Color(r, g, b, a);
    }

    /**
     * Adds a change listener.
     */
    public void addChangeListener(ChangeListener listener) {
        changeListeners.add(listener);
    }

    /**
     * Removes a change listener.
     */
    public void removeChangeListener(ChangeListener listener) {
        changeListeners.remove(listener);
    }

    /**
     * Notifies all listeners that the state has changed.
     */
    protected void fireStateChanged() {
        ChangeEvent event = new ChangeEvent(this);
        for (ChangeListener listener : changeListeners) {
            listener.stateChanged(event);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        // If there's no text, just return the toggle size
        if (text == null || text.isEmpty()) {
            return new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        }

        // Otherwise, add space for the text
        FontMetrics fm = getFontMetrics(font);
        int textWidth = fm.stringWidth(text) + 8; // 8px padding after text

        return new Dimension(DEFAULT_WIDTH + textWidth, Math.max(DEFAULT_HEIGHT, fm.getHeight()));
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }
}