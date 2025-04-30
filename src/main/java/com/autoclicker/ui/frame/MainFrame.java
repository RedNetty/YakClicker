package com.autoclicker.ui.frame;


import com.autoclicker.service.click.AbstractClickService;
import com.autoclicker.service.click.AutoClickerService;
import com.autoclicker.service.keybind.KeybindManager;
import com.autoclicker.service.pattern.PatternRecorderService;
import com.autoclicker.storage.SettingsManager;
import com.autoclicker.ui.panel.*;
import com.autoclicker.ui.theme.UIThemeManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
// No longer need java.net.URL, java.awt.image.BufferedImage, java.util.List, java.util.ArrayList
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The main window frame for the YakClicker application.
 * Features custom styling, animations, and integrates various panels.
 * Enhanced with modern UI components, performance metrics, and pattern recording.
 *
 * Fixed potential flashing issue with ClickIndicatorPanel by making its parent opaque.
 * Reverted to using standard text characters (X, -, D/L, *) for title bar controls
 * for better cross-platform font compatibility.
 */
public class MainFrame extends JFrame {
    private final SettingsManager settingsManager;
    private final AutoClickerService clickerService;
    private final PatternRecorderService patternRecorderService;
    private final KeybindManager keybindManager;
    private final ControlPanel controlPanel;

    // UI Components
    private JPanel statusBar;
    private JLabel statusLabel;
    private JLabel cpsLabel;
    private JPanel titleBar;
    private JPanel bottomPanel;
    private JTabbedPane tabbedPane;
    private ClickIndicatorPanel clickIndicator;
    private QuickActionBar quickActionBar;
    private StatisticsPanel statisticsPanel;
    private JPanel rightPanel; // Added field reference for theme updates
    // Removed icon-related fields (appIconLabel, themeButton references are still okay if needed elsewhere, but icons themselves are gone)
    // private JLabel appIconLabel; // Can remove if only used for icon
    private JButton themeButton; // Keep reference for updating text

    // Timers
    private Timer statusUpdateTimer;
    private Timer cpsUpdateTimer;
    private Timer animationTimer;
    private ScheduledExecutorService cursorTracker;

    // Animation properties
    private static final int ANIMATION_DELAY_MS = 5;
    private static final int ANIMATION_STEP_DIVIDER = 8;
    private static final int SLIDE_IN_TARGET_Y_OFFSET = 50;
    private int targetY;
    private int currentY;
    private boolean isAnimating = false;

    // Current theme state
    private boolean isDarkMode = true;

    // Auto-hide properties
    private static final int AUTO_HIDE_SENSITIVITY = 5;
    private static final int CURSOR_TRACK_INTERVAL_MS = 200;
    private boolean isAutoHideEnabled = false;

    // Resize Handle properties
    private static final int RESIZE_HANDLE_SIZE = 15;

    /**
     * Creates a new MainFrame.
     */
    public MainFrame(AutoClickerService clickerService, SettingsManager settingsManager, PatternRecorderService patternRecorderService) {
        this.clickerService = clickerService;
        this.settingsManager = settingsManager;
        this.patternRecorderService = patternRecorderService;

        // Load theme preference from settings
        this.isDarkMode = settingsManager.isDarkMode();

        // Initialize UIThemeManager with current theme
        UIThemeManager.getInstance().setDarkMode(isDarkMode);

        // --- Icon loading removed ---

        // Create keybind manager
        this.keybindManager = new KeybindManager(settingsManager, this, clickerService);

        setTitle("YakClicker");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Main handles shutdown hook for cleanup
        setSize(800, 600); // Adjusted default size
        setMinimumSize(new Dimension(700, 500)); // Set minimum size
        setLocationRelativeTo(null);
        setUndecorated(true); // Remove window borders for modern look

        // Make window background transparent for custom rounded shape painting
        setBackground(new Color(0, 0, 0, 0));

        // Set window always on top based on settings
        setAlwaysOnTop(settingsManager.isAlwaysOnTop());

        // Set initial window transparency
        setWindowTransparency(settingsManager.getTransparency());

        // Set custom rounded window shape (call after size is set)
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(),
                        UIThemeManager.CORNER_RADIUS_LARGE, UIThemeManager.CORNER_RADIUS_LARGE));
            }
        });
        // Initial shape setting
        setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(),
                UIThemeManager.CORNER_RADIUS_LARGE, UIThemeManager.CORNER_RADIUS_LARGE));

        // Create main content panel (ControlPanel)
        controlPanel = new ControlPanel(clickerService, settingsManager, this);

        // Create click indicator
        clickIndicator = new ClickIndicatorPanel();
        // Theme applied later in applyTheme()

        // Create UI Elements
        titleBar = createTitleBar(); // Title bar uses text icons again
        statusBar = createStatusBar();
        JPanel resizeHandle = createResizeHandle();

        // Register click listener to get notified of each click
        clickerService.addClickListener(new AbstractClickService.ClickListener() {
            @Override
            public void onClickPerformed() {
                if (clickIndicator != null) {
                    SwingUtilities.invokeLater(() -> clickIndicator.addClickDot());
                }
            }
        });

        // Create bottom panel to hold status bar and resize handle
        bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false); // Bottom panel itself can be transparent
        bottomPanel.add(statusBar, BorderLayout.CENTER);
        bottomPanel.add(resizeHandle, BorderLayout.EAST);

        // Create tabbed pane for content
        tabbedPane = new JTabbedPane();
        tabbedPane.setFocusable(false);

        // Create quick action bar
        quickActionBar = new QuickActionBar(clickerService, settingsManager, this);

        // Create statistics panel
        statisticsPanel = new StatisticsPanel(settingsManager, clickerService, this);

        // Add tabs
        tabbedPane.addTab("Settings", null, controlPanel, "Configure auto-clicker settings");
        tabbedPane.addTab("Statistics", null, statisticsPanel, "View performance statistics");
        if (patternRecorderService != null) {
            PatternRecorderPanel patternRecorderPanel = new PatternRecorderPanel(patternRecorderService, this);
            tabbedPane.addTab("Patterns", null, patternRecorderPanel, "Record and play click patterns");
        }

        // Create main content layout
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false); // Main content area transparent to show frame background

        // Add the click indicator to the right of the content panel
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false); // Center panel transparent
        centerPanel.add(tabbedPane, BorderLayout.CENTER);

        // *** Create the right panel for the click indicator ***
        rightPanel = new JPanel(new BorderLayout());
        // *** Make the rightPanel OPAQUE to provide a stable background ***
        rightPanel.setOpaque(true);
        rightPanel.add(clickIndicator, BorderLayout.CENTER);
        // Add padding around the indicator
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 10));
        // Background color will be set in applyTheme()

        contentPanel.add(quickActionBar, BorderLayout.NORTH);
        contentPanel.add(centerPanel, BorderLayout.CENTER);
        contentPanel.add(rightPanel, BorderLayout.EAST);

        // Set up main layout using content pane
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(titleBar, BorderLayout.NORTH);
        contentPane.add(contentPanel, BorderLayout.CENTER);
        contentPane.add(bottomPanel, BorderLayout.SOUTH);

        // Apply theme initially to all components
        applyTheme();

        // Start timers
        startStatusUpdateTimer();
        startCpsUpdateTimer();

        // Initialize animation coordinates (start offscreen top)
        currentY = -getHeight();
        setLocation(getX(), currentY); // Set initial offscreen position

        // Add window movement listeners for dragging (attach to title bar)
        setupWindowDragListeners(titleBar);

        // Setup cursor tracking for auto-show/hide
        setupCursorTracking();
        setAutoHide(settingsManager.isAutoHide()); // Apply initial auto-hide setting

        // Handle window events
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Settings are saved via shutdown hook in Main or exit action
            }

            @Override
            public void windowIconified(WindowEvent e) {
                // Minimize to tray behavior (handled by system tray setup if enabled)
                if (settingsManager.isMinimizeToTray() && SystemTray.isSupported()) {
                    setVisible(false); // Hide frame when minimized if tray is used
                }
            }

            @Override
            public void windowOpened(WindowEvent e) {
                // Start slide-in animation when window is first opened
                slideIn();
            }
        });
    }

    // --- Icon loading methods removed ---
    // loadIcons(), loadImageIcon(), createPlaceholderIcon(), showIconLoadWarning()

    /**
     * Toggles the application theme between light and dark mode.
     */
    public void toggleTheme() {
        isDarkMode = !isDarkMode;
        settingsManager.setDarkMode(isDarkMode);

        // Update the theme manager
        UIThemeManager.getInstance().setDarkMode(isDarkMode);

        applyTheme(); // Apply theme to the entire frame and its children
    }

    /**
     * Applies the current theme (colors, fonts) to the frame and its components.
     */
    private void applyTheme() {
        UIThemeManager themeManager = UIThemeManager.getInstance();

        // Get theme colors
        Color frameBg = themeManager.getColor("surface"); // Background for title, status, bottom
        Color contentBg = themeManager.getColor("background"); // Main content area background
        Color borderCol = themeManager.getColor("border");
        Color titleTextCol = themeManager.getColor("text_primary");
        Color statusTextCol = themeManager.getColor("text_secondary");
        Color primaryCol = themeManager.getColor("primary"); // For app icon

        // Apply to root pane and content pane for overall background
        getRootPane().setBackground(frameBg); // Frame background (visible due to rounded corners)
        getContentPane().setBackground(contentBg); // Background for the main content area

        // Apply to specific panels
        if (titleBar != null) {
            titleBar.setBackground(frameBg);
            titleBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, borderCol));
            // Update title label color and app icon label color
            Component titlePanelComp = titleBar.getComponent(0); // Assuming titlePanel is first
            if (titlePanelComp instanceof JPanel) {
                JPanel titlePanel = (JPanel) titlePanelComp;
                for(Component c : titlePanel.getComponents()) {
                    if (c instanceof JLabel) {
                        JLabel label = (JLabel) c;
                        // Check if it's the app icon label (based on text maybe?)
                        if ("*".equals(label.getText())) { // Assuming "*" is the app icon text
                            label.setForeground(primaryCol); // Use primary color for app icon
                            label.setFont(new Font("Arial", Font.BOLD, 18)); // Make icon slightly larger/bolder
                        } else { // It's the title text label
                            label.setForeground(titleTextCol);
                            label.setFont(themeManager.getFont("heading_medium"));
                        }
                    }
                }
            }
        }

        if (statusBar != null) {
            statusBar.setBackground(frameBg);
            // Reset border before applying new one to avoid compounding
            statusBar.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, borderCol),
                    BorderFactory.createEmptyBorder(5, 12, 5, 12)
            ));
        }

        if (bottomPanel != null) {
            // Keep bottom panel transparent or set to frameBg if needed
            bottomPanel.setOpaque(false);
            // Ensure resize handle stays visible
            Component resizeHandle = bottomPanel.getComponent(1); // Assuming resize handle is second
            if (resizeHandle != null) resizeHandle.repaint();
        }

        // Apply to status labels
        if (statusLabel != null) {
            statusLabel.setForeground(statusTextCol);
            statusLabel.setFont(themeManager.getFont("body_regular"));
        }

        if (cpsLabel != null) {
            cpsLabel.setForeground(statusTextCol);
            cpsLabel.setFont(themeManager.getFont("body_regular"));
        }

        // Apply theme to ControlPanel and its children
        if (controlPanel != null) {
            controlPanel.setBackground(contentBg); // Control panel uses content background
            controlPanel.applyThemeColors();
        }

        // Apply theme to statistics panel
        if (statisticsPanel != null) {
            statisticsPanel.applyThemeColors();
        }

        // Apply theme to quick action bar
        if (quickActionBar != null) {
            quickActionBar.applyThemeColors();
        }

        // *** Apply theme to the right panel containing the click indicator ***
        if (rightPanel != null) {
            rightPanel.setBackground(contentBg); // Set background to match content area
            rightPanel.setOpaque(true); // Ensure it's opaque
        }

        // Apply theme to click indicator (needs parent frame reference)
        if (clickIndicator != null) {
            // clickIndicator itself remains non-opaque for its rounded corners
            clickIndicator.applyThemeColors(this);
        }


        // Apply theme to tabbed pane
        if (tabbedPane != null) {
            tabbedPane.setBackground(contentBg); // Tabbed pane uses content background
            themeManager.styleTabbedPane(tabbedPane);
            tabbedPane.repaint();
        }

        // Update title bar buttons (styling and theme text)
        if (titleBar != null && titleBar.getComponentCount() > 1) {
            updateTitleBarButtons();
        }

        // Update window shape for rounded corners
        setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(),
                UIThemeManager.CORNER_RADIUS_LARGE, UIThemeManager.CORNER_RADIUS_LARGE));

        // Trigger repaint and revalidation
        revalidate();
        repaint();
    }

    /**
     * Updates the title bar buttons with current theme styling and correct theme text.
     */
    private void updateTitleBarButtons() {
        Component buttonPanelComp = titleBar.getComponent(1); // Assuming button panel is second
        if (buttonPanelComp instanceof JPanel) {
            JPanel buttonPanel = (JPanel) buttonPanelComp;
            for (Component c : buttonPanel.getComponents()) {
                if (c instanceof JButton) {
                    JButton button = (JButton) c;
                    String actionCommand = button.getActionCommand(); // Use action command

                    if ("toggle_theme".equals(actionCommand)) {
                        // Update theme text based on current theme
                        button.setText(isDarkMode ? "L" : "D"); // L for Light, D for Dark
                        button.setToolTipText(isDarkMode ? "Switch to Light Theme" : "Switch to Dark Theme");
                        styleWindowControlButton(button, "theme"); // Re-apply base styling
                    } else if ("minimize".equals(actionCommand)) {
                        button.setText("-"); // Ensure text is set
                        styleWindowControlButton(button, "minimize");
                    } else if ("close".equals(actionCommand)) {
                        button.setText("X"); // Ensure text is set
                        styleWindowControlButton(button, "close");
                    }
                }
            }
        }
    }

    /**
     * Applies styling to window control buttons (using text).
     */
    private void styleWindowControlButton(JButton button, String type) {
        UIThemeManager themeManager = UIThemeManager.getInstance();

        // Remove existing mouse listeners to prevent duplicates
        for (MouseListener ml : button.getMouseListeners()) {
            if (ml.getClass().isAnonymousClass() || ml.getClass().getEnclosingClass() == MainFrame.class) {
                button.removeMouseListener(ml);
            }
        }

        // Base styling for text buttons
        button.setForeground(themeManager.getColor("text_secondary")); // Default text color
        button.setFont(new Font("Arial", Font.BOLD, 14)); // Use a common font, bold
        button.setBackground(null); // Start transparent
        button.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12)); // Adjust padding for text
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false); // Button itself is not opaque
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Button-specific styling and effects
        Color hoverBg;
        Color hoverFg = button.getForeground(); // Default foreground

        if (type.equals("close")) {
            // Close button has danger color on hover
            hoverBg = themeManager.getColor("danger");
            hoverFg = Color.WHITE; // Text becomes white on hover
        } else {
            // Other buttons have subtle hover effect
            hoverBg = themeManager.isDarkMode() ?
                    themeManager.getColor("border") :
                    UIThemeManager.darken(themeManager.getColor("surface"), 0.05f);
            // Keep original foreground color on hover for minimize/theme
            hoverFg = themeManager.getColor("text_secondary");
        }

        // Use final variables for listener
        final Color finalHoverBg = hoverBg;
        final Color finalHoverFg = hoverFg;
        final Color originalFg = themeManager.getColor("text_secondary");


        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(finalHoverBg);
                button.setForeground(finalHoverFg); // Set foreground on hover
                button.setContentAreaFilled(true);
                button.setOpaque(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setContentAreaFilled(false);
                button.setOpaque(false);
                button.setForeground(originalFg); // Reset foreground color
            }
        });
    }

    // --- UI Element Creation ---

    private JPanel createTitleBar() {
        UIThemeManager themeManager = UIThemeManager.getInstance();

        JPanel titleBarPanel = new JPanel();
        titleBarPanel.setLayout(new BorderLayout());
        // Background and border set in applyTheme()

        // App title section (West)
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titlePanel.setOpaque(false); // Title panel transparent

        // --- Use text character for the app icon ---
        JLabel appIconLabel = new JLabel("*"); // Simple asterisk as icon
        // Styling (color, font) applied in applyTheme()
        appIconLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        JLabel titleLabel = new JLabel("YakClicker", JLabel.LEFT);
        // Font and color set in applyTheme()
        titleLabel.setBorder(BorderFactory.createEmptyBorder(8, 5, 8, 10)); // Padding

        titlePanel.add(appIconLabel); // Add the text icon label
        titlePanel.add(titleLabel);

        // Control buttons section (East)
        JPanel controlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        controlButtons.setOpaque(false); // Buttons panel transparent

        // --- Create buttons using text ---
        themeButton = createControlButton(isDarkMode ? "L" : "D", // L for Light, D for Dark
                isDarkMode ? "Switch to Light Theme" : "Switch to Dark Theme",
                "toggle_theme");
        themeButton.addActionListener(e -> toggleTheme());

        JButton minimizeButton = createControlButton("-", "Minimize", "minimize");
        minimizeButton.addActionListener(e -> setState(Frame.ICONIFIED));

        JButton closeButton = createControlButton("X", "Close", "close"); // Use capital X
        closeButton.addActionListener(e -> {
            // Settings saving handled by shutdown hook or exit action in Main
            System.exit(0);
        });

        controlButtons.add(themeButton);
        controlButtons.add(minimizeButton);
        controlButtons.add(closeButton);

        titleBarPanel.add(titlePanel, BorderLayout.WEST);
        titleBarPanel.add(controlButtons, BorderLayout.EAST);

        return titleBarPanel;
    }

    /**
     * Creates a control button with text.
     * @param text The text to display on the button.
     * @param tooltip The tooltip text.
     * @param actionCommand A string identifier for the button's action.
     * @return The configured JButton.
     */
    private JButton createControlButton(String text, String tooltip, String actionCommand) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setActionCommand(actionCommand); // Set action command for identification

        // Initial styling applied here, refined in applyTheme/updateTitleBarButtons
        // Determine initial type based on action command for styling
        String type = "theme"; // Default
        if ("minimize".equals(actionCommand)) type = "minimize";
        else if ("close".equals(actionCommand)) type = "close";
        styleWindowControlButton(button, type);
        return button;
    }


    private JPanel createStatusBar() {
        UIThemeManager themeManager = UIThemeManager.getInstance();

        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BorderLayout());
        // Background and border set in applyTheme()

        statusLabel = new JLabel("Ready");
        // Font and color set in applyTheme()

        cpsLabel = new JLabel("0.0 CPS");
        cpsLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        // Font and color set in applyTheme()

        statusPanel.add(statusLabel, BorderLayout.WEST);
        statusPanel.add(cpsLabel, BorderLayout.EAST);
        return statusPanel;
    }

    private JPanel createResizeHandle() {
        JPanel resizePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                // No need to call super.paintComponent if opaque is false and we paint everything
                Graphics2D g2d = (Graphics2D) g.create();
                try {
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    // Use theme color for handle
                    Color handleColor = UIThemeManager.getInstance().getColor("text_secondary");
                    g2d.setColor(handleColor);
                    int size = 2; // Dot size
                    int spacing = 3; // Spacing between dots
                    // Calculate starting position based on panel size and desired handle size
                    int startX = getWidth() - RESIZE_HANDLE_SIZE + 4; // Adjusted for padding
                    int startY = getHeight() - RESIZE_HANDLE_SIZE + 4; // Adjusted for padding

                    // Draw 3 dots diagonally
                    for (int i = 0; i < 3; i++) {
                        g2d.fillOval(startX + i * (size + spacing), startY + i * (size + spacing), size, size);
                    }
                } finally {
                    g2d.dispose();
                }
            }
        };
        resizePanel.setPreferredSize(new Dimension(RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE));
        resizePanel.setOpaque(false); // Resize handle itself is transparent
        resizePanel.setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));

        // Variables for resize operation using AtomicReference for thread safety
        final AtomicReference<Point> startDragPoint = new AtomicReference<>();
        final AtomicReference<Dimension> startFrameSize = new AtomicReference<>();

        resizePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Convert point relative to component to screen coordinates
                startDragPoint.set(e.getLocationOnScreen());
                startFrameSize.set(MainFrame.this.getSize()); // Get size of the main frame
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                startDragPoint.set(null);
                startFrameSize.set(null);
                // Optional: Recalculate layout if needed after resize
                MainFrame.this.validate();
            }
        });

        resizePanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point currentPoint = e.getLocationOnScreen();
                Point startPoint = startDragPoint.get();
                Dimension startSize = startFrameSize.get();

                if (startPoint != null && startSize != null) {
                    int deltaX = currentPoint.x - startPoint.x;
                    int deltaY = currentPoint.y - startPoint.y;

                    int newWidth = startSize.width + deltaX;
                    int newHeight = startSize.height + deltaY;

                    // Enforce minimum size
                    Dimension minSize = MainFrame.this.getMinimumSize();
                    newWidth = Math.max(minSize.width, newWidth);
                    newHeight = Math.max(minSize.height, newHeight);

                    // Set size of the MainFrame
                    MainFrame.this.setSize(newWidth, newHeight);
                    // No need to revalidate/repaint here, setSize triggers it
                }
            }
        });

        return resizePanel;
    }

    // --- Functionality Methods ---

    /**
     * Starts the timer for updating status display.
     */
    private void startStatusUpdateTimer() {
        if (statusUpdateTimer != null && statusUpdateTimer.isRunning()) {
            statusUpdateTimer.stop();
        }
        statusUpdateTimer = new Timer(250, e -> updateStatusDisplay());
        statusUpdateTimer.setInitialDelay(0);
        statusUpdateTimer.start();
    }

    /**
     * Starts the timer for updating CPS measurements.
     */
    private void startCpsUpdateTimer() {
        if (cpsUpdateTimer != null && cpsUpdateTimer.isRunning()) {
            cpsUpdateTimer.stop();
        }

        cpsUpdateTimer = new Timer(1000, e -> {
            if (clickerService != null && clickerService.isRunning() && !clickerService.isPaused()) {
                // Update measured CPS (assuming this method exists and calculates)
                // clickerService.updateMeasuredCPS(); // You might need a method like this

                // Update display
                updateStatusDisplay(); // Update status which includes CPS
            }
        });

        cpsUpdateTimer.start();
    }

    /**
     * Updates the status display with current information.
     */
    public void updateStatusDisplay() {
        // Ensure updates happen on the EDT
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::updateStatusDisplay);
            return;
        }

        if (clickerService == null || statusLabel == null || cpsLabel == null) return;

        UIThemeManager themeManager = UIThemeManager.getInstance();
        Color successColor = themeManager.getColor("success");
        Color warningColor = themeManager.getColor("warning");
        Color dangerColor = themeManager.getColor("danger");
        Color secondaryTextColor = themeManager.getColor("text_secondary"); // Default color

        String statusText;
        Color statusColor;
        String cpsText;

        if (clickerService.isRunning()) {
            if (clickerService.isPaused()) {
                statusText = "● Paused"; // Unicode circle still okay here
                statusColor = warningColor;
            } else {
                statusText = "● Running";
                statusColor = successColor;
            }

            // Show both target and actual CPS
            double targetCps = settingsManager.getCPS();
            double actualCps = clickerService.getMeasuredCPS(); // Assuming this method exists
            if (actualCps > 0) {
                cpsText = String.format("Target: %.1f / Actual: %.1f CPS", targetCps, actualCps);
            } else {
                // Show only target if actual is 0 (e.g., just started)
                cpsText = String.format("Target: %.1f CPS", targetCps);
            }
        } else {
            statusText = "● Stopped";
            statusColor = dangerColor;

            double targetCps = settingsManager.getCPS();
            cpsText = String.format("%.1f CPS", targetCps);
        }

        statusLabel.setText(statusText);
        statusLabel.setForeground(statusColor);
        cpsLabel.setText(cpsText);
        cpsLabel.setForeground(secondaryTextColor); // CPS label uses secondary text color

        // Update click indicator state
        if (clickIndicator != null) {
            clickIndicator.setActive(clickerService.isRunning() && !clickerService.isPaused());
        }

        // Update quick action bar button states
        if (quickActionBar != null) {
            quickActionBar.updateButtonStates();
        }
    }

    /**
     * Updates the control panel with current settings.
     */
    public void updateControlPanel() {
        if (controlPanel != null) {
            // Ensure UI updates are on the EDT
            SwingUtilities.invokeLater(controlPanel::loadSettingsToUI);
        }
    }

    /**
     * Sets the window transparency.
     */
    public void setWindowTransparency(double transparency) {
        // Ensure transparency is within valid range [0.0, 1.0]
        // Allow full transparency if needed, but often a minimum is desired for visibility
        transparency = Math.max(0.1, Math.min(1.0, transparency)); // Example: min 10% opacity
        setOpacity((float) transparency);
    }

    /**
     * Sets up drag listeners for moving the window using the specified component.
     */
    private void setupWindowDragListeners(Component dragComponent) {
        final AtomicReference<Point> dragOffset = new AtomicReference<>(); // Store offset from corner

        dragComponent.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Record the offset between the mouse click and the window's top-left corner
                dragOffset.set(e.getPoint());
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                dragOffset.set(null); // Clear offset when mouse is released
            }
        });

        dragComponent.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point currentOffset = dragOffset.get();
                if (currentOffset != null) {
                    // Calculate new window location based on mouse position on screen
                    // minus the initial offset
                    Point currentLocationOnScreen = e.getLocationOnScreen();
                    int newX = currentLocationOnScreen.x - currentOffset.x;
                    int newY = currentLocationOnScreen.y - currentOffset.y;
                    setLocation(newX, newY);
                }
            }
        });
    }

    /**
     * Sets up cursor tracking for auto-hide feature.
     */
    private void setupCursorTracking() {
        if (cursorTracker != null && !cursorTracker.isShutdown()) {
            cursorTracker.shutdownNow(); // Ensure previous tracker is stopped
        }
        cursorTracker = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "CursorTrackerThread");
            t.setDaemon(true); // Allow JVM to exit even if this thread is running
            return t;
        });

        cursorTracker.scheduleAtFixedRate(() -> {
            // Only check if auto-hide is enabled, window is visible but not animating
            if (isAutoHideEnabled && isVisible() && !isAnimating) {
                try {
                    Point currentMousePosition = MouseInfo.getPointerInfo().getLocation();
                    Rectangle windowBounds = getBounds();

                    // Check if mouse is OUTSIDE the window bounds
                    if (!windowBounds.contains(currentMousePosition)) {
                        // Optional: Add a small delay before hiding
                        // Thread.sleep(500); // Consider adding a small delay
                        if (!getBounds().contains(MouseInfo.getPointerInfo().getLocation())) {
                            SwingUtilities.invokeLater(this::slideOut);
                        }
                    }
                } catch (HeadlessException e) {
                    System.err.println("Could not get mouse info (headless environment?): " + e.getMessage());
                    if (cursorTracker != null) cursorTracker.shutdown(); // Stop tracking if unusable
                } catch (Exception e) { // Catch broader exceptions during check
                    System.err.println("Error in cursor tracking: " + e.getMessage());
                    // Consider logging e.printStackTrace() for debugging
                }
            }
            // Check for showing the window when hidden
            else if (isAutoHideEnabled && !isVisible() && !isAnimating) {
                try {
                    Point currentMousePosition = MouseInfo.getPointerInfo().getLocation();
                    // Check if cursor is near the top edge where the window would appear
                    GraphicsConfiguration gc = getGraphicsConfiguration();
                    Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
                    // Define a sensitive area at the top edge
                    Rectangle topEdgeArea = new Rectangle(getX(), screenInsets.top, getWidth(), AUTO_HIDE_SENSITIVITY * 2);

                    if (currentMousePosition.y < screenInsets.top + AUTO_HIDE_SENSITIVITY) {
                        SwingUtilities.invokeLater(this::slideIn);
                    }
                } catch (HeadlessException e) {
                    System.err.println("Could not get mouse info (headless environment?): " + e.getMessage());
                    if (cursorTracker != null) cursorTracker.shutdown();
                } catch (Exception e) {
                    System.err.println("Error in cursor tracking (show check): " + e.getMessage());
                }
            }
        }, 0, CURSOR_TRACK_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    // --- Animation Methods ---

    /**
     * Slides the window in from the top of the screen.
     */
    public void slideIn() {
        // Ensure this runs on the EDT
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::slideIn);
            return;
        }
        // Prevent starting slide-in if already visible and not animating, or already animating in
        if ((isVisible() && !isAnimating) || (isAnimating && targetY > -getHeight())) return;


        isAnimating = true;
        // Ensure window is visible before starting animation
        setVisible(true);
        // Start position just offscreen top
        currentY = -getHeight() - 10; // Ensure it's fully offscreen
        setLocation(getX(), currentY);

        // Calculate target Y position near the top edge, considering screen insets
        GraphicsConfiguration gc = getGraphicsConfiguration();
        Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        targetY = screenInsets.top + SLIDE_IN_TARGET_Y_OFFSET; // Target position below top inset

        if (animationTimer != null) {
            animationTimer.stop(); // Stop any existing animation
        }

        animationTimer = new Timer(ANIMATION_DELAY_MS, e -> {
            // Calculate step based on remaining distance (simple easing)
            int deltaY = (targetY - currentY) / ANIMATION_STEP_DIVIDER;
            // Ensure minimum movement to prevent getting stuck
            deltaY = (deltaY == 0) ? 1 : deltaY;

            currentY += deltaY;

            // Check if target is reached or overshot
            if (currentY >= targetY) {
                currentY = targetY; // Snap to final position
                isAnimating = false;
                ((Timer)e.getSource()).stop(); // Stop the timer
                //System.out.println("Slide In Complete.");
                // Ensure final state is rendered correctly
                setLocation(getX(), currentY);
                validate(); // May be needed after animation
                repaint();
            } else {
                setLocation(getX(), currentY); // Update position during animation
            }
        });
        animationTimer.start();
    }

    /**
     * Slides the window out to the top of the screen and hides it.
     */
    public void slideOut() {
        // Ensure this runs on the EDT
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::slideOut);
            return;
        }
        // Prevent starting slide-out if already hidden or animating out
        if (!isVisible() || (isAnimating && targetY < 0)) return;

        //System.out.println("Sliding Out...");
        isAnimating = true;
        currentY = getY(); // Start from current position
        targetY = -getHeight() - 10; // Target position fully offscreen top

        if (animationTimer != null) {
            animationTimer.stop(); // Stop any existing animation
        }

        animationTimer = new Timer(ANIMATION_DELAY_MS, e -> {
            // Calculate step based on remaining distance
            int deltaY = (targetY - currentY) / ANIMATION_STEP_DIVIDER;
            // Ensure minimum movement
            deltaY = (deltaY == 0) ? -1 : deltaY;

            currentY += deltaY;

            // Check if target is reached or passed
            if (currentY <= targetY) {
                currentY = targetY; // Snap to final offscreen position
                isAnimating = false;
                setVisible(false); // Hide the window completely
                ((Timer)e.getSource()).stop(); // Stop the timer
                //System.out.println("Slide Out Complete.");
                // Ensure final state is rendered correctly (though hidden)
                setLocation(getX(), currentY);
            } else {
                setLocation(getX(), currentY); // Update position during animation
            }
        });
        animationTimer.start();
    }

    /**
     * Toggles the visibility of the window with slide animation.
     */
    public void toggleVisibility() {
        if (isVisible() && !isAnimating) {
            slideOut();
        } else if (!isVisible() && !isAnimating) {
            slideIn();
        }
        // Do nothing if currently animating
    }

    /**
     * Enables or disables the auto-hide feature.
     */
    public void setAutoHide(boolean autoHide) {
        this.isAutoHideEnabled = autoHide;
        //System.out.println("Auto-hide set to: " + autoHide);
        // If disabling auto-hide, ensure the window is visible
        if (!autoHide && !isVisible() && !isAnimating) {
            slideIn();
        }
        // If enabling auto-hide, the cursor tracker will handle hiding/showing
    }

    /**
     * Gets the tabbed pane for UI updates.
     */
    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    /**
     * Gets the keybind manager.
     */
    public KeybindManager getKeybindManager() {
        return keybindManager;
    }

    /**
     * Gets the pattern recorder service.
     */
    public PatternRecorderService getPatternRecorderService() {
        return patternRecorderService;
    }

    /**
     * Refreshes keybind registration and event listeners.
     * Call this after changing hotkey settings.
     */
    public void refreshKeybinds() {
        // Implementation likely involves calling methods on keybindManager
        if (keybindManager != null) {
            // Example: keybindManager.reregisterKeybinds();
            System.out.println("Keybind refresh requested (implementation needed in refreshKeybinds method).");
        }
    }

    // --- Color and Theme Getters ---

    public boolean isDarkMode() { return isDarkMode; }
    public Color getPrimaryColor() { return UIThemeManager.getInstance().getColor("primary"); }
    public Color getSecondaryColor() { return UIThemeManager.getInstance().getColor("secondary"); }
    public Color getSuccessColor() { return UIThemeManager.getInstance().getColor("success"); }
    public Color getDangerColor() { return UIThemeManager.getInstance().getColor("danger"); }
    public Color getWarningColor() { return UIThemeManager.getInstance().getColor("warning"); }
    public Color getBackgroundColor() { return UIThemeManager.getInstance().getColor("background"); }
    public Color getCardBackgroundColor() { return UIThemeManager.getInstance().getColor("card"); }
    public Color getSurfaceColor() { return UIThemeManager.getInstance().getColor("surface"); } // Added getter
    public Color getBorderColor() { return UIThemeManager.getInstance().getColor("border"); }
    public Color getTextPrimaryColor() { return UIThemeManager.getInstance().getColor("text_primary"); }
    public Color getTextSecondaryColor() { return UIThemeManager.getInstance().getColor("text_secondary"); }
    public Color getTextFieldBackgroundColor() { return UIThemeManager.getInstance().getColor("input_background"); }
    public Color getSelectionBackgroundColor() {
        // Provide distinct selection colors for light/dark modes
        return isDarkMode ?
                UIThemeManager.getInstance().getColor("primary") : // Use primary in dark mode
                UIThemeManager.lighten(UIThemeManager.getInstance().getColor("primary"), 0.3f); // Lighter primary in light mode
    }
    public Color getSelectionForegroundColor() {
        // Ensure good contrast with selection background
        return isDarkMode ? Color.BLACK : Color.WHITE; // Black text on primary, White text on lightened primary
    }

    /**
     * Cleans up resources when the frame is disposed.
     */
    @Override
    public void dispose() {
        System.out.println("Disposing MainFrame...");
        // Stop timers
        if (statusUpdateTimer != null) statusUpdateTimer.stop();
        if (cpsUpdateTimer != null) cpsUpdateTimer.stop();
        if (animationTimer != null) animationTimer.stop();

        // Shutdown executor services gracefully
        if (cursorTracker != null && !cursorTracker.isShutdown()) {
            cursorTracker.shutdown();
            try {
                if (!cursorTracker.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                    cursorTracker.shutdownNow();
                }
            } catch (InterruptedException ie) {
                cursorTracker.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Cleanup child components if they have resources
        if (statisticsPanel != null) {
            statisticsPanel.cleanup();
        }
        if (clickIndicator != null) {
            clickIndicator.cleanup();
        }
        // ControlPanel, etc., might have listeners but typically don't need explicit cleanup unless they manage threads/timers


        // Call superclass dispose
        super.dispose();
        System.out.println("MainFrame disposed.");
    }
}
