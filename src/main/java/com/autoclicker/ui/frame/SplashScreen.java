package com.autoclicker.ui.frame;

import com.autoclicker.config.AppConfig;

import javax.swing.*;
import java.awt.*;

/**
 * Splash screen displayed during application startup.
 */
public class SplashScreen extends JWindow {
    private static final int WIDTH = 400;
    private static final int HEIGHT = 220;

    /**
     * Creates a new splash screen.
     */
    public SplashScreen() {
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Content panel
        JPanel content = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;

                // Enable anti-aliasing
                g2d.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                // Background gradient
                GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(31, 41, 55),
                        0, getHeight(), new Color(17, 24, 39));
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // Draw logo
                int centerX = getWidth() / 2;
                int centerY = getHeight() / 2 - 20;

                // Draw lightning symbol
                g2d.setColor(new Color(79, 70, 229));
                Font symbolFont = new Font("Arial", Font.BOLD, 60);
                g2d.setFont(symbolFont);
                FontMetrics fm = g2d.getFontMetrics();
                String symbol = "⚡";
                g2d.drawString(symbol, centerX - fm.stringWidth(symbol) / 2, centerY);

                // Draw app name
                g2d.setColor(Color.WHITE);
                Font nameFont = new Font("Arial", Font.BOLD, 28);
                g2d.setFont(nameFont);
                fm = g2d.getFontMetrics();
                String name = AppConfig.APP_NAME;
                g2d.drawString(name, centerX - fm.stringWidth(name) / 2, centerY + 50);

                // Draw version
                g2d.setColor(new Color(156, 163, 175));
                Font versionFont = new Font("Arial", Font.PLAIN, 14);
                g2d.setFont(versionFont);
                fm = g2d.getFontMetrics();
                String version = "Version " + AppConfig.VERSION;
                g2d.drawString(version, centerX - fm.stringWidth(version) / 2, centerY + 80);
            }
        };

        // Progress bar
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setForeground(new Color(79, 70, 229));
        progressBar.setBorderPainted(false);
        progressBar.setBackground(new Color(31, 41, 55));
        progressBar.setPreferredSize(new Dimension(WIDTH, 5));

        add(content, BorderLayout.CENTER);
        add(progressBar, BorderLayout.SOUTH);

        // Set window shape
        setShape(new java.awt.geom.RoundRectangle2D.Double(0, 0, WIDTH, HEIGHT, 12, 12));
    }
}