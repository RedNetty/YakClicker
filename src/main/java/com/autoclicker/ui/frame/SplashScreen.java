package com.autoclicker.ui.frame;

import com.autoclicker.config.AppConfig; // Assuming AppConfig holds APP_NAME and VERSION

import javax.imageio.ImageIO; // Required for image loading
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage; // Required for image handling
import java.io.IOException; // Required for exception handling
import java.io.InputStream; // Required for loading resource from classpath

/**
 * Splash screen displayed during application startup.
 * Uses an image icon instead of an emoji for better cross-platform compatibility.
 */
public class SplashScreen extends JWindow {
    private static final int WIDTH = 400;
    private static final int HEIGHT = 220; // Adjusted height slightly if needed for icon
    private static final int CORNER_RADIUS = 12;
    private static final String ICON_PATH = "/icons/tray_icon.png"; // Path within resources (leading slash important)
    private static final int ICON_SIZE = 104; // Desired display size for the icon

    private BufferedImage appIcon; // Field to store the loaded icon

    /**
     * Creates a new splash screen.
     */
    public SplashScreen() {
        // Load the icon image during initialization
        loadIcon();

        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null); // Center on screen
        setLayout(new BorderLayout());
        setAlwaysOnTop(true); // Splash screens are usually always on top

        // Content panel for custom painting
        JPanel content = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g); // Let the UI delegate paint first
                Graphics2D g2d = (Graphics2D) g.create(); // Create a copy to avoid modifying original Graphics context

                try {
                    // Enable anti-aliasing for smoother graphics
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR); // For smoother image scaling

                    // Background gradient
                    GradientPaint gradient = new GradientPaint(
                            0, 0, new Color(31, 41, 55), // Dark gray-blue top
                            0, getHeight(), new Color(17, 24, 39)); // Darker gray-blue bottom
                    g2d.setPaint(gradient);
                    g2d.fillRect(0, 0, getWidth(), getHeight());

                    // --- Draw Logo/Icon ---
                    int centerX = getWidth() / 2;
                    int iconY = 30; // Position icon closer to the top
                    if (appIcon != null) {
                        int iconX = centerX - ICON_SIZE / 2;
                        // Draw the loaded image, scaling it to ICON_SIZE
                        g2d.drawImage(appIcon, iconX, iconY, ICON_SIZE, ICON_SIZE, this);
                    } else {
                        // Fallback: Draw a placeholder if icon failed to load
                        g2d.setColor(Color.RED);
                        g2d.fillRect(centerX - ICON_SIZE / 2, iconY, ICON_SIZE, ICON_SIZE);
                        g2d.setColor(Color.WHITE);
                        g2d.drawString("!", centerX - 5, iconY + ICON_SIZE / 2 + 5); // Simple placeholder
                        System.err.println("Splash Screen: App icon failed to load, drawing placeholder.");
                    }

                    // --- Draw App Name ---
                    int textY = iconY + ICON_SIZE + 35; // Position text below the icon
                    g2d.setColor(Color.WHITE);
                    Font nameFont = new Font("Arial", Font.BOLD, 28);
                    g2d.setFont(nameFont);
                    FontMetrics fm = g2d.getFontMetrics();
                    String name = AppConfig.APP_NAME; // Get name from config
                    g2d.drawString(name, centerX - fm.stringWidth(name) / 2, textY);

                    // --- Draw Version ---
                    textY += 25; // Position version below the name
                    g2d.setColor(new Color(156, 163, 175)); // Light gray color
                    Font versionFont = new Font("Arial", Font.PLAIN, 14);
                    g2d.setFont(versionFont);
                    fm = g2d.getFontMetrics();
                    String version = "Version " + AppConfig.VERSION; // Get version from config
                    g2d.drawString(version, centerX - fm.stringWidth(version) / 2, textY);

                } finally {
                    g2d.dispose(); // Dispose of the graphics copy
                }
            }
        };
        content.setOpaque(true); // Ensure the panel paints its background

        // Progress bar
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true); // Use indeterminate mode for loading feedback
        progressBar.setForeground(new Color(79, 70, 229)); // Indigo color
        progressBar.setBackground(new Color(31, 41, 55)); // Match background gradient start
        progressBar.setBorderPainted(false); // No border for a flatter look
        progressBar.setPreferredSize(new Dimension(WIDTH, 6)); // Slightly thicker bar

        // Add components to the JWindow
        add(content, BorderLayout.CENTER);
        add(progressBar, BorderLayout.SOUTH);

        // Apply rounded corners to the window
        // Ensure this is called after the window is potentially realized or sized
        // Using ComponentListener to be safer, although often works directly
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), CORNER_RADIUS, CORNER_RADIUS));
            }
        });
        // Set initial shape in case resize event doesn't fire immediately
        setShape(new RoundRectangle2D.Double(0, 0, WIDTH, HEIGHT, CORNER_RADIUS, CORNER_RADIUS));
    }

    /**
     * Loads the application icon from the classpath resources.
     * Handles potential errors during loading.
     */
    private void loadIcon() {
        try {
            // Load the image resource from the classpath
            InputStream iconStream = SplashScreen.class.getResourceAsStream(ICON_PATH);
            if (iconStream == null) {
                throw new IOException("Cannot find resource: " + ICON_PATH);
            }
            // Read the image data
            appIcon = ImageIO.read(iconStream);
            iconStream.close(); // Close the stream after reading
            System.out.println("Splash Screen: Icon loaded successfully from " + ICON_PATH);
        } catch (IOException e) {
            // Log an error if the image cannot be loaded
            System.err.println("Failed to load splash screen icon: " + ICON_PATH);
            e.printStackTrace(); // Print stack trace for debugging
            appIcon = null; // Ensure appIcon is null if loading failed
        } catch (Exception e) {
            // Catch any other unexpected errors during loading
            System.err.println("An unexpected error occurred while loading the splash screen icon: " + e.getMessage());
            e.printStackTrace();
            appIcon = null;
        }
    }
}
