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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The main window frame for the YakClicker application.
 * Features custom styling, animations, and integrates various panels.
 * Enhanced with modern UI components, performance metrics, and pattern recording.
 */
public class MainFrame extends JFrame {

    // --- Constants ---
    private static final String APP_TITLE = "YakClicker";
    private static final Dimension DEFAULT_SIZE = new Dimension(800, 600);
    private static final Dimension MINIMUM_SIZE = new Dimension(700, 500);
    private static final int ANIMATION_DELAY_MS = 5;
    private static final int ANIMATION_STEP_DIVIDER = 8; // Higher value = slower easing
    private static final int SLIDE_IN_TARGET_Y_OFFSET = 50; // Pixels from screen top (after insets)
    private static final int AUTO_HIDE_SENSITIVITY = 5; // Pixels near edge to trigger show
    private static final int CURSOR_TRACK_INTERVAL_MS = 200;
    private static final int RESIZE_HANDLE_SIZE = 15; // Square dimension for the resize handle
    private static final int STATUS_UPDATE_INTERVAL_MS = 250;
    private static final int CPS_UPDATE_INTERVAL_MS = 1000;

    // Action Commands for Buttons
    private static final String ACTION_TOGGLE_THEME = "toggle_theme";
    private static final String ACTION_MINIMIZE = "minimize";
    private static final String ACTION_CLOSE = "close";

    // --- Dependencies ---
    private final SettingsManager settingsManager;
    private final AutoClickerService clickerService;
    private final PatternRecorderService patternRecorderService;
    private final KeybindManager keybindManager;
    private final UIThemeManager themeManager; // Store instance

    // --- UI Components ---
    private JPanel titleBar;
    private JPanel statusBar;
    private JPanel bottomPanel;
    private JPanel rightPanel; // Panel containing the click indicator
    private JPanel contentPanel; // Main content area below title bar
    private JLabel statusLabel;
    private JLabel cpsLabel;
    private JLabel titleLabel; // Reference to update font/color
    private JLabel appIconLabel; // Reference to update font/color
    private JButton themeButton; // Reference to update text/tooltip
    private JTabbedPane tabbedPane;
    private ControlPanel controlPanel;
    private StatisticsPanel statisticsPanel;
    private ClickIndicatorPanel clickIndicator;
    private QuickActionBar quickActionBar;

    // --- Timers & Executors ---
    private Timer statusUpdateTimer;
    private Timer cpsUpdateTimer;
    private Timer animationTimer;
    private ScheduledExecutorService cursorTracker;

    // --- State ---
    private boolean isDarkMode;
    private boolean isAutoHideEnabled;
    private volatile boolean isAnimating = false; // Flag for animation state (volatile for visibility)
    private int targetY; // Target Y position for animations
    private int currentY; // Current Y position during animations

    // --- Drag & Resize State ---
    private final AtomicReference<Point> dragOffset = new AtomicReference<>();
    private final AtomicReference<Point> resizeStartDragPoint = new AtomicReference<>();
    private final AtomicReference<Dimension> resizeStartFrameSize = new AtomicReference<>();

    /**
     * Creates a new MainFrame.
     *
     * @param clickerService         The auto clicker service instance.
     * @param settingsManager        The settings manager instance.
     * @param patternRecorderService The pattern recorder service instance (can be null).
     */
    public MainFrame(AutoClickerService clickerService, SettingsManager settingsManager, PatternRecorderService patternRecorderService) {
        this.clickerService = clickerService;
        this.settingsManager = settingsManager;
        this.patternRecorderService = patternRecorderService;
        this.themeManager = UIThemeManager.getInstance(); // Get theme manager instance

        // Load initial theme state
        this.isDarkMode = settingsManager.isDarkMode();
        this.themeManager.setDarkMode(isDarkMode);

        // Initialize Keybind Manager
        this.keybindManager = new KeybindManager(settingsManager, this, clickerService);

        // Configure Frame Properties
        configureFrame();

        // Create UI Components
        createUIComponents();

        // Assemble Layout
        assembleLayout();

        // Apply Initial Theme
        applyTheme(); // Apply theme after components are created and added

        // Initialize State & Listeners
        initializeStateAndListeners();

        // Start Timers
        startTimers();
    }

    // --- Initialization & Configuration ---

    /**
     * Configures the main JFrame properties (title, size, behavior, appearance).
     */
    private void configureFrame() {
        setTitle(APP_TITLE);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Main handles shutdown hook
        setSize(DEFAULT_SIZE);
        setMinimumSize(MINIMUM_SIZE);
        setLocationRelativeTo(null);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0)); // Transparent background for custom shape
        setAlwaysOnTop(settingsManager.isAlwaysOnTop());
        setOpacity((float) Math.max(0.1, Math.min(1.0, settingsManager.getTransparency()))); // Apply initial transparency

        // Add listener to update shape on resize
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateWindowShape();
            }
        });
        // Set initial shape (call after size is set)
        updateWindowShape();
    }

    /**
     * Creates all the major UI components for the frame.
     */
    private void createUIComponents() {
        controlPanel = new ControlPanel(clickerService, settingsManager, this);
        clickIndicator = new ClickIndicatorPanel();
        quickActionBar = new QuickActionBar(clickerService, settingsManager, this);
        statisticsPanel = new StatisticsPanel(settingsManager, clickerService, this);

        titleBar = createTitleBar();
        statusBar = createStatusBar();
        JPanel resizeHandle = createResizeHandle();
        bottomPanel = createBottomPanel(statusBar, resizeHandle);
        tabbedPane = createTabbedPane();
        rightPanel = createRightPanel(clickIndicator);
        contentPanel = createContentPanel(tabbedPane, rightPanel, quickActionBar);
    }

    /**
     * Assembles the main layout of the frame using the created components.
     */
    private void assembleLayout() {
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(titleBar, BorderLayout.NORTH);
        contentPane.add(contentPanel, BorderLayout.CENTER);
        contentPane.add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * Initializes frame state, sets up listeners, and prepares for display.
     */
    private void initializeStateAndListeners() {
        // Initial animation position (offscreen top)
        currentY = -getHeight();
        setLocation(getX(), currentY);

        // Window movement listeners (attach to title bar)
        setupWindowDragListeners(titleBar);

        // Cursor tracking for auto-show/hide
        setupCursorTracking();
        setAutoHide(settingsManager.isAutoHide()); // Apply initial setting

        // Click listener for indicator
        clickerService.addClickListener(new AbstractClickService.ClickListener() {
            @Override
            public void onClickPerformed() {
                if (clickIndicator != null) {
                    SwingUtilities.invokeLater(clickIndicator::addClickDot);
                }
            }
        });

        // Window event handling
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Shutdown hook in Main handles saving settings
            }

            @Override
            public void windowIconified(WindowEvent e) {
                handleMinimizeToTray();
            }

            @Override
            public void windowOpened(WindowEvent e) {
                // Start slide-in animation when window first opens
                slideIn();
            }
        });
    }

    /**
     * Starts the background timers for status updates and CPS calculation.
     */
    private void startTimers() {
        startStatusUpdateTimer();
        startCpsUpdateTimer();
    }

    // --- UI Element Creation Methods ---

    private JPanel createTitleBar() {
        JPanel barPanel = new JPanel(new BorderLayout());

        // --- Title Section (West) ---
        JPanel titleSection = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titleSection.setOpaque(false);

        appIconLabel = new JLabel(";)"); // Simple text icon
        appIconLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0)); // Top/Bottom padding

        titleLabel = new JLabel(APP_TITLE, JLabel.LEFT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(8, 5, 8, 10)); // Padding

        titleSection.add(appIconLabel);
        titleSection.add(titleLabel);

        // --- Control Buttons Section (East) ---
        JPanel buttonSection = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonSection.setOpaque(false);

        themeButton = createControlButton(isDarkMode ? "L" : "D",
                isDarkMode ? "Switch to Light Theme" : "Switch to Dark Theme",
                ACTION_TOGGLE_THEME, e -> toggleTheme());

        JButton minimizeButton = createControlButton("-", "Minimize", ACTION_MINIMIZE,
                e -> setState(Frame.ICONIFIED));

        JButton closeButton = createControlButton("X", "Close", ACTION_CLOSE,
                e -> System.exit(0)); // Shutdown hook handles saving

        buttonSection.add(themeButton);
        buttonSection.add(minimizeButton);
        buttonSection.add(closeButton);

        barPanel.add(titleSection, BorderLayout.WEST);
        barPanel.add(buttonSection, BorderLayout.EAST);

        return barPanel;
    }

    private JButton createControlButton(String text, String tooltip, String actionCommand, ActionListener listener) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setActionCommand(actionCommand);
        if (listener != null) {
            button.addActionListener(listener);
        }
        // Initial styling (will be refined by applyTheme)
        styleWindowControlButton(button, actionCommand); // Use actionCommand to determine type initially
        return button;
    }

    private JPanel createStatusBar() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setOpaque(false); // Let applyTheme handle background/border

        statusLabel = new JLabel("Ready");
        cpsLabel = new JLabel("0.0 CPS");
        cpsLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        statusPanel.add(statusLabel, BorderLayout.WEST);
        statusPanel.add(cpsLabel, BorderLayout.EAST);
        return statusPanel;
    }

    private JPanel createBottomPanel(JPanel status, JPanel resizeHandle) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false); // Keep transparent
        panel.add(status, BorderLayout.CENTER);
        panel.add(resizeHandle, BorderLayout.EAST);
        return panel;
    }

    private JTabbedPane createTabbedPane() {
        JTabbedPane pane = new JTabbedPane();
        pane.setFocusable(false); // Prevent focus traversal issues

        pane.addTab("Settings", null, controlPanel, "Configure auto-clicker settings");
        pane.addTab("Statistics", null, statisticsPanel, "View performance statistics");

        // Conditionally add Patterns tab
        if (patternRecorderService != null) {
            PatternRecorderPanel patternRecorderPanel = new PatternRecorderPanel(patternRecorderService, this);
            pane.addTab("Patterns", null, patternRecorderPanel, "Record and play click patterns");
        }

        return pane;
    }

    private JPanel createRightPanel(Component content) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(true); // Opaque background for click indicator stability
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 10)); // Padding
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createContentPanel(Component center, Component east, Component north) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false); // Transparent to show frame background

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.add(center, BorderLayout.CENTER); // Add tabbed pane

        panel.add(north, BorderLayout.NORTH); // Quick action bar
        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(east, BorderLayout.EAST); // Right panel (click indicator)

        return panel;
    }


    private JPanel createResizeHandle() {
        JPanel resizePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                // Don't call super if opaque is false and we paint everything
                Graphics2D g2d = (Graphics2D) g.create();
                try {
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setColor(themeManager.getColor("text_secondary")); // Use theme color

                    int dotSize = 2;
                    int spacing = 3;
                    // Calculate starting position relative to bottom-right corner
                    int startX = getWidth() - RESIZE_HANDLE_SIZE + 4;
                    int startY = getHeight() - RESIZE_HANDLE_SIZE + 4;

                    // Draw 3 diagonal dots
                    for (int i = 0; i < 3; i++) {
                        g2d.fillOval(startX + i * (dotSize + spacing), startY + i * (dotSize + spacing), dotSize, dotSize);
                    }
                } finally {
                    g2d.dispose();
                }
            }
        };
        resizePanel.setPreferredSize(new Dimension(RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE));
        resizePanel.setOpaque(false);
        resizePanel.setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));

        setupResizeListeners(resizePanel);

        return resizePanel;
    }

    // --- Theming ---

    /**
     * Toggles the application theme between light and dark mode and applies it.
     */
    public void toggleTheme() {
        isDarkMode = !isDarkMode;
        settingsManager.setDarkMode(isDarkMode);
        themeManager.setDarkMode(isDarkMode);
        applyTheme();
    }

    /**
     * Applies the current theme settings to all relevant UI components.
     */
    private void applyTheme() {
        Color frameBg = themeManager.getColor("surface");
        Color contentBg = themeManager.getColor("background");
        Color borderCol = themeManager.getColor("border");
        Color titleTextCol = themeManager.getColor("text_primary");
        Color statusTextCol = themeManager.getColor("text_secondary");
        Color primaryCol = themeManager.getColor("primary");

        // Frame background (visible at rounded corners)
        getRootPane().setBackground(frameBg);
        // Main content area background
        getContentPane().setBackground(contentBg);

        // --- Apply Theme to Panels ---
        applyThemeToTitleBar(frameBg, borderCol, titleTextCol, primaryCol);
        applyThemeToStatusBar(frameBg, borderCol, statusTextCol);
        applyThemeToBottomPanel(); // Mostly transparent, ensures resize handle redraws
        applyThemeToRightPanel(contentBg);
        applyThemeToContentPanel(); // Ensure child panels get themed

        // --- Apply Theme to Specific Components ---
        if (statusLabel != null) {
            // Color is set dynamically in updateStatusDisplay
            statusLabel.setFont(themeManager.getFont("body_regular"));
        }
        if (cpsLabel != null) {
            cpsLabel.setForeground(statusTextCol);
            cpsLabel.setFont(themeManager.getFont("body_regular"));
        }
        if (clickIndicator != null) {
            clickIndicator.applyThemeColors(this); // Pass frame for context if needed
        }
        if (tabbedPane != null) {
            tabbedPane.setBackground(contentBg);
            themeManager.styleTabbedPane(tabbedPane);
        }

        // --- Apply Theme to Child Panels ---
        if (controlPanel != null) {
            controlPanel.setBackground(contentBg);
            controlPanel.applyThemeColors();
        }
        if (statisticsPanel != null) {
            statisticsPanel.applyThemeColors();
        }
        if (quickActionBar != null) {
            quickActionBar.applyThemeColors();
        }

        // --- Final Updates ---
        updateWindowShape(); // Re-apply shape in case corner radius changed
        revalidate();
        repaint();
    }

    private void applyThemeToTitleBar(Color background, Color border, Color titleText, Color iconColor) {
        if (titleBar == null) return;
        titleBar.setBackground(background);
        titleBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, border));

        if (appIconLabel != null) {
            appIconLabel.setForeground(iconColor);
            appIconLabel.setFont(new Font("Arial", Font.BOLD, 18)); // Style for icon
        }
        if (titleLabel != null) {
            titleLabel.setForeground(titleText);
            titleLabel.setFont(themeManager.getFont("heading_medium"));
        }
        // Update control buttons styling and theme button text
        updateTitleBarButtons();
    }

    private void applyThemeToStatusBar(Color background, Color border, Color statusText) {
        if (statusBar == null) return;
        statusBar.setBackground(background);
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, border),
                BorderFactory.createEmptyBorder(5, 12, 5, 12) // Padding
        ));
        // Note: Status label color is dynamic, CPS label color set in applyTheme
    }

    private void applyThemeToBottomPanel() {
        if (bottomPanel != null) {
            bottomPanel.setOpaque(false); // Ensure it stays transparent
            // Force repaint of children like the resize handle
            bottomPanel.revalidate();
            bottomPanel.repaint();
        }
    }

    private void applyThemeToRightPanel(Color background) {
        if (rightPanel != null) {
            rightPanel.setBackground(background);
            rightPanel.setOpaque(true); // Ensure opaque background
        }
    }

    private void applyThemeToContentPanel() {
        if (contentPanel != null) {
            contentPanel.setOpaque(false); // Ensure it stays transparent
            // Child backgrounds (tabbedPane, rightPanel) are set individually
        }
    }


    /**
     * Updates the styling of title bar buttons based on the current theme
     * and updates the theme toggle button's text/tooltip.
     */
    private void updateTitleBarButtons() {
        if (titleBar == null || titleBar.getComponentCount() < 2) return;
        Component buttonPanelComp = titleBar.getComponent(1); // Assuming button panel is second (East)

        if (buttonPanelComp instanceof JPanel) {
            JPanel buttonPanel = (JPanel) buttonPanelComp;
            for (Component c : buttonPanel.getComponents()) {
                if (c instanceof JButton) {
                    JButton button = (JButton) c;
                    String actionCommand = button.getActionCommand();

                    if (ACTION_TOGGLE_THEME.equals(actionCommand)) {
                        button.setText(isDarkMode ? "L" : "D"); // L=Light, D=Dark
                        button.setToolTipText(isDarkMode ? "Switch to Light Theme" : "Switch to Dark Theme");
                        styleWindowControlButton(button, actionCommand); // Re-apply styling
                    } else {
                        // Re-apply styling for minimize and close buttons
                        styleWindowControlButton(button, actionCommand);
                    }
                }
            }
        }
    }

    /**
     * Applies visual styling to a window control button (minimize, close, theme).
     * Uses text characters instead of icons.
     *
     * @param button        The JButton to style.
     * @param actionCommand The action command string identifying the button's purpose (e.g., "close").
     */
    private void styleWindowControlButton(JButton button, String actionCommand) {
        // Remove previous custom listeners to avoid duplication if called multiple times
        for (MouseListener ml : button.getMouseListeners()) {
            if (ml instanceof CustomHoverListener) { // Identify our specific listener
                button.removeMouseListener(ml);
            }
        }

        // --- Base Styling ---
        button.setFont(new Font("Arial", Font.BOLD, 14)); // Consistent font
        button.setForeground(themeManager.getColor("text_secondary"));
        button.setBackground(null); // Transparent background initially
        button.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12)); // Padding
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // --- Hover/Interaction Styling ---
        Color hoverBg;
        Color hoverFg = button.getForeground(); // Default hover foreground
        Color originalFg = themeManager.getColor("text_secondary");

        switch (actionCommand) {
            case ACTION_CLOSE:
                hoverBg = themeManager.getColor("danger");
                hoverFg = Color.WHITE; // White text on red hover
                break;
            case ACTION_MINIMIZE:
            case ACTION_TOGGLE_THEME:
            default:
                hoverBg = themeManager.isDarkMode() ?
                        themeManager.getColor("border") :
                        UIThemeManager.darken(themeManager.getColor("surface"), 0.05f);
                // Keep original text color for minimize/theme hover
                hoverFg = originalFg;
                break;
        }

        // Add the custom hover listener
        button.addMouseListener(new CustomHoverListener(hoverBg, hoverFg, originalFg));
    }

    /**
     * Custom MouseAdapter to handle hover effects for control buttons.
     */
    private static class CustomHoverListener extends MouseAdapter {
        private final Color hoverBg;
        private final Color hoverFg;
        private final Color originalFg;

        CustomHoverListener(Color hoverBg, Color hoverFg, Color originalFg) {
            this.hoverBg = hoverBg;
            this.hoverFg = hoverFg;
            this.originalFg = originalFg;
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            JButton button = (JButton) e.getSource();
            button.setBackground(hoverBg);
            button.setForeground(hoverFg);
            button.setContentAreaFilled(true); // Make background visible
            button.setOpaque(true);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            JButton button = (JButton) e.getSource();
            button.setContentAreaFilled(false); // Make background transparent again
            button.setOpaque(false);
            button.setForeground(originalFg); // Restore original text color
        }
    }


    // --- Event Handling & Listeners ---

    /**
     * Sets up listeners for dragging the undecorated window.
     *
     * @param dragComponent The component (e.g., title bar) used to initiate the drag.
     */
    private void setupWindowDragListeners(Component dragComponent) {
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
     * Sets up listeners for resizing the undecorated window using the handle.
     *
     * @param resizeHandle The component used to initiate the resize.
     */
    private void setupResizeListeners(Component resizeHandle) {
        resizeHandle.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                resizeStartDragPoint.set(e.getLocationOnScreen());
                resizeStartFrameSize.set(MainFrame.this.getSize());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                resizeStartDragPoint.set(null);
                resizeStartFrameSize.set(null);
                MainFrame.this.validate(); // Re-layout components after resize
            }
        });

        resizeHandle.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point currentPoint = e.getLocationOnScreen();
                Point startPoint = resizeStartDragPoint.get();
                Dimension startSize = resizeStartFrameSize.get();

                if (startPoint != null && startSize != null) {
                    int deltaX = currentPoint.x - startPoint.x;
                    int deltaY = currentPoint.y - startPoint.y;

                    int newWidth = startSize.width + deltaX;
                    int newHeight = startSize.height + deltaY;

                    // Enforce minimum size
                    Dimension minSize = MainFrame.this.getMinimumSize();
                    newWidth = Math.max(minSize.width, newWidth);
                    newHeight = Math.max(minSize.height, newHeight);

                    MainFrame.this.setSize(newWidth, newHeight);
                    // setSize implicitly calls revalidate/repaint
                }
            }
        });
    }

    /**
     * Sets up the scheduled task for tracking the cursor position for auto-hide.
     */
    private void setupCursorTracking() {
        // Ensure previous tracker is stopped before creating a new one
        shutdownCursorTracker();

        cursorTracker = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "YakClicker-CursorTracker");
            t.setDaemon(true); // Allow JVM exit even if running
            return t;
        });

        cursorTracker.scheduleAtFixedRate(this::checkCursorForAutoHide,
                0, CURSOR_TRACK_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Checks cursor position and triggers slide-in/slide-out if auto-hide is enabled.
     * This method is executed by the cursorTracker executor.
     */
    private void checkCursorForAutoHide() {
        if (!isAutoHideEnabled || isAnimating) {
            return; // Do nothing if disabled, animating
        }

        try {
            Point mousePos = MouseInfo.getPointerInfo().getLocation();
            Rectangle windowBounds = getBounds(); // Get current bounds on screen

            if (isVisible()) {
                // --- Check for Hiding ---
                // Hide if mouse is outside the window bounds
                if (!windowBounds.contains(mousePos)) {
                    // Optional: Add a small delay before hiding to prevent accidental hides
                    // try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                    // Re-check after delay
                    // if (!getBounds().contains(MouseInfo.getPointerInfo().getLocation())) {
                    SwingUtilities.invokeLater(this::slideOut);
                    // }
                }
            } else {
                // --- Check for Showing ---
                // Show if mouse is near the top edge where the window would appear
                GraphicsConfiguration gc = getGraphicsConfiguration();
                if (gc == null) return; // Cannot determine screen insets
                Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(gc);

                // Define a sensitive area near the top edge (adjust sensitivity as needed)
                // Check if cursor Y is within the top sensitivity zone
                if (mousePos.y >= screenInsets.top && mousePos.y < screenInsets.top + AUTO_HIDE_SENSITIVITY * 2) {
                    // Optional: Check X coordinate too, if needed
                    // Rectangle topEdgeArea = new Rectangle(getX(), screenInsets.top, getWidth(), AUTO_HIDE_SENSITIVITY * 2);
                    // if (topEdgeArea.contains(mousePos)) {
                    SwingUtilities.invokeLater(this::slideIn);
                    // }
                }
            }
        } catch (HeadlessException e) {
            System.err.println("Cannot track mouse in headless environment. Disabling auto-hide check.");
            shutdownCursorTracker(); // Stop the tracker if it can't function
        } catch (Exception e) {
            // Catch unexpected errors during check
            System.err.println("Error during cursor auto-hide check: " + e.getMessage());
            // e.printStackTrace(); // Uncomment for detailed debugging
        }
    }


    /**
     * Shuts down the cursor tracking executor service gracefully.
     */
    private void shutdownCursorTracker() {
        if (cursorTracker != null && !cursorTracker.isShutdown()) {
            cursorTracker.shutdown();
            try {
                // Wait a short time for tasks to finish
                if (!cursorTracker.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                    cursorTracker.shutdownNow(); // Force shutdown if necessary
                }
            } catch (InterruptedException e) {
                cursorTracker.shutdownNow();
                Thread.currentThread().interrupt(); // Preserve interrupt status
            }
        }
        cursorTracker = null; // Allow garbage collection
    }

    /**
     * Handles the minimize-to-tray logic based on settings.
     */
    private void handleMinimizeToTray() {
        if (settingsManager.isMinimizeToTray() && SystemTray.isSupported()) {
            setVisible(false); // Hide frame when minimized if tray icon is used
            // Assumes another part of the application manages the SystemTray icon itself
        }
        // If not minimizing to tray, default JFrame minimize behavior occurs
    }

    // --- Core Functionality ---

    /**
     * Updates the status label and CPS display. Should be called on the EDT.
     */
    public void updateStatusDisplay() {
        // Ensure updates happen on the EDT (redundant check if always called via invokeLater)
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::updateStatusDisplay);
            return;
        }

        if (clickerService == null || statusLabel == null || cpsLabel == null) return;

        Color successColor = themeManager.getColor("success");
        Color warningColor = themeManager.getColor("warning");
        Color dangerColor = themeManager.getColor("danger");
        Color secondaryTextColor = themeManager.getColor("text_secondary");

        String statusText;
        Color statusColor;
        String cpsText;

        if (clickerService.isRunning()) {
            if (clickerService.isPaused()) {
                statusText = "● Paused";
                statusColor = warningColor;
            } else {
                statusText = "● Running";
                statusColor = successColor;
            }
            double targetCps = settingsManager.getCPS();
            double actualCps = clickerService.getMeasuredCPS();
            cpsText = (actualCps > 0)
                    ? String.format("Target: %.1f / Actual: %.1f CPS", targetCps, actualCps)
                    : String.format("Target: %.1f CPS", targetCps);
        } else {
            statusText = "● Stopped";
            statusColor = dangerColor;
            cpsText = String.format("%.1f CPS", settingsManager.getCPS());
        }

        statusLabel.setText(statusText);
        statusLabel.setForeground(statusColor);
        cpsLabel.setText(cpsText);
        cpsLabel.setForeground(secondaryTextColor); // CPS always uses secondary color

        // Update related components
        if (clickIndicator != null) {
            clickIndicator.setActive(clickerService.isRunning() && !clickerService.isPaused());
        }
        if (quickActionBar != null) {
            quickActionBar.updateButtonStates();
        }
    }

    /**
     * Reloads settings from the SettingsManager into the ControlPanel UI.
     * Ensures the update happens on the Event Dispatch Thread.
     */
    public void updateControlPanel() {
        if (controlPanel != null) {
            SwingUtilities.invokeLater(controlPanel::loadSettingsToUI);
        }
    }

    /**
     * Sets the window's transparency (opacity).
     *
     * @param transparency Value between 0.0 (fully transparent) and 1.0 (fully opaque).
     * Clamped to a minimum of 0.1 for visibility.
     */
    public void setWindowTransparency(double transparency) {
        float opacity = (float) Math.max(0.1, Math.min(1.0, transparency));
        setOpacity(opacity);
    }

    /**
     * Updates the window's shape to have rounded corners based on the theme.
     */
    private void updateWindowShape() {
        if (isDisplayable()) { // Ensure component is added to the hierarchy
            // Use large radius for the main window frame
            float cornerRadius = (float) UIThemeManager.CORNER_RADIUS_LARGE;
            setShape(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius));
        }
    }

    /**
     * Refreshes keybind registration. Delegates to the KeybindManager.
     */
    public void refreshKeybinds() {
        if (keybindManager != null) {
            // Assuming KeybindManager has a method to re-register listeners
            // keybindManager.reregisterKeybinds();
            System.out.println("Keybind refresh requested (implementation needed in KeybindManager).");
        }
    }

    /**
     * Enables or disables the auto-hide feature.
     *
     * @param autoHide true to enable, false to disable.
     */
    public void setAutoHide(boolean autoHide) {
        this.isAutoHideEnabled = autoHide;
        // If disabling auto-hide, ensure the window becomes visible if it was hidden
        if (!autoHide && !isVisible() && !isAnimating) {
            slideIn();
        }
        // If enabling, the cursor tracker will handle showing/hiding as needed
    }

    // --- Animation Methods ---

    /**
     * Slides the window into view from the top of the screen.
     * Ensures execution on the EDT.
     */
    public void slideIn() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::slideIn);
            return;
        }
        // Prevent starting slide-in if already visible and not animating, or already animating in
        if ((isVisible() && !isAnimating) || (isAnimating && targetY > -getHeight())) {
            return;
        }

        isAnimating = true;
        setVisible(true); // Make visible before starting animation

        // Start position just offscreen top
        currentY = -getHeight() - 10;
        setLocation(getX(), currentY);

        // Calculate target Y based on screen insets
        GraphicsConfiguration gc = getGraphicsConfiguration();
        Insets screenInsets = (gc != null) ? Toolkit.getDefaultToolkit().getScreenInsets(gc) : new Insets(0,0,0,0);
        targetY = screenInsets.top + SLIDE_IN_TARGET_Y_OFFSET;

        startAnimationTimer(true); // Start timer for sliding in
    }

    /**
     * Slides the window out of view to the top of the screen and hides it.
     * Ensures execution on the EDT.
     */
    public void slideOut() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::slideOut);
            return;
        }
        // Prevent starting slide-out if already hidden or animating out
        if (!isVisible() || (isAnimating && targetY < 0)) {
            return;
        }

        isAnimating = true;
        currentY = getY(); // Start from current position
        targetY = -getHeight() - 10; // Target position fully offscreen top

        startAnimationTimer(false); // Start timer for sliding out
    }

    /**
     * Starts or restarts the animation timer for sliding in or out.
     *
     * @param isSlidingIn True if animating in, false if animating out.
     */
    private void startAnimationTimer(boolean isSlidingIn) {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }

        animationTimer = new Timer(ANIMATION_DELAY_MS, e -> {
            // Calculate step (simple easing)
            int deltaY = (targetY - currentY) / ANIMATION_STEP_DIVIDER;
            // Ensure minimum movement to prevent getting stuck near the target
            if (isSlidingIn) {
                deltaY = Math.max(1, deltaY); // Move at least 1 pixel down
            } else {
                deltaY = Math.min(-1, deltaY); // Move at least 1 pixel up
            }

            currentY += deltaY;

            // Check completion condition
            boolean finished = isSlidingIn ? (currentY >= targetY) : (currentY <= targetY);

            if (finished) {
                currentY = targetY; // Snap to final position
                isAnimating = false;
                ((Timer) e.getSource()).stop();
                if (!isSlidingIn) {
                    setVisible(false); // Hide only after sliding out completely
                }
                setLocation(getX(), currentY); // Ensure final position
                // Optional: validate() might be needed if layout changes affect animation bounds
                // validate();
                repaint(); // Ensure final frame is painted correctly
            } else {
                setLocation(getX(), currentY); // Update position during animation
            }
        });
        animationTimer.setInitialDelay(0);
        animationTimer.start();
    }


    /**
     * Toggles the visibility of the window using slide animations.
     */
    public void toggleVisibility() {
        if (isAnimating) return; // Prevent action during animation

        if (isVisible()) {
            slideOut();
        } else {
            slideIn();
        }
    }


    // --- Timer Management ---

    private void startStatusUpdateTimer() {
        stopTimer(statusUpdateTimer); // Ensure previous timer is stopped
        statusUpdateTimer = new Timer(STATUS_UPDATE_INTERVAL_MS, e -> SwingUtilities.invokeLater(this::updateStatusDisplay));
        statusUpdateTimer.setInitialDelay(0);
        statusUpdateTimer.start();
    }

    private void startCpsUpdateTimer() {
        stopTimer(cpsUpdateTimer); // Ensure previous timer is stopped
        cpsUpdateTimer = new Timer(CPS_UPDATE_INTERVAL_MS, e -> {
            // Only need to trigger status update if running, as it includes CPS
            if (clickerService != null && clickerService.isRunning() && !clickerService.isPaused()) {
                // Potentially trigger CPS recalculation in service if needed:
                // clickerService.calculateCurrentCPS();
                SwingUtilities.invokeLater(this::updateStatusDisplay);
            }
        });
        cpsUpdateTimer.setInitialDelay(CPS_UPDATE_INTERVAL_MS); // Start after 1 second
        cpsUpdateTimer.start();
    }

    /**
     * Safely stops a Swing Timer if it's running.
     * @param timer The Timer to stop.
     */
    private void stopTimer(Timer timer) {
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }
    }

    // --- Getters for Child Components/Services ---

    public JTabbedPane getTabbedPane() { return tabbedPane; }
    public KeybindManager getKeybindManager() { return keybindManager; }
    public PatternRecorderService getPatternRecorderService() { return patternRecorderService; }

    // --- Theme Color Getters (Delegates to UIThemeManager) ---
    // These provide convenient access for child components

    public boolean isDarkMode() { return themeManager.isDarkMode(); }
    public Color getPrimaryColor() { return themeManager.getColor("primary"); }
    public Color getSecondaryColor() { return themeManager.getColor("secondary"); }
    public Color getSuccessColor() { return themeManager.getColor("success"); }
    public Color getDangerColor() { return themeManager.getColor("danger"); }
    public Color getWarningColor() { return themeManager.getColor("warning"); }
    public Color getBackgroundColor() { return themeManager.getColor("background"); }
    public Color getCardBackgroundColor() { return themeManager.getColor("card"); }
    public Color getSurfaceColor() { return themeManager.getColor("surface"); }
    public Color getBorderColor() { return themeManager.getColor("border"); }
    public Color getTextPrimaryColor() { return themeManager.getColor("text_primary"); }
    public Color getTextSecondaryColor() { return themeManager.getColor("text_secondary"); }
    public Color getTextFieldBackgroundColor() { return themeManager.getColor("input_background"); }
    public Color getSelectionBackgroundColor() { return themeManager.getColor("selection_background"); } // Assuming theme manager provides this
    public Color getSelectionForegroundColor() { return themeManager.getColor("selection_foreground"); } // Assuming theme manager provides this


    // --- Resource Cleanup ---

    /**
     * Cleans up resources like timers and executors when the frame is disposed.
     * This is crucial for graceful shutdown.
     */
    @Override
    public void dispose() {
        System.out.println("Disposing MainFrame and cleaning up resources...");

        // Stop all timers
        stopTimer(statusUpdateTimer);
        stopTimer(cpsUpdateTimer);
        stopTimer(animationTimer);

        // Shutdown executor services
        shutdownCursorTracker();

        // Cleanup child components that might hold resources (like timers)
        if (statisticsPanel != null) {
            statisticsPanel.cleanup(); // Assuming StatisticsPanel might have timers/threads
        }
        if (clickIndicator != null) {
            clickIndicator.cleanup(); // Assuming ClickIndicatorPanel might have timers
        }
        // Other panels like ControlPanel usually don't need explicit cleanup unless they start threads/timers

        // Call superclass dispose LAST
        super.dispose();
        System.out.println("MainFrame disposed.");
    }
}
