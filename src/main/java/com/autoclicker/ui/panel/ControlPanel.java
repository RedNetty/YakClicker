package com.autoclicker.ui.panel;

import com.autoclicker.service.click.AutoClickerService;
import com.autoclicker.service.keybind.KeybindManager;
import com.autoclicker.storage.SettingsManager;
import com.autoclicker.ui.frame.MainFrame;
import com.autoclicker.ui.theme.UIThemeManager;
import com.autoclicker.util.PlatformDetector;
import com.autoclicker.util.UIUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Enhanced control panel for the AutoClicker application, providing settings and controls.
 * Redesigned with a modern UI using consistent styling for smooth appearance across the application.
 * Uses UIThemeManager for consistent theming and UIUtils for component styling.
 */
public class ControlPanel extends JPanel {
    private final AutoClickerService clickerService;
    private final SettingsManager settingsManager;
    private final MainFrame parentFrame;
    private final UIThemeManager themeManager;

    // UI Components - grouped by category
    // Click Settings Components
    private JSlider cpsSlider;
    private JSpinner cpsSpinner;
    private JComboBox<String> clickModeComboBox;
    private JComboBox<String> mouseButtonComboBox;

    // Humanization Components
    private JCheckBox randomizeIntervalCheckBox;
    private JSlider randomizationFactorSlider;
    private JLabel randomizationValueLabel;
    private JCheckBox randomMovementCheckBox;
    private JSpinner movementRadiusSpinner;

    // Hotkey Components
    private JComboBox<String> toggleHotkeyComboBox;
    private JComboBox<String> visibilityHotkeyComboBox;
    private JComboBox<String> pauseHotkeyComboBox;
    private JComboBox<String> increaseSpeedHotkeyComboBox;
    private JComboBox<String> decreaseSpeedHotkeyComboBox;

    // Appearance Components
    private JCheckBox alwaysOnTopCheckBox;
    private JSlider transparencySlider;
    private JLabel transparencyValueLabel;
    private JCheckBox minimizeToTrayCheckBox;
    private JCheckBox autoHideCheckBox;
    private JButton toggleThemeButton;

    // Control Buttons
    private JButton startButton;
    private JButton stopButton;

    // Component caches for theme updates
    private final Map<JComponent, Color> originalBackgrounds = new HashMap<>();
    private final Map<JComponent, Color> originalForegrounds = new HashMap<>();

    // Constants for layout
    private static final int VERTICAL_GAP = 15;
    private static final int HORIZONTAL_GAP = 10;
    private static final int PANEL_PADDING = 15;
    private static final int CARD_PADDING = 12;
    private static final int SECTION_HEADER_PADDING = 8;

    // Constants for component values
    private static final int SLIDER_MIN_CPS = 1;
    private static final int SLIDER_MAX_CPS = 100;
    private static final double SPINNER_MIN_CPS = 0.1;
    private static final double SPINNER_MAX_CPS = 500.0;
    private static final double SPINNER_STEP_CPS = 0.1;

    /**
     * Creates a new ControlPanel.
     *
     * @param clickerService The auto clicker service
     * @param settingsManager The settings manager
     * @param parentFrame The parent main frame
     */
    public ControlPanel(AutoClickerService clickerService, SettingsManager settingsManager, MainFrame parentFrame) {
        this.clickerService = clickerService;
        this.settingsManager = settingsManager;
        this.parentFrame = parentFrame;
        this.themeManager = UIThemeManager.getInstance();

        setLayout(new BorderLayout());
        setBackground(themeManager.getColor("background"));

        // Initialize UI components
        SwingUtilities.invokeLater(() -> {
            initializeComponents();
            loadSettingsToUI();
            applyThemeColors();
        });
    }

    /**
     * Initializes all UI components and adds them to the panel.
     */
    private void initializeComponents() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFocusable(false);

        // Create tab content panels
        JPanel clickSettingsPanel = createClickSettingsPanel();
        JPanel humanizationPanel = createHumanizationPanel();
        JPanel hotkeysPanel = createHotkeysPanel();
        JPanel appearancePanel = createAppearancePanel();
        JPanel platformPanel = new PlatformSettingsPanel(settingsManager, parentFrame);

        // Add tabs
        tabbedPane.addTab("Clicking", createScrollPane(clickSettingsPanel));
        tabbedPane.addTab("Humanization", createScrollPane(humanizationPanel));
        tabbedPane.addTab("Hotkeys", createScrollPane(hotkeysPanel));
        tabbedPane.addTab("Appearance", createScrollPane(appearancePanel));
        tabbedPane.addTab("Platform", createScrollPane(platformPanel));

        // Create and add the control buttons
        JPanel buttonPanel = createControlButtonPanel();

        add(tabbedPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Creates a scroll pane for tab content with consistent styling.
     */
    private JScrollPane createScrollPane(JPanel contentPanel) {
        JScrollPane scrollPane = new JScrollPane();

        // Set the viewport view after scrollPane creation
        scrollPane.setViewportView(contentPanel);

        // Apply styling
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // Set background colors
        Color bgColor = themeManager.getColor("background");
        scrollPane.getViewport().setBackground(bgColor);
        contentPanel.setBackground(bgColor);
        contentPanel.setBorder(new EmptyBorder(PANEL_PADDING, PANEL_PADDING, PANEL_PADDING, PANEL_PADDING));

        return scrollPane;
    }

    /**
     * Creates the click settings panel with speed and options controls.
     */
    private JPanel createClickSettingsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        // Click Speed Section
        panel.add(createSectionHeader("Click Speed"));
        panel.add(createClickSpeedCard());
        panel.add(Box.createVerticalStrut(VERTICAL_GAP));

        // Click Options Section
        panel.add(createSectionHeader("Click Options"));
        panel.add(createClickOptionsCard());

        // macOS Recommendation Card (conditional)
        if (PlatformDetector.isMacOS()) {
            panel.add(Box.createVerticalStrut(VERTICAL_GAP));
            panel.add(createMacOsRecommendationCard());
        }

        // Add flexible spacer at bottom
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel createClickSpeedCard() {
        JPanel card = createCardPanel();
        card.setLayout(new BorderLayout(HORIZONTAL_GAP, VERTICAL_GAP / 2));

        JLabel cpsTitleLabel = new JLabel("Clicks Per Second (CPS)");
        cpsTitleLabel.setFont(UIThemeManager.getInstance().getFont("Dialog"));
        card.add(cpsTitleLabel, BorderLayout.NORTH);

        // Panel for slider and value display similar to randomization panel
        JPanel cpsControlPanel = new JPanel(new BorderLayout(HORIZONTAL_GAP, 0));
        cpsControlPanel.setOpaque(false);
        cpsControlPanel.setBorder(new EmptyBorder(8, 5, 5, 5));

        // Fixed-width label for "Value:"
        JLabel valueLabel = new JLabel("Value:");
        valueLabel.setPreferredSize(new Dimension(70, valueLabel.getPreferredSize().height));
        cpsControlPanel.add(valueLabel, BorderLayout.WEST);

        // Center slider
        cpsSlider = new JSlider(SLIDER_MIN_CPS, SLIDER_MAX_CPS, (int) settingsManager.getCPS());
        cpsSlider.setOpaque(false);
        cpsControlPanel.add(cpsSlider, BorderLayout.CENTER);

        // Panel for spinner on the right
        JPanel spinnerPanel = new JPanel(new BorderLayout());
        spinnerPanel.setOpaque(false);

        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(
                settingsManager.getCPS(), SPINNER_MIN_CPS, SPINNER_MAX_CPS, SPINNER_STEP_CPS);
        cpsSpinner = new JSpinner(spinnerModel);
        cpsSpinner.setBackground(themeManager.getColor("background"));

        styleSpinner(cpsSpinner);
        spinnerPanel.add(cpsSpinner, BorderLayout.CENTER);
        cpsControlPanel.add(spinnerPanel, BorderLayout.EAST);

        card.add(cpsControlPanel, BorderLayout.CENTER);

        // Description label
        JLabel descriptionLabel = new JLabel("<html><p>Adjust how many clicks per second the auto-clicker will perform.</p></html>");
        descriptionLabel.setBorder(new EmptyBorder(5, 10, 5, 10));
        card.add(descriptionLabel, BorderLayout.SOUTH);

        // Sync listeners
        cpsSlider.addChangeListener(e -> {
            if (!cpsSlider.getValueIsAdjusting()) {
                double sliderValue = cpsSlider.getValue();
                if (sliderValue >= SPINNER_MIN_CPS && sliderValue <= SPINNER_MAX_CPS) {
                    if (Math.abs((Double) cpsSpinner.getValue() - sliderValue) > SPINNER_STEP_CPS / 2) {
                        cpsSpinner.setValue(sliderValue);
                    }
                }
                settingsManager.setCPS(sliderValue);
                // Update status bar if clicking is stopped
                if (!clickerService.isRunning() && parentFrame != null) {
                    parentFrame.updateStatusDisplay();
                }
            }
        });
        cpsSpinner.addChangeListener(e -> {
            double spinnerValue = (double) cpsSpinner.getValue();
            settingsManager.setCPS(spinnerValue);
            if (spinnerValue >= SLIDER_MIN_CPS && spinnerValue <= SLIDER_MAX_CPS) {
                if (cpsSlider.getValue() != (int) Math.round(spinnerValue)) {
                    cpsSlider.setValue((int) Math.round(spinnerValue));
                }
            }
            // Update status bar if clicking is stopped
            if (!clickerService.isRunning() && parentFrame != null) {
                parentFrame.updateStatusDisplay();
            }
        });

        return card;
    }

    /**
     * Sets up synchronized behavior between CPS slider and spinner.
     */
    private void setupCpsControls() {
        // Update spinner when slider changes
        cpsSlider.addChangeListener(e -> {
            if (!cpsSlider.getValueIsAdjusting()) {
                double sliderValue = cpsSlider.getValue();
                if (sliderValue >= SPINNER_MIN_CPS && sliderValue <= SPINNER_MAX_CPS) {
                    if (Math.abs((Double) cpsSpinner.getValue() - sliderValue) > SPINNER_STEP_CPS / 2) {
                        cpsSpinner.setValue(sliderValue);
                    }
                }
                settingsManager.setCPS(sliderValue);
                updateStatusIfNeeded();
            }
        });

        // Update slider when spinner changes
        cpsSpinner.addChangeListener(e -> {
            double spinnerValue = (double) cpsSpinner.getValue();
            settingsManager.setCPS(spinnerValue);
            if (spinnerValue >= SLIDER_MIN_CPS && spinnerValue <= SLIDER_MAX_CPS) {
                if (cpsSlider.getValue() != (int) Math.round(spinnerValue)) {
                    cpsSlider.setValue((int) Math.round(spinnerValue));
                }
            }
            updateStatusIfNeeded();
        });
    }

    /**
     * Updates the status display in the parent frame if the auto-clicker is not running.
     */
    private void updateStatusIfNeeded() {
        // Update status bar if clicking is stopped
        if (parentFrame != null && clickerService != null && !clickerService.isRunning()) {
            parentFrame.updateStatusDisplay();
        }
    }

    /**
     * Creates the card for click options (mode and button).
     */
    private JPanel createClickOptionsCard() {
        JPanel card = createCardPanel();
        card.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Constraints
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        gbc.insets = new Insets(0, 0, VERTICAL_GAP / 2, HORIZONTAL_GAP);
        GridBagConstraints gbcControl = (GridBagConstraints) gbc.clone();
        gbcControl.gridx = 1; gbcControl.weightx = 1.0;
        gbcControl.fill = GridBagConstraints.HORIZONTAL;
        gbcControl.insets = new Insets(0, 0, VERTICAL_GAP / 2, 0);

        // Click Mode
        JLabel clickModeLabel = new JLabel("Click Mode:");
        clickModeLabel.setFont(themeManager.getFont("body_regular"));
        clickModeLabel.setForeground(themeManager.getColor("text_primary"));
        card.add(clickModeLabel, gbc);

        // Create click mode combo box
        clickModeComboBox = new JComboBox<>(new String[]{"SINGLE", "DOUBLE", "TRIPLE"});
        clickModeComboBox.setSelectedItem(settingsManager.getClickMode());
        styleComboBox(clickModeComboBox);
        clickModeComboBox.addActionListener(e -> settingsManager.setClickMode((String) clickModeComboBox.getSelectedItem()));
        card.add(clickModeComboBox, gbcControl);

        // Mouse Button
        gbc.gridy++; gbcControl.gridy++;
        JLabel mouseButtonLabel = new JLabel("Mouse Button:");
        mouseButtonLabel.setFont(themeManager.getFont("body_regular"));
        mouseButtonLabel.setForeground(themeManager.getColor("text_primary"));
        card.add(mouseButtonLabel, gbc);

        // Create mouse button combo box
        mouseButtonComboBox = new JComboBox<>(new String[]{"LEFT", "MIDDLE", "RIGHT"});
        mouseButtonComboBox.setSelectedItem(settingsManager.getMouseButton());
        styleComboBox(mouseButtonComboBox);
        mouseButtonComboBox.addActionListener(e -> settingsManager.setMouseButton((String) mouseButtonComboBox.getSelectedItem()));
        card.add(mouseButtonComboBox, gbcControl);

        return card;
    }

    /**
     * Creates a recommendation card for macOS users.
     */
    private JPanel createMacOsRecommendationCard() {
        JPanel card = createInfoCard("macOS Recommendation");
        JLabel noteText = new JLabel(
                "<html><p style='width:95%'>For macOS, a CPS value between <b>5-15</b> works most reliably " +
                        "and helps prevent cursor locking issues when using native events.</p></html>");
        noteText.setFont(themeManager.getFont("small_regular"));
        noteText.setForeground(themeManager.getColor("warning"));
        card.add(noteText, BorderLayout.CENTER);
        return card;
    }

    /**
     * Creates the humanization panel with randomization options.
     */
    private JPanel createHumanizationPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        // Click Timing Section
        panel.add(createSectionHeader("Click Timing Randomization"));
        panel.add(createRandomizeIntervalCard());
        panel.add(Box.createVerticalStrut(VERTICAL_GAP));

        // Mouse Behavior Section
        panel.add(createSectionHeader("Mouse Behavior"));
        panel.add(createMouseMovementCard());

        // macOS Warning
        if (PlatformDetector.isMacOS()) {
            panel.add(Box.createVerticalStrut(VERTICAL_GAP));
            panel.add(createMacOsWarningCard());
        }

        // Add flexible spacer at bottom
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    /**
     * Creates the card for interval randomization controls.
     */
    private JPanel createRandomizeIntervalCard() {
        JPanel card = createCardPanel();
        card.setLayout(new BorderLayout(HORIZONTAL_GAP, VERTICAL_GAP / 2));

        // Create the checkbox with appropriate alignment
        JPanel checkboxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        checkboxPanel.setOpaque(false);

        randomizeIntervalCheckBox = new JCheckBox("Randomize Click Interval");
        randomizeIntervalCheckBox.setFont(themeManager.getFont("heading_small"));
        randomizeIntervalCheckBox.setForeground(themeManager.getColor("text_primary"));
        randomizeIntervalCheckBox.setOpaque(false);
        randomizeIntervalCheckBox.setSelected(settingsManager.isRandomizeInterval());
        UIUtils.styleCheckBox(randomizeIntervalCheckBox, themeManager.getColor("text_primary"));
        checkboxPanel.add(randomizeIntervalCheckBox);

        card.add(checkboxPanel, BorderLayout.NORTH);

        // Panel for the slider and labels
        JPanel randomizationFactorPanel = new JPanel(new BorderLayout(HORIZONTAL_GAP, 0));
        randomizationFactorPanel.setOpaque(false);
        randomizationFactorPanel.setBorder(new EmptyBorder(8, 5, 5, 5));

        // Fixed-width label for "Intensity:"
        JLabel intensityLabel = new JLabel("Intensity:");
        intensityLabel.setPreferredSize(new Dimension(70, intensityLabel.getPreferredSize().height));
        intensityLabel.setFont(themeManager.getFont("body_regular"));
        intensityLabel.setForeground(themeManager.getColor("text_secondary"));
        intensityLabel.setEnabled(settingsManager.isRandomizeInterval());
        randomizationFactorPanel.add(intensityLabel, BorderLayout.WEST);

        // Slider for randomization factor
        randomizationFactorSlider = new JSlider(0, 100, (int) (settingsManager.getRandomizationFactor() * 100));
        randomizationFactorSlider.setOpaque(false);
        randomizationFactorSlider.setEnabled(settingsManager.isRandomizeInterval());
        randomizationFactorPanel.add(randomizationFactorSlider, BorderLayout.CENTER);

        // Label showing randomization percentage
        randomizationValueLabel = new JLabel(String.format("%.0f%%", settingsManager.getRandomizationFactor() * 100));
        randomizationValueLabel.setPreferredSize(new Dimension(50, randomizationValueLabel.getPreferredSize().height));
        randomizationValueLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        randomizationValueLabel.setFont(themeManager.getFont("body_regular"));
        randomizationValueLabel.setForeground(themeManager.getColor("text_secondary"));
        randomizationValueLabel.setEnabled(settingsManager.isRandomizeInterval());
        randomizationFactorPanel.add(randomizationValueLabel, BorderLayout.EAST);

        card.add(randomizationFactorPanel, BorderLayout.CENTER);

        // Description label
        JLabel descriptionLabel = new JLabel(
                "<html><p>Adds randomness to click timing for more natural behavior.</p></html>");
        descriptionLabel.setFont(themeManager.getFont("small_regular"));
        descriptionLabel.setForeground(themeManager.getColor("text_secondary"));
        descriptionLabel.setBorder(new EmptyBorder(5, 10, 5, 10));
        card.add(descriptionLabel, BorderLayout.SOUTH);

        // Set up listeners
        setupRandomizationControls();

        return card;
    }

    /**
     * Sets up listeners for randomization controls.
     */
    private void setupRandomizationControls() {
        // Update label when slider changes
        randomizationFactorSlider.addChangeListener(e -> {
            double value = randomizationFactorSlider.getValue() / 100.0;
            randomizationValueLabel.setText(String.format("%.0f%%", value * 100));
            if (!randomizationFactorSlider.getValueIsAdjusting()) {
                settingsManager.setRandomizationFactor(value);
            }
        });

        // Enable/disable controls when checkbox changes
        randomizeIntervalCheckBox.addActionListener(e -> {
            boolean selected = randomizeIntervalCheckBox.isSelected();
            settingsManager.setRandomizeInterval(selected);
            randomizationFactorSlider.setEnabled(selected);
            randomizationValueLabel.setEnabled(selected);

            // Find the intensity label
            Component parent = randomizationFactorSlider.getParent();
            if (parent instanceof Container) {
                for (Component c : ((Container) parent).getComponents()) {
                    if (c instanceof JLabel && ((JLabel) c).getText().equals("Intensity:")) {
                        c.setEnabled(selected);
                        break;
                    }
                }
            }
        });
    }

    /**
     * Creates the card for mouse movement controls.
     */
    private JPanel createMouseMovementCard() {
        JPanel card = createCardPanel();
        card.setLayout(new BorderLayout(HORIZONTAL_GAP, VERTICAL_GAP / 2));

        // Create the checkbox with appropriate alignment
        JPanel checkboxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        checkboxPanel.setOpaque(false);

        randomMovementCheckBox = new JCheckBox("Add Slight Mouse Movement");
        randomMovementCheckBox.setFont(themeManager.getFont("heading_small"));
        randomMovementCheckBox.setForeground(themeManager.getColor("text_primary"));
        randomMovementCheckBox.setOpaque(false);
        randomMovementCheckBox.setSelected(settingsManager.isRandomMovement());
        UIUtils.styleCheckBox(randomMovementCheckBox, themeManager.getColor("text_primary"));
        checkboxPanel.add(randomMovementCheckBox);

        card.add(checkboxPanel, BorderLayout.NORTH);

        // Create the radius panel with proper layout
        JPanel movementRadiusPanel = new JPanel(new BorderLayout(HORIZONTAL_GAP, 0));
        movementRadiusPanel.setOpaque(false);
        movementRadiusPanel.setBorder(new EmptyBorder(8, 5, 5, 5));

        // Fixed-width label
        JLabel radiusLabel = new JLabel("Max Radius (pixels):");
        radiusLabel.setPreferredSize(new Dimension(140, radiusLabel.getPreferredSize().height));
        radiusLabel.setFont(themeManager.getFont("body_regular"));
        radiusLabel.setForeground(themeManager.getColor("text_secondary"));
        radiusLabel.setEnabled(settingsManager.isRandomMovement());
        movementRadiusPanel.add(radiusLabel, BorderLayout.WEST);

        // Wrapper panel to prevent spinner from filling the entire space
        JPanel spinnerWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        spinnerWrapper.setOpaque(false);

        // Create spinner for radius value
        SpinnerNumberModel movementRadiusModel = new SpinnerNumberModel(
                settingsManager.getMovementRadius(), 1, 20, 1);
        movementRadiusSpinner = new JSpinner(movementRadiusModel);
        movementRadiusSpinner.setEnabled(settingsManager.isRandomMovement());

        // Apply our custom spinner styling
        styleSpinner(movementRadiusSpinner);

        // Set fixed width for better appearance
        Dimension preferredSize = new Dimension(60, movementRadiusSpinner.getPreferredSize().height);
        movementRadiusSpinner.setPreferredSize(preferredSize);

        // Set width for spinner
        JComponent editor = movementRadiusSpinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JFormattedTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
            tf.setColumns(3);
        }

        spinnerWrapper.add(movementRadiusSpinner);
        movementRadiusPanel.add(spinnerWrapper, BorderLayout.CENTER);

        card.add(movementRadiusPanel, BorderLayout.CENTER);

        // Description
        JLabel descriptionLabel = new JLabel(
                "<html><p>Adds small, random mouse offsets before each click. " +
                        "Helps avoid detection in some applications.</p></html>");
        descriptionLabel.setFont(themeManager.getFont("small_regular"));
        descriptionLabel.setForeground(themeManager.getColor("text_secondary"));
        descriptionLabel.setBorder(new EmptyBorder(5, 10, 5, 10));
        card.add(descriptionLabel, BorderLayout.SOUTH);

        // Set up listeners
        setupMouseMovementControls();

        return card;
    }

    /**
     * Sets up listeners for mouse movement controls.
     */
    private void setupMouseMovementControls() {
        // Enable/disable controls when checkbox changes
        randomMovementCheckBox.addActionListener(e -> {
            boolean selected = randomMovementCheckBox.isSelected();
            settingsManager.setRandomMovement(selected);
            movementRadiusSpinner.setEnabled(selected);

            // Find the radius label
            Container parent = movementRadiusSpinner.getParent();
            while (parent != null && !(parent instanceof JPanel && parent.getLayout() instanceof BorderLayout)) {
                parent = parent.getParent();
            }

            if (parent != null) {
                for (Component c : ((Container) parent).getComponents()) {
                    if (c instanceof JLabel && ((JLabel) c).getText().equals("Max Radius (pixels):")) {
                        c.setEnabled(selected);
                        break;
                    }
                }
            }
        });

        // Update settings when spinner changes
        movementRadiusSpinner.addChangeListener(e ->
                settingsManager.setMovementRadius((int) movementRadiusSpinner.getValue()));
    }

    /**
     * Creates a warning card for macOS users.
     */
    private JPanel createMacOsWarningCard() {
        JPanel card = createInfoCard("macOS Compatibility Note");
        JLabel warningText = new JLabel(
                "<html><p style='width:95%'>On macOS, the 'Slight Mouse Movement' feature might occasionally " +
                        "interfere with native event posting, potentially causing minor cursor jumps. " +
                        "If issues arise, disable this feature.</p></html>");
        warningText.setFont(themeManager.getFont("small_regular"));
        warningText.setForeground(themeManager.getColor("warning"));
        card.add(warningText, BorderLayout.CENTER);
        return card;
    }

    /**
     * Creates the hotkeys panel with global hotkey settings.
     */
    private JPanel createHotkeysPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        // Global Hotkeys Section
        panel.add(createSectionHeader("Global Hotkeys"));
        panel.add(createHotkeysCard());
        panel.add(Box.createVerticalStrut(VERTICAL_GAP));

        // Information Section
        panel.add(createSectionHeader("Information"));
        panel.add(createHotkeyInfoCard());

        // Add flexible spacer at bottom
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    /**
     * Creates the card containing all hotkey configuration options.
     */
    private JPanel createHotkeysCard() {
        JPanel card = createCardPanel();
        card.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Constraints for labels
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        gbc.insets = new Insets(0, 0, VERTICAL_GAP / 2, HORIZONTAL_GAP);

        // Constraints for controls (ComboBoxes)
        GridBagConstraints gbcControl = (GridBagConstraints) gbc.clone();
        gbcControl.gridx = 1; gbcControl.weightx = 1.0;
        gbcControl.fill = GridBagConstraints.HORIZONTAL;
        gbcControl.insets = new Insets(0, 0, VERTICAL_GAP / 2, 0);

        // --- Add Hotkey Controls ---

        // Toggle Hotkey
        JLabel toggleLabel = new JLabel("Start/Stop Hotkey:");
        toggleLabel.setFont(themeManager.getFont("body_regular"));
        toggleLabel.setForeground(themeManager.getColor("text_primary"));
        card.add(toggleLabel, gbc);

        toggleHotkeyComboBox = createHotkeyComboBox(settingsManager.getToggleHotkey(),
                key -> settingsManager.setToggleHotkey(key));
        card.add(toggleHotkeyComboBox, gbcControl);

        // Visibility Hotkey
        gbc.gridy++; gbcControl.gridy++;
        JLabel visibilityLabel = new JLabel("Show/Hide Hotkey:");
        visibilityLabel.setFont(themeManager.getFont("body_regular"));
        visibilityLabel.setForeground(themeManager.getColor("text_primary"));
        card.add(visibilityLabel, gbc);

        visibilityHotkeyComboBox = createHotkeyComboBox(settingsManager.getVisibilityHotkey(),
                key -> settingsManager.setVisibilityHotkey(key));
        card.add(visibilityHotkeyComboBox, gbcControl);

        // Pause/Resume Hotkey
        gbc.gridy++; gbcControl.gridy++;
        JLabel pauseLabel = new JLabel("Pause/Resume Hotkey:");
        pauseLabel.setFont(themeManager.getFont("body_regular"));
        pauseLabel.setForeground(themeManager.getColor("text_primary"));
        card.add(pauseLabel, gbc);

        pauseHotkeyComboBox = createHotkeyComboBox(settingsManager.getPauseHotkey(),
                key -> settingsManager.setPauseHotkey(key));
        card.add(pauseHotkeyComboBox, gbcControl);

        // Increase Speed Hotkey
        gbc.gridy++; gbcControl.gridy++;
        JLabel increaseSpeedLabel = new JLabel("Increase Speed Hotkey:");
        increaseSpeedLabel.setFont(themeManager.getFont("body_regular"));
        increaseSpeedLabel.setForeground(themeManager.getColor("text_primary"));
        card.add(increaseSpeedLabel, gbc);

        increaseSpeedHotkeyComboBox = createHotkeyComboBox(settingsManager.getIncreaseSpeedHotkey(),
                key -> settingsManager.setIncreaseSpeedHotkey(key));
        card.add(increaseSpeedHotkeyComboBox, gbcControl);

        // Decrease Speed Hotkey
        gbc.gridy++; gbcControl.gridy++;
        JLabel decreaseSpeedLabel = new JLabel("Decrease Speed Hotkey:");
        decreaseSpeedLabel.setFont(themeManager.getFont("body_regular"));
        decreaseSpeedLabel.setForeground(themeManager.getColor("text_primary"));
        card.add(decreaseSpeedLabel, gbc);

        decreaseSpeedHotkeyComboBox = createHotkeyComboBox(settingsManager.getDecreaseSpeedHotkey(),
                key -> settingsManager.setDecreaseSpeedHotkey(key));
        card.add(decreaseSpeedHotkeyComboBox, gbcControl);

        return card;
    }

    /**
     * Helper method to create and configure a hotkey combo box.
     */
    private JComboBox<String> createHotkeyComboBox(String initialValue, java.util.function.Consumer<String> saveAction) {
        // Create the combo box
        JComboBox<String> comboBox = new JComboBox<>();

        // Delay populating items until the component is fully initialized
        SwingUtilities.invokeLater(() -> {
            if (parentFrame != null && parentFrame.getKeybindManager() != null) {
                String[] items = parentFrame.getKeybindManager().getAvailableKeyNames();
                if (items != null && items.length > 0) {
                    DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(items);
                    comboBox.setModel(model);

                    // Select initial value if valid
                    if (initialValue != null && comboBox.getItemCount() > 0) {
                        comboBox.setSelectedItem(initialValue);
                    }
                }
            }
        });

        // Style the combo box
        styleComboBox(comboBox);

        // Add action listener
        comboBox.addActionListener(e -> {
            String selectedHotkey = (String) comboBox.getSelectedItem();
            if (selectedHotkey != null) {
                saveAction.accept(selectedHotkey);

                // Refresh keybind registrations if applicable
                if (parentFrame != null && parentFrame.getKeybindManager() != null) {
                    parentFrame.refreshKeybinds();
                }
            }
        });

        return comboBox;
    }

    /**
     * Creates an information card for hotkey usage.
     */
    private JPanel createHotkeyInfoCard() {
        JPanel card = createInfoCard("Hotkey Information");

        String infoHTML = "<html><ul style='margin-left: 15px; list-style-type: disc; padding-left: 10px;'>" +
                "<li>Hotkeys work globally, even when the app is minimized or inactive.</li>" +
                "<li>Changes take effect immediately.</li>" +
                "<li>If a hotkey conflicts with another application, choose a different key.</li>" +
                "<li>Speed adjustment hotkeys change CPS by 1.0 per press.</li>" +
                "</ul></html>";
        JLabel infoText = new JLabel(infoHTML);
        infoText.setFont(themeManager.getFont("body_regular"));
        infoText.setForeground(themeManager.getColor("text_primary"));
        card.add(infoText, BorderLayout.CENTER);

        return card;
    }

    /**
     * Creates the appearance panel with window and theme settings.
     */
    private JPanel createAppearancePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        // Window Settings Section
        panel.add(createSectionHeader("Window Behavior"));
        panel.add(createWindowOptionsCard());
        panel.add(Box.createVerticalStrut(VERTICAL_GAP));

        // Appearance Section
        panel.add(createSectionHeader("Visuals"));
        panel.add(createTransparencyCard());
        panel.add(Box.createVerticalStrut(VERTICAL_GAP));
        panel.add(createThemeToggleCard());

        // Add flexible spacer at bottom
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    /**
     * Creates the card with window behavior options.
     */
    private JPanel createWindowOptionsCard() {
        JPanel card = createCardPanel();
        card.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Constraints
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, VERTICAL_GAP / 3, 0);

        // Always On Top checkbox
        alwaysOnTopCheckBox = new JCheckBox("Always On Top");
        alwaysOnTopCheckBox.setFont(themeManager.getFont("body_regular"));
        alwaysOnTopCheckBox.setForeground(themeManager.getColor("text_primary"));
        alwaysOnTopCheckBox.setOpaque(false);
        alwaysOnTopCheckBox.setSelected(settingsManager.isAlwaysOnTop());
        UIUtils.styleCheckBox(alwaysOnTopCheckBox, themeManager.getColor("text_primary"));
        alwaysOnTopCheckBox.addActionListener(e -> {
            boolean selected = alwaysOnTopCheckBox.isSelected();
            settingsManager.setAlwaysOnTop(selected);
            if (parentFrame != null) {
                parentFrame.setAlwaysOnTop(selected);
            }
        });
        card.add(alwaysOnTopCheckBox, gbc);

        // Auto Hide checkbox
        gbc.gridy++;
        autoHideCheckBox = new JCheckBox("Auto-Hide Window (Show on Top Edge Hover)");
        autoHideCheckBox.setFont(themeManager.getFont("body_regular"));
        autoHideCheckBox.setForeground(themeManager.getColor("text_primary"));
        autoHideCheckBox.setOpaque(false);
        autoHideCheckBox.setSelected(settingsManager.isAutoHide());
        UIUtils.styleCheckBox(autoHideCheckBox, themeManager.getColor("text_primary"));
        autoHideCheckBox.setToolTipText("Hides the window automatically; reappears when the mouse touches the top screen edge.");
        autoHideCheckBox.addActionListener(e -> {
            boolean selected = autoHideCheckBox.isSelected();
            settingsManager.setAutoHide(selected);
            if (parentFrame != null) {
                parentFrame.setAutoHide(selected);
            }
        });
        card.add(autoHideCheckBox, gbc);

        // Minimize to Tray checkbox
        gbc.gridy++;
        minimizeToTrayCheckBox = new JCheckBox("Minimize to System Tray");
        minimizeToTrayCheckBox.setFont(themeManager.getFont("body_regular"));
        minimizeToTrayCheckBox.setForeground(themeManager.getColor("text_primary"));
        minimizeToTrayCheckBox.setOpaque(false);
        minimizeToTrayCheckBox.setSelected(settingsManager.isMinimizeToTray());
        UIUtils.styleCheckBox(minimizeToTrayCheckBox, themeManager.getColor("text_primary"));
        minimizeToTrayCheckBox.setToolTipText("Requires System Tray support to be enabled in the application.");
        minimizeToTrayCheckBox.addActionListener(e ->
                settingsManager.setMinimizeToTray(minimizeToTrayCheckBox.isSelected()));
        card.add(minimizeToTrayCheckBox, gbc);

        return card;
    }

    /**
     * Creates the card for window transparency controls.
     */
    private JPanel createTransparencyCard() {
        JPanel card = createCardPanel();
        card.setLayout(new BorderLayout(HORIZONTAL_GAP, 0));

        JLabel transparencyLabel = new JLabel("Window Transparency:");
        transparencyLabel.setFont(themeManager.getFont("heading_small"));
        transparencyLabel.setForeground(themeManager.getColor("text_primary"));
        card.add(transparencyLabel, BorderLayout.NORTH);

        JPanel sliderPanel = new JPanel(new BorderLayout(HORIZONTAL_GAP, 0));
        sliderPanel.setOpaque(false);
        sliderPanel.setBorder(new EmptyBorder(8, 0, 0, 0));

        // Transparency slider
        transparencySlider = new JSlider(20, 100, (int) (settingsManager.getTransparency() * 100));
        transparencySlider.setOpaque(false);
        sliderPanel.add(transparencySlider, BorderLayout.CENTER);

        // Value label
        transparencyValueLabel = new JLabel(String.format("%.0f%%", settingsManager.getTransparency() * 100));
        transparencyValueLabel.setPreferredSize(new Dimension(50, transparencyValueLabel.getPreferredSize().height));
        transparencyValueLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        transparencyValueLabel.setFont(themeManager.getFont("body_regular"));
        transparencyValueLabel.setForeground(themeManager.getColor("text_secondary"));
        sliderPanel.add(transparencyValueLabel, BorderLayout.EAST);

        // Set up listener for transparency slider
        transparencySlider.addChangeListener(e -> {
            double value = transparencySlider.getValue() / 100.0;
            transparencyValueLabel.setText(String.format("%.0f%%", value * 100));
            if (!transparencySlider.getValueIsAdjusting()) {
                settingsManager.setTransparency(value);
                if (parentFrame != null) {
                    parentFrame.setWindowTransparency(value);
                }
            }
        });

        card.add(sliderPanel, BorderLayout.CENTER);
        return card;
    }

    /**
     * Creates the card with the theme toggle button.
     */
    private JPanel createThemeToggleCard() {
        JPanel card = createCardPanel();
        card.setLayout(new FlowLayout(FlowLayout.CENTER));

        // Theme toggle button
        toggleThemeButton = new JButton(parentFrame != null && parentFrame.isDarkMode()
                ? "Switch to Light Theme" : "Switch to Dark Theme");

        // Apply button styling
        UIUtils.styleButton(toggleThemeButton,
                themeManager.getColor("primary"),
                parentFrame != null ? parentFrame.isDarkMode() : themeManager.isDarkMode());

        // Add action listener
        toggleThemeButton.addActionListener(e -> {
            if (parentFrame != null) {
                parentFrame.toggleTheme();
                toggleThemeButton.setText(parentFrame.isDarkMode() ?
                        "Switch to Light Theme" : "Switch to Dark Theme");
            }
        });

        card.add(toggleThemeButton);
        return card;
    }

    /**
     * Creates the control button panel with start/stop buttons.
     */
    private JPanel createControlButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, HORIZONTAL_GAP * 2, 0));
        panel.setBorder(new EmptyBorder(VERTICAL_GAP, 0, VERTICAL_GAP, 0));
        panel.setOpaque(false);

        // Start button
        startButton = UIUtils.createStyledButton("Start Clicking",
                themeManager.getColor("success"),
                themeManager.isDarkMode());
        startButton.addActionListener(e -> {
            clickerService.startClicking();
            if (parentFrame != null) {
                parentFrame.updateStatusDisplay();
            }
        });

        // Stop button
        stopButton = UIUtils.createStyledButton("Stop Clicking",
                themeManager.getColor("danger"),
                themeManager.isDarkMode());
        stopButton.addActionListener(e -> {
            clickerService.stopClicking();
            if (parentFrame != null) {
                parentFrame.updateStatusDisplay();
            }
        });

        panel.add(startButton);
        panel.add(stopButton);

        return panel;
    }

    // --- UI Helper Methods ---

    /**
     * Creates a section header label with consistent styling.
     */
    private JLabel createSectionHeader(String text) {
        JLabel header = new JLabel(text);
        header.setFont(themeManager.getFont("heading_medium"));
        header.setForeground(themeManager.getColor("text_primary"));
        header.setBorder(new EmptyBorder(SECTION_HEADER_PADDING, SECTION_HEADER_PADDING,
                SECTION_HEADER_PADDING/2, SECTION_HEADER_PADDING));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        return header;
    }

    /**
     * Creates a card panel with consistent styling.
     */
    private JPanel createCardPanel() {
        JPanel card = new JPanel();

        // Apply card styling
        card.setBackground(themeManager.getColor("card"));

        // Set border with padding
        Border paddingBorder = new EmptyBorder(CARD_PADDING, CARD_PADDING, CARD_PADDING, CARD_PADDING);
        Border lineBorder = BorderFactory.createLineBorder(themeManager.getColor("border"), 1);
        card.setBorder(new CompoundBorder(lineBorder, paddingBorder));

        // Set maximum width and alignment for consistent layout
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getMaximumSize().height));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        return card;
    }

    /**
     * Creates an information card with a title.
     */
    private JPanel createInfoCard(String title) {
        JPanel card = createCardPanel();
        card.setLayout(new BorderLayout(0, VERTICAL_GAP / 2));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(themeManager.getFont("heading_small"));
        titleLabel.setForeground(themeManager.getColor("warning")); // Use warning color for attention
        titleLabel.setBorder(new EmptyBorder(0, 0, 5, 0));

        card.add(titleLabel, BorderLayout.NORTH);
        return card;
    }

    /**
     * Applies consistent styling to a spinner.
     */
    private void styleSpinner(JSpinner spinner) {
        // Get proper theme colors
        Color bg = themeManager.getColor("card");
        Color fg = themeManager.getColor("text_primary");
        Color borderColor = themeManager.getColor("border");

        // Style the spinner and its text field
        UIUtils.styleSpinner(spinner, bg, fg, borderColor);

        // Ensure the text field has proper colors
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JFormattedTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
            textField.setBackground(bg);
            textField.setForeground(fg);
            textField.setBorder(BorderFactory.createLineBorder(borderColor));
        }
    }

    /**
     * Applies consistent styling to a combo box.
     */
    private void styleComboBox(JComboBox<String> comboBox) {
        UIUtils.styleComboBox(comboBox,
                themeManager.getColor("input_background"),
                themeManager.getColor("text_primary"),
                themeManager.getColor("border"),
                themeManager.getColor("selection_background"),
                themeManager.getColor("selection_foreground"));
    }

    /**
     * Applies theme colors to all components.
     */
    public void applyThemeColors() {
        // Set background for the main panel
        setBackground(themeManager.getColor("background"));

        // Update all components with the current theme
        updateComponentTheme(this);

        // Refresh button styling
        if (startButton != null) {
            UIUtils.styleButton(startButton, themeManager.getColor("success"),
                    themeManager.isDarkMode());
        }

        if (stopButton != null) {
            UIUtils.styleButton(stopButton, themeManager.getColor("danger"),
                    themeManager.isDarkMode());
        }

        if (toggleThemeButton != null) {
            toggleThemeButton.setText(parentFrame != null && parentFrame.isDarkMode()
                    ? "Switch to Light Theme" : "Switch to Dark Theme");
            UIUtils.styleButton(toggleThemeButton, themeManager.getColor("primary"),
                    themeManager.isDarkMode());
        }

        // Ensure UI is refreshed
        revalidate();
        repaint();
    }

    /**
     * Recursively updates theme colors for all components.
     */
    private void updateComponentTheme(Container container) {
        // Get theme colors
        Color bgColor = themeManager.getColor("background");
        Color cardBgColor = themeManager.getColor("card");
        Color borderColor = themeManager.getColor("border");
        Color textPrimary = themeManager.getColor("text_primary");
        Color textSecondary = themeManager.getColor("text_secondary");

        // Update each component in the container
        for (Component comp : container.getComponents()) {
            // Handle specific component types
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;

                // Check if this looks like a card panel
                if (panel.getBorder() instanceof CompoundBorder) {
                    // Update card background and border
                    panel.setBackground(cardBgColor);

                    // Set border with updated colors
                    Border paddingBorder = new EmptyBorder(CARD_PADDING, CARD_PADDING, CARD_PADDING, CARD_PADDING);
                    Border lineBorder = BorderFactory.createLineBorder(borderColor, 1);
                    panel.setBorder(new CompoundBorder(lineBorder, paddingBorder));
                } else if (panel.isOpaque()) {
                    // Update regular panel background
                    panel.setBackground(bgColor);
                }
            } else if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;

                // Check if it's a section header
                if (label.getFont().equals(themeManager.getFont("heading_medium")) ||
                        label.getFont().equals(themeManager.getFont("heading_small"))) {
                    label.setForeground(textPrimary);
                } else if (label.getText() != null && label.getText().contains("<html>")) {
                    // HTML content is usually description or info text
                    label.setForeground(textSecondary);
                } else if (comp == randomizationValueLabel || comp == transparencyValueLabel) {
                    // Value labels use secondary text color
                    label.setForeground(textSecondary);
                } else {
                    // Regular labels use primary text color
                    label.setForeground(textPrimary);
                }
            } else if (comp instanceof JCheckBox) {
                JCheckBox checkBox = (JCheckBox) comp;
                checkBox.setForeground(textPrimary);
                UIUtils.styleCheckBox(checkBox, textPrimary);
            } else if (comp instanceof JSlider) {
                JSlider slider = (JSlider) comp;
                slider.setOpaque(false);
            } else if (comp instanceof JSpinner) {
                // Use our custom spinner styling that properly sets colors
                styleSpinner((JSpinner) comp);
            } else if (comp instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) comp;
                scrollPane.getViewport().setBackground(bgColor);
                scrollPane.setBorder(BorderFactory.createEmptyBorder());
            } else if (comp instanceof JTabbedPane) {
                JTabbedPane tabbedPane = (JTabbedPane) comp;
                themeManager.styleTabbedPane(tabbedPane);
            }

            // Recursively update child containers
            if (comp instanceof Container && !(comp instanceof JButton)) {
                updateComponentTheme((Container) comp);
            }
        }
    }

    /**
     * Loads settings from the SettingsManager into the UI components.
     */
    public void loadSettingsToUI() {
        // Ensure we operate on the EDT
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::loadSettingsToUI);
            return;
        }

        // Check if components are initialized
        if (cpsSpinner == null || clickModeComboBox == null) {
            SwingUtilities.invokeLater(this::loadSettingsToUI);
            return;
        }

        try {
            // Click Settings
            double currentCps = settingsManager.getCPS();
            cpsSpinner.setValue(currentCps);

            // Style the spinner immediately after setting its value
            styleSpinner(cpsSpinner);

            if (currentCps >= SLIDER_MIN_CPS && currentCps <= SLIDER_MAX_CPS) {
                cpsSlider.setValue((int) Math.round(currentCps));
            } else {
                cpsSlider.setValue(currentCps < SLIDER_MIN_CPS ? SLIDER_MIN_CPS : SLIDER_MAX_CPS);
            }

            // Set combo box selections
            clickModeComboBox.setSelectedItem(settingsManager.getClickMode());
            mouseButtonComboBox.setSelectedItem(settingsManager.getMouseButton());

            // Humanization
            boolean isRandomInterval = settingsManager.isRandomizeInterval();
            randomizeIntervalCheckBox.setSelected(isRandomInterval);
            randomizationFactorSlider.setValue((int) (settingsManager.getRandomizationFactor() * 100));
            randomizationFactorSlider.setEnabled(isRandomInterval);
            randomizationValueLabel.setText(String.format("%.0f%%", settingsManager.getRandomizationFactor() * 100));
            randomizationValueLabel.setEnabled(isRandomInterval);

            // Find and update "Intensity:" label state
            updateLabelState(randomizationFactorSlider.getParent(), "Intensity:", isRandomInterval);

            // Random movement
            boolean isRandomMove = settingsManager.isRandomMovement();
            randomMovementCheckBox.setSelected(isRandomMove);
            movementRadiusSpinner.setValue(settingsManager.getMovementRadius());
            movementRadiusSpinner.setEnabled(isRandomMove);

            // Style the radius spinner
            styleSpinner(movementRadiusSpinner);

            // Find and update "Max Radius (pixels):" label state
            Container parent = getParentContainer(movementRadiusSpinner);
            if (parent != null) {
                updateLabelState(parent, "Max Radius (pixels):", isRandomMove);
            }

            // Hotkeys - Apply on another invokeLater to ensure combo boxes are initialized
            SwingUtilities.invokeLater(() -> {
                if (parentFrame != null && parentFrame.getKeybindManager() != null) {
                    // Set hotkey combo box selections
                    safeSetComboBoxItem(toggleHotkeyComboBox, settingsManager.getToggleHotkey());
                    safeSetComboBoxItem(visibilityHotkeyComboBox, settingsManager.getVisibilityHotkey());
                    safeSetComboBoxItem(pauseHotkeyComboBox, settingsManager.getPauseHotkey());
                    safeSetComboBoxItem(increaseSpeedHotkeyComboBox, settingsManager.getIncreaseSpeedHotkey());
                    safeSetComboBoxItem(decreaseSpeedHotkeyComboBox, settingsManager.getDecreaseSpeedHotkey());
                }
            });

            // Appearance settings
            safeSetSelected(alwaysOnTopCheckBox, settingsManager.isAlwaysOnTop());
            safeSetSelected(autoHideCheckBox, settingsManager.isAutoHide());
            safeSetSelected(minimizeToTrayCheckBox, settingsManager.isMinimizeToTray());

            if (transparencySlider != null) {
                transparencySlider.setValue((int) (settingsManager.getTransparency() * 100));
            }

            if (transparencyValueLabel != null) {
                transparencyValueLabel.setText(String.format("%.0f%%", settingsManager.getTransparency() * 100));
            }

            // Apply settings to the frame
            if (parentFrame != null) {
                parentFrame.setAlwaysOnTop(settingsManager.isAlwaysOnTop());
                parentFrame.setAutoHide(settingsManager.isAutoHide());
            }

        } catch (Exception e) {
            System.err.println("Error loading settings into UI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gets the appropriate parent container for finding associated labels.
     */
    private Container getParentContainer(Component comp) {
        Container parent = comp.getParent();
        int searchDepth = 0;
        final int MAX_DEPTH = 5; // Prevent infinite recursion

        while (parent != null && searchDepth < MAX_DEPTH) {
            if (parent instanceof JPanel && parent.getLayout() instanceof BorderLayout) {
                return parent;
            }
            parent = parent.getParent();
            searchDepth++;
        }

        return null;
    }

    /**
     * Updates the enabled state of a label with specific text.
     */
    private void updateLabelState(Container container, String labelText, boolean enabled) {
        if (container == null || labelText == null) return;

        for (Component comp : container.getComponents()) {
            if (comp instanceof JLabel && Objects.equals(labelText, ((JLabel) comp).getText())) {
                comp.setEnabled(enabled);
                return;
            }
        }
    }

    /**
     * Safely sets a combo box selection, handling null values and validation.
     */
    private void safeSetComboBoxItem(JComboBox<String> comboBox, String item) {
        if (comboBox != null && item != null) {
            // Check if the item exists in the combo box
            boolean validItem = false;
            for (int i = 0; i < comboBox.getItemCount(); i++) {
                if (item.equals(comboBox.getItemAt(i))) {
                    validItem = true;
                    break;
                }
            }

            if (validItem) {
                comboBox.setSelectedItem(item);
            }
        }
    }

    /**
     * Safely sets a checkbox selection, handling null values.
     */
    private void safeSetSelected(JCheckBox checkBox, boolean selected) {
        if (checkBox != null) {
            checkBox.setSelected(selected);
        }
    }
}