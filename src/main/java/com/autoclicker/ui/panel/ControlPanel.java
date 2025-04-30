package com.autoclicker.ui.panel;

import com.autoclicker.application.YakClickerApp;
import com.autoclicker.service.click.AutoClickerService;
import com.autoclicker.service.keybind.KeybindManager;
import com.autoclicker.storage.SettingsManager;
import com.autoclicker.ui.frame.MainFrame;
import com.autoclicker.util.PlatformDetector;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * Control panel for the AutoClicker application, providing settings and controls.
 * Redesigned with a modern UI using consistent styling for smooth appearance.
 * Updated to include controls for all hotkeys managed by KeybindManager.
 */
public class ControlPanel extends JPanel {
    private final AutoClickerService clickerService;
    private final SettingsManager settingsManager;
    private final MainFrame parentFrame;

    // UI Components
    private JButton startButton;
    private JButton stopButton;
    private JSlider cpsSlider;
    private JSpinner cpsSpinner;
    private JComboBox<String> clickModeComboBox;
    private JComboBox<String> mouseButtonComboBox;
    private JCheckBox randomizeIntervalCheckBox;
    private JSlider randomizationFactorSlider;
    private JLabel randomizationValueLabel;
    private JCheckBox randomMovementCheckBox;
    private JSpinner movementRadiusSpinner;
    // Hotkey Components
    private JComboBox<String> toggleHotkeyComboBox;
    private JComboBox<String> visibilityHotkeyComboBox;
    private JComboBox<String> pauseHotkeyComboBox; // New
    private JComboBox<String> increaseSpeedHotkeyComboBox; // New
    private JComboBox<String> decreaseSpeedHotkeyComboBox; // New
    // Appearance Components
    private JCheckBox alwaysOnTopCheckBox;
    private JSlider transparencySlider;
    private JLabel transparencyValueLabel;
    private JCheckBox minimizeToTrayCheckBox;
    private JCheckBox autoHideCheckBox;
    private JButton toggleThemeButton;

    // Constants for layout
    private static final int VERTICAL_GAP = 15;
    private static final int HORIZONTAL_GAP = 10;
    private static final int PANEL_PADDING = 15;
    private static final int CARD_INTERNAL_PADDING = 12;
    private static final int SECTION_HEADER_PADDING = 10;

    // Fonts
    private static final Font SECTION_HEADER_FONT = new Font("Arial", Font.BOLD, 14);
    private static final Font CARD_TITLE_FONT = new Font("Arial", Font.BOLD, 13);
    private static final Font DEFAULT_FONT = new Font("Arial", Font.PLAIN, 13);
    private static final Font SMALL_ITALIC_FONT = new Font("Arial", Font.ITALIC, 11);
    private static final Font BUTTON_FONT = new Font("Arial", Font.BOLD, 13);

    // Constants for component values
    private static final int SLIDER_MIN_CPS = 1;
    private static final int SLIDER_MAX_CPS = 100;
    private static final double SPINNER_MIN_CPS = 0.1;
    private static final double SPINNER_MAX_CPS = 500.0;
    private static final double SPINNER_STEP_CPS = 0.1;

    public ControlPanel(AutoClickerService clickerService, SettingsManager settingsManager, MainFrame parentFrame) {
        this.clickerService = clickerService;
        this.settingsManager = settingsManager;
        this.parentFrame = parentFrame;
        setLayout(new BorderLayout());
        if (parentFrame != null) {
            setBackground(parentFrame.getBackgroundColor());
        } else {
            setBackground(UIManager.getColor("Panel.background"));
        }
        initializeComponents();
        loadSettingsToUI();
        applyThemeColors();
    }

    private void initializeComponents() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFocusable(false);
        // Style applied in applyThemeColors

        JPanel clickSettingsPanel = createClickSettingsPanel();
        JPanel humanizationPanel = createHumanizationPanel();
        JPanel hotkeysPanel = createHotkeysPanel();
        JPanel appearancePanel = createAppearancePanel();
        JPanel platformPanel = new PlatformSettingsPanel(settingsManager, parentFrame);

        tabbedPane.addTab("Clicking", createScrollPane(clickSettingsPanel));
        tabbedPane.addTab("Humanization", createScrollPane(humanizationPanel));
        tabbedPane.addTab("Hotkeys", createScrollPane(hotkeysPanel));
        tabbedPane.addTab("Appearance", createScrollPane(appearancePanel));
        tabbedPane.addTab("Platform", createScrollPane(platformPanel));

        JPanel buttonPanel = createControlButtonPanel();

        add(tabbedPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /** Wraps a content panel in a JScrollPane, styled manually. */
    private JScrollPane createScrollPane(JPanel contentPanel) {
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        Color bgColor = (parentFrame != null) ? parentFrame.getBackgroundColor() : UIManager.getColor("Panel.background");
        scrollPane.getViewport().setBackground(bgColor);
        contentPanel.setBackground(bgColor);
        contentPanel.setBorder(new EmptyBorder(new Insets(PANEL_PADDING, PANEL_PADDING, PANEL_PADDING, PANEL_PADDING)));

        return scrollPane;
    }

    // --- Click Settings Panel ---
    private JPanel createClickSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Default Constraints
        gbc.gridx = 0; gbc.gridwidth = 1; gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(0, 0, VERTICAL_GAP, 0);

        // Click Speed Section
        gbc.gridy = 0; panel.add(createSectionHeader("Click Speed"), gbc);
        gbc.gridy++; panel.add(createClickSpeedCard(), gbc);

        // Click Options Section
        gbc.gridy++; panel.add(createSectionHeader("Click Options"), gbc);
        gbc.gridy++; panel.add(createClickOptionsCard(), gbc);

        // macOS Recommendation
        if (PlatformDetector.isMacOS()) {
            gbc.gridy++; panel.add(createMacOsRecommendationCard(), gbc);
        }

        // Vertical Glue
        gbc.gridy++; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH;
        panel.add(Box.createVerticalGlue(), gbc);

        return panel;
    }

    private JPanel createClickSpeedCard() {
        JPanel card = createCardPanel();
        card.setLayout(new BorderLayout(HORIZONTAL_GAP, VERTICAL_GAP / 2));

        JLabel cpsTitleLabel = new JLabel("Clicks Per Second (CPS)");
        cpsTitleLabel.setFont(CARD_TITLE_FONT);
        card.add(cpsTitleLabel, BorderLayout.NORTH);

        // Panel for slider and spinner
        JPanel cpsControlPanel = new JPanel(new BorderLayout(HORIZONTAL_GAP, 0));
        cpsControlPanel.setOpaque(false);

        cpsSlider = new JSlider(SLIDER_MIN_CPS, SLIDER_MAX_CPS, (int) settingsManager.getCPS());
        cpsSlider.setOpaque(false);
        cpsControlPanel.add(cpsSlider, BorderLayout.CENTER);

        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(
                settingsManager.getCPS(), SPINNER_MIN_CPS, SPINNER_MAX_CPS, SPINNER_STEP_CPS);
        cpsSpinner = new JSpinner(spinnerModel);
        styleSpinner(cpsSpinner);
        cpsControlPanel.add(cpsSpinner, BorderLayout.EAST);

        card.add(cpsControlPanel, BorderLayout.CENTER);

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
        card.add(clickModeLabel, gbc);
        clickModeComboBox = new JComboBox<>(new String[]{"SINGLE", "DOUBLE", "TRIPLE"});
        clickModeComboBox.setSelectedItem(settingsManager.getClickMode());
        clickModeComboBox.addActionListener(e -> settingsManager.setClickMode((String) clickModeComboBox.getSelectedItem()));
        styleComboBox(clickModeComboBox);
        card.add(clickModeComboBox, gbcControl);

        // Mouse Button
        gbc.gridy++; gbcControl.gridy++;
        JLabel mouseButtonLabel = new JLabel("Mouse Button:");
        card.add(mouseButtonLabel, gbc);
        mouseButtonComboBox = new JComboBox<>(new String[]{"LEFT", "MIDDLE", "RIGHT"});
        mouseButtonComboBox.setSelectedItem(settingsManager.getMouseButton());
        mouseButtonComboBox.addActionListener(e -> settingsManager.setMouseButton((String) mouseButtonComboBox.getSelectedItem()));
        styleComboBox(mouseButtonComboBox);
        card.add(mouseButtonComboBox, gbcControl);

        return card;
    }

    private JPanel createMacOsRecommendationCard() {
        JPanel card = createInfoCard("macOS Recommendation");
        JLabel noteText = new JLabel("<html><p style='width:95%'>For macOS, a CPS value between <b>5-15</b> works most reliably and helps prevent cursor locking issues when using native events.</p></html>");
        noteText.setFont(SMALL_ITALIC_FONT);
        card.add(noteText, BorderLayout.CENTER);
        return card;
    }

    // --- Humanization Panel ---
    private JPanel createHumanizationPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Default Constraints
        gbc.gridx = 0; gbc.gridwidth = 1; gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(0, 0, VERTICAL_GAP, 0);

        // Click Timing Section
        gbc.gridy = 0; panel.add(createSectionHeader("Click Timing Randomization"), gbc);
        gbc.gridy++; panel.add(createRandomizeIntervalCard(), gbc);

        // Mouse Behavior Section
        gbc.gridy++; panel.add(createSectionHeader("Mouse Behavior"), gbc);
        gbc.gridy++; panel.add(createMouseMovementCard(), gbc);

        // macOS Warning
        if (PlatformDetector.isMacOS()) {
            gbc.gridy++; panel.add(createMacOsWarningCard(), gbc);
        }

        // Vertical Glue
        gbc.gridy++; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH;
        panel.add(Box.createVerticalGlue(), gbc);

        return panel;
    }

    private JPanel createRandomizeIntervalCard() {
        JPanel card = createCardPanel();
        card.setLayout(new BorderLayout(HORIZONTAL_GAP, VERTICAL_GAP / 2));

        // Create the checkbox with appropriate alignment
        JPanel checkboxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        checkboxPanel.setOpaque(false);

        randomizeIntervalCheckBox = new JCheckBox("Randomize Click Interval");
        configureCheckBox(randomizeIntervalCheckBox, CARD_TITLE_FONT);
        randomizeIntervalCheckBox.setSelected(settingsManager.isRandomizeInterval());
        checkboxPanel.add(randomizeIntervalCheckBox);

        card.add(checkboxPanel, BorderLayout.NORTH);

        // Panel for the slider and labels using BorderLayout
        JPanel randomizationFactorPanel = new JPanel(new BorderLayout(HORIZONTAL_GAP, 0));
        randomizationFactorPanel.setOpaque(false);
        randomizationFactorPanel.setBorder(new EmptyBorder(8, 5, 5, 5));

        // Fixed-width label for "Intensity:"
        JLabel intensityLabel = new JLabel("Intensity:");
        intensityLabel.setPreferredSize(new Dimension(70, intensityLabel.getPreferredSize().height));
        randomizationFactorPanel.add(intensityLabel, BorderLayout.WEST);

        randomizationFactorSlider = new JSlider(0, 100, (int) (settingsManager.getRandomizationFactor() * 100));
        randomizationFactorSlider.setOpaque(false);
        randomizationFactorSlider.setEnabled(settingsManager.isRandomizeInterval());
        randomizationFactorPanel.add(randomizationFactorSlider, BorderLayout.CENTER);

        randomizationValueLabel = new JLabel(String.format("%.0f%%", settingsManager.getRandomizationFactor() * 100));
        randomizationValueLabel.setPreferredSize(new Dimension(50, randomizationValueLabel.getPreferredSize().height));
        randomizationValueLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        randomizationValueLabel.setEnabled(settingsManager.isRandomizeInterval());
        randomizationFactorPanel.add(randomizationValueLabel, BorderLayout.EAST);

        card.add(randomizationFactorPanel, BorderLayout.CENTER);

        // Description label
        JLabel descriptionLabel = new JLabel("<html><p>Adds randomness to click timing for more natural behavior.</p></html>");
        descriptionLabel.setFont(SMALL_ITALIC_FONT);
        descriptionLabel.setBorder(new EmptyBorder(5, 10, 5, 10));
        card.add(descriptionLabel, BorderLayout.SOUTH);

        // Listeners
        randomizationFactorSlider.addChangeListener(e -> {
            double value = randomizationFactorSlider.getValue() / 100.0;
            randomizationValueLabel.setText(String.format("%.0f%%", value * 100));
            if (!randomizationFactorSlider.getValueIsAdjusting()) {
                settingsManager.setRandomizationFactor(value);
            }
        });

        randomizeIntervalCheckBox.addActionListener(e -> {
            boolean selected = randomizeIntervalCheckBox.isSelected();
            settingsManager.setRandomizeInterval(selected);
            randomizationFactorSlider.setEnabled(selected);
            randomizationValueLabel.setEnabled(selected);
            intensityLabel.setEnabled(selected);
        });

        return card;
    }

    private JPanel createMouseMovementCard() {
        JPanel card = createCardPanel();
        card.setLayout(new BorderLayout(HORIZONTAL_GAP, VERTICAL_GAP / 2));

        // Create the checkbox with appropriate alignment
        JPanel checkboxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        checkboxPanel.setOpaque(false);

        randomMovementCheckBox = new JCheckBox("Add Slight Mouse Movement");
        configureCheckBox(randomMovementCheckBox, CARD_TITLE_FONT);
        randomMovementCheckBox.setSelected(settingsManager.isRandomMovement());
        checkboxPanel.add(randomMovementCheckBox);

        card.add(checkboxPanel, BorderLayout.NORTH);

        // Create the radius panel with proper layout
        JPanel movementRadiusPanel = new JPanel(new BorderLayout(HORIZONTAL_GAP, 0));
        movementRadiusPanel.setOpaque(false);
        movementRadiusPanel.setBorder(new EmptyBorder(8, 5, 5, 5));

        // Fixed-width label
        JLabel radiusLabel = new JLabel("Max Radius (pixels):");
        radiusLabel.setPreferredSize(new Dimension(140, radiusLabel.getPreferredSize().height));
        radiusLabel.setEnabled(randomMovementCheckBox.isSelected());
        movementRadiusPanel.add(radiusLabel, BorderLayout.WEST);

        // Wrapper panel to prevent spinner from filling the entire space
        JPanel spinnerWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        spinnerWrapper.setOpaque(false);

        SpinnerNumberModel movementRadiusModel = new SpinnerNumberModel(
                settingsManager.getMovementRadius(), 1, 20, 1);
        movementRadiusSpinner = new JSpinner(movementRadiusModel);
        movementRadiusSpinner.setEnabled(settingsManager.isRandomMovement());
        styleSpinner(movementRadiusSpinner);
        JComponent editor = movementRadiusSpinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JFormattedTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
            tf.setColumns(3); // Limit the width
        }

        spinnerWrapper.add(movementRadiusSpinner);
        movementRadiusPanel.add(spinnerWrapper, BorderLayout.CENTER);

        card.add(movementRadiusPanel, BorderLayout.CENTER);

        // Description
        JLabel descriptionLabel = new JLabel("<html><p>Adds small, random mouse offsets before each click. Helps avoid detection in some applications.</p></html>");
        descriptionLabel.setFont(SMALL_ITALIC_FONT);
        descriptionLabel.setBorder(new EmptyBorder(5, 10, 5, 10));
        card.add(descriptionLabel, BorderLayout.SOUTH);

        randomMovementCheckBox.addActionListener(e -> {
            boolean selected = randomMovementCheckBox.isSelected();
            settingsManager.setRandomMovement(selected);
            movementRadiusSpinner.setEnabled(selected);
            radiusLabel.setEnabled(selected);
        });

        movementRadiusSpinner.addChangeListener(e -> settingsManager.setMovementRadius((int) movementRadiusSpinner.getValue()));

        return card;
    }

    private JPanel createMacOsWarningCard() {
        JPanel card = createInfoCard("macOS Compatibility Note");
        JLabel warningText = new JLabel("<html><p style='width:95%'>On macOS, the 'Slight Mouse Movement' feature might occasionally interfere with native event posting, potentially causing minor cursor jumps. If issues arise, disable this feature.</p></html>");
        warningText.setFont(SMALL_ITALIC_FONT);
        card.add(warningText, BorderLayout.CENTER);
        return card;
    }

    // --- Hotkeys Panel (Updated) ---
    private JPanel createHotkeysPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Default Constraints
        gbc.gridx = 0; gbc.gridwidth = 1; gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(0, 0, VERTICAL_GAP, 0);

        // Global Hotkeys Section
        gbc.gridy = 0; panel.add(createSectionHeader("Global Hotkeys"), gbc);
        gbc.gridy++; panel.add(createHotkeysCard(), gbc); // Updated card method

        // Information Section
        gbc.gridy++; panel.add(createSectionHeader("Information"), gbc);
        gbc.gridy++; panel.add(createHotkeyInfoCard(), gbc);

        // Vertical Glue
        gbc.gridy++; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH;
        panel.add(Box.createVerticalGlue(), gbc);

        return panel;
    }

    /** Creates the card containing all hotkey configuration options. */
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
        card.add(toggleLabel, gbc);
        toggleHotkeyComboBox = createHotkeyComboBox(settingsManager.getToggleHotkey(), settingsManager::setToggleHotkey);
        card.add(toggleHotkeyComboBox, gbcControl);

        // Visibility Hotkey
        gbc.gridy++; gbcControl.gridy++;
        JLabel visibilityLabel = new JLabel("Show/Hide Hotkey:");
        card.add(visibilityLabel, gbc);
        visibilityHotkeyComboBox = createHotkeyComboBox(settingsManager.getVisibilityHotkey(), settingsManager::setVisibilityHotkey);
        card.add(visibilityHotkeyComboBox, gbcControl);

        // Pause/Resume Hotkey
        gbc.gridy++; gbcControl.gridy++;
        JLabel pauseLabel = new JLabel("Pause/Resume Hotkey:");
        card.add(pauseLabel, gbc);
        pauseHotkeyComboBox = createHotkeyComboBox(settingsManager.getPauseHotkey(), settingsManager::setPauseHotkey);
        card.add(pauseHotkeyComboBox, gbcControl);

        // Increase Speed Hotkey
        gbc.gridy++; gbcControl.gridy++;
        JLabel increaseSpeedLabel = new JLabel("Increase Speed Hotkey:");
        card.add(increaseSpeedLabel, gbc);
        increaseSpeedHotkeyComboBox = createHotkeyComboBox(settingsManager.getIncreaseSpeedHotkey(), settingsManager::setIncreaseSpeedHotkey);
        card.add(increaseSpeedHotkeyComboBox, gbcControl);

        // Decrease Speed Hotkey
        gbc.gridy++; gbcControl.gridy++;
        JLabel decreaseSpeedLabel = new JLabel("Decrease Speed Hotkey:");
        card.add(decreaseSpeedLabel, gbc);
        decreaseSpeedHotkeyComboBox = createHotkeyComboBox(settingsManager.getDecreaseSpeedHotkey(), settingsManager::setDecreaseSpeedHotkey);
        card.add(decreaseSpeedHotkeyComboBox, gbcControl);

        return card;
    }

    /** Helper method to create and configure a hotkey combo box. */
    private JComboBox<String> createHotkeyComboBox(String initialValue, java.util.function.Consumer<String> saveAction) {
        JComboBox<String> comboBox = new JComboBox<>(parentFrame.getKeybindManager().getAvailableKeyNames());
        comboBox.setSelectedItem(initialValue);
        comboBox.addActionListener(e -> {
            String selectedHotkey = (String) comboBox.getSelectedItem();
            saveAction.accept(selectedHotkey); // Use the provided Consumer to save the setting

        });
        styleComboBox(comboBox); // Apply theme styling
        return comboBox;
    }


    private JPanel createHotkeyInfoCard() {
        JPanel card = createInfoCard("Hotkey Information");

        String infoHTML = "<html><ul style='margin-left: 15px; list-style-type: disc; padding-left: 10px;'>" +
                "<li>Hotkeys work globally, even when the app is minimized or inactive.</li>" +
                "<li>Changes take effect immediately.</li>" +
                "<li>If a hotkey conflicts with another application, choose a different key.</li>" +
                "<li>Speed adjustment hotkeys change CPS by 1.0 per press.</li>" +
                "</ul></html>";
        JLabel infoText = new JLabel(infoHTML);
        infoText.setFont(DEFAULT_FONT);
        card.add(infoText, BorderLayout.CENTER);
        return card;
    }

    // --- Appearance Panel ---
    private JPanel createAppearancePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Default Constraints
        gbc.gridx = 0; gbc.gridwidth = 1; gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(0, 0, VERTICAL_GAP, 0);

        // Window Settings Section
        gbc.gridy = 0; panel.add(createSectionHeader("Window Behavior"), gbc);
        gbc.gridy++; panel.add(createWindowOptionsCard(), gbc);

        // Appearance Section
        gbc.gridy++; panel.add(createSectionHeader("Visuals"), gbc);
        gbc.gridy++; panel.add(createTransparencyCard(), gbc);
        gbc.gridy++; panel.add(createThemeToggleCard(), gbc);

        // Vertical Glue
        gbc.gridy++; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH;
        panel.add(Box.createVerticalGlue(), gbc);

        return panel;
    }

    private JPanel createWindowOptionsCard() {
        JPanel card = createCardPanel();
        card.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Constraints
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, VERTICAL_GAP / 3, 0);

        // Always On Top
        alwaysOnTopCheckBox = new JCheckBox("Always On Top");
        configureCheckBox(alwaysOnTopCheckBox, DEFAULT_FONT);
        alwaysOnTopCheckBox.setSelected(settingsManager.isAlwaysOnTop());
        alwaysOnTopCheckBox.addActionListener(e -> {
            boolean selected = alwaysOnTopCheckBox.isSelected();
            settingsManager.setAlwaysOnTop(selected);
            if (parentFrame != null) parentFrame.setAlwaysOnTop(selected);
        });
        card.add(alwaysOnTopCheckBox, gbc);

        // Auto Hide
        gbc.gridy++;
        autoHideCheckBox = new JCheckBox("Auto-Hide Window (Show on Top Edge Hover)");
        configureCheckBox(autoHideCheckBox, DEFAULT_FONT);
        autoHideCheckBox.setSelected(settingsManager.isAutoHide());
        autoHideCheckBox.addActionListener(e -> {
            boolean selected = autoHideCheckBox.isSelected();
            settingsManager.setAutoHide(selected);
            if (parentFrame != null) parentFrame.setAutoHide(selected);
        });
        autoHideCheckBox.setToolTipText("Hides the window automatically; reappears when the mouse touches the top screen edge.");
        card.add(autoHideCheckBox, gbc);

        // Minimize to Tray
        gbc.gridy++;
        minimizeToTrayCheckBox = new JCheckBox("Minimize to System Tray");
        configureCheckBox(minimizeToTrayCheckBox, DEFAULT_FONT);
        minimizeToTrayCheckBox.setSelected(settingsManager.isMinimizeToTray());
        minimizeToTrayCheckBox.addActionListener(e -> settingsManager.setMinimizeToTray(minimizeToTrayCheckBox.isSelected()));
        minimizeToTrayCheckBox.setToolTipText("Requires System Tray support to be enabled in the application.");
        card.add(minimizeToTrayCheckBox, gbc);

        return card;
    }

    private JPanel createTransparencyCard() {
        JPanel card = createCardPanel();
        card.setLayout(new BorderLayout(HORIZONTAL_GAP, 0));

        JLabel transparencyLabel = new JLabel("Window Transparency:");
        transparencyLabel.setFont(CARD_TITLE_FONT);
        card.add(transparencyLabel, BorderLayout.NORTH);

        JPanel sliderPanel = new JPanel(new BorderLayout(HORIZONTAL_GAP, 0));
        sliderPanel.setOpaque(false);
        sliderPanel.setBorder(new EmptyBorder(8, 0, 0, 0));

        transparencySlider = new JSlider(20, 100, (int) (settingsManager.getTransparency() * 100));
        transparencySlider.setOpaque(false);
        sliderPanel.add(transparencySlider, BorderLayout.CENTER);

        transparencyValueLabel = new JLabel(String.format("%.0f%%", settingsManager.getTransparency() * 100));
        transparencyValueLabel.setPreferredSize(new Dimension(50, transparencyValueLabel.getPreferredSize().height));
        transparencyValueLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        sliderPanel.add(transparencyValueLabel, BorderLayout.EAST);

        transparencySlider.addChangeListener(e -> {
            double value = transparencySlider.getValue() / 100.0;
            transparencyValueLabel.setText(String.format("%.0f%%", value * 100));
            if (!transparencySlider.getValueIsAdjusting()) {
                settingsManager.setTransparency(value);
                if (parentFrame != null) parentFrame.setWindowTransparency(value);
            }
        });

        card.add(sliderPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createThemeToggleCard() {
        JPanel card = createCardPanel();
        card.setLayout(new FlowLayout(FlowLayout.CENTER));

        toggleThemeButton = new JButton();
        if (parentFrame != null) {
            toggleThemeButton.setText(parentFrame.isDarkMode() ? "Switch to Light Theme" : "Switch to Dark Theme");
        }

        toggleThemeButton.setFont(BUTTON_FONT);
        toggleThemeButton.addActionListener(e -> {
            if (parentFrame != null) parentFrame.toggleTheme();
        });
        card.add(toggleThemeButton);
        return card;
    }

    // --- Control Button Panel ---
    private JPanel createControlButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, HORIZONTAL_GAP * 2, 0));
        panel.setBorder(new EmptyBorder(VERTICAL_GAP, 0, VERTICAL_GAP, 0));

        startButton = new JButton("Start Clicking");
        startButton.setFont(BUTTON_FONT);
        startButton.addActionListener(e -> {
            clickerService.startClicking();
            if (parentFrame != null) parentFrame.updateStatusDisplay();
        });

        stopButton = new JButton("Stop Clicking");
        stopButton.setFont(BUTTON_FONT);
        stopButton.addActionListener(e -> {
            clickerService.stopClicking();
            if (parentFrame != null) parentFrame.updateStatusDisplay();
        });

        panel.add(startButton);
        panel.add(stopButton);

        return panel;
    }

    // --- UI Helper Methods ---

    private JPanel createCardPanel() {
        JPanel card = new JPanel();
        // Background and border set by applyThemeColors -> updateComponentColors
        return card;
    }

    private JPanel createInfoCard(String title) {
        JPanel card = createCardPanel();
        card.setLayout(new BorderLayout(0, VERTICAL_GAP / 2));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(CARD_TITLE_FONT);
        titleLabel.setBorder(new EmptyBorder(0,0,5,0));
        card.add(titleLabel, BorderLayout.NORTH);
        return card;
    }

    private JLabel createSectionHeader(String text) {
        JLabel header = new JLabel(text);
        header.setFont(SECTION_HEADER_FONT);
        // Reduced vertical padding for section headers
        header.setBorder(new EmptyBorder(new Insets(SECTION_HEADER_PADDING / 2, SECTION_HEADER_PADDING, SECTION_HEADER_PADDING / 2, SECTION_HEADER_PADDING)));
        return header;
    }

    private void styleButton(JButton button, Color color) {
        final Color finalColor = (color == null) ? (parentFrame != null ? parentFrame.getPrimaryColor() : Color.GRAY) : color;
        button.setBackground(finalColor);
        double luminance = (0.299 * finalColor.getRed() + 0.587 * finalColor.getGreen() + 0.114 * finalColor.getBlue()) / 255;
        button.setForeground(luminance > 0.5 ? Color.BLACK : Color.WHITE);
        button.setFont(BUTTON_FONT);
        button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorderPainted(false);
        Color darkerColor = darken(finalColor, 0.1f);

        // Remove old listeners to prevent duplicates
        for (MouseListener ml : button.getMouseListeners()) {
            if (ml instanceof MouseAdapter && ml.getClass().isAnonymousClass()) {
                button.removeMouseListener(ml);
            }
        }

        button.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { button.setBackground(darkerColor); }
            @Override public void mouseExited(MouseEvent e) { button.setBackground(finalColor); }
        });
    }

    private void styleComboBox(JComboBox<String> comboBox) {
        Color comboBg = Color.WHITE;
        Color comboFg = Color.BLACK;
        Color selectionBg = UIManager.getColor("ComboBox.selectionBackground");
        Color selectionFg = UIManager.getColor("ComboBox.selectionForeground");
        Color borderCol = Color.LIGHT_GRAY;
        boolean isDark = false;

        if (parentFrame != null) {
            isDark = parentFrame.isDarkMode();
            comboFg = parentFrame.getTextPrimaryColor();
            comboBg = isDark ? new Color(60, 63, 65) : Color.WHITE; // Use specific dark color
            borderCol = isDark ? parentFrame.getBorderColor() : Color.LIGHT_GRAY; // Use theme border or default
            try { selectionBg = parentFrame.getSelectionBackgroundColor(); } catch (Exception e) { selectionBg = isDark ? parentFrame.getPrimaryColor().darker() : parentFrame.getPrimaryColor(); }
            try { selectionFg = parentFrame.getSelectionForegroundColor(); } catch (Exception e) { selectionFg = isDark ? Color.WHITE : Color.WHITE; }
        }
        comboBox.setBackground(comboBg);
        comboBox.setForeground(comboFg);
        comboBox.setFont(DEFAULT_FONT);
        comboBox.setBorder(BorderFactory.createLineBorder(borderCol));

        Component editorComponent = comboBox.getEditor().getEditorComponent();
        if (editorComponent instanceof JTextField) {
            JTextField tfEditor = (JTextField) editorComponent;
            tfEditor.setForeground(comboFg);
            tfEditor.setBackground(comboBg);
            tfEditor.setBorder(new EmptyBorder(2, 4, 2, 4));
            tfEditor.setOpaque(true);
        }

        Color finalSelectionBg = selectionBg;
        Color finalSelectionFg = selectionFg;
        Color finalComboBg = comboBg;
        Color finalComboFg = comboFg;
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (c instanceof JLabel) {
                    JLabel label = (JLabel) c;
                    label.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

                    if (isSelected) {
                        label.setBackground(finalSelectionBg);
                        label.setForeground(finalSelectionFg);
                    } else {
                        label.setBackground(finalComboBg);
                        label.setForeground(finalComboFg);
                    }
                }

                return c;
            }
        });
    }

    private void styleSpinner(JSpinner spinner) {
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JFormattedTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
            textField.setFont(DEFAULT_FONT);
            textField.setColumns(4);

            Color spinnerFg = Color.BLACK;
            Color spinnerBg = Color.WHITE;
            Color borderCol = Color.LIGHT_GRAY;
            boolean isDark = false;

            if (parentFrame != null) {
                isDark = parentFrame.isDarkMode();
                spinnerFg = parentFrame.getTextPrimaryColor();
                spinnerBg = isDark ? Color.DARK_GRAY : Color.WHITE;
                borderCol = isDark ? parentFrame.getBorderColor() : Color.LIGHT_GRAY;
            }
            textField.setForeground(spinnerFg);
            textField.setBackground(spinnerBg);
            textField.setBorder(BorderFactory.createLineBorder(borderCol));
            textField.setOpaque(true);
        }
    }

    private void configureCheckBox(JCheckBox checkBox, Font font) {
        checkBox.setFont(font);
        if (parentFrame != null) {
            checkBox.setForeground(parentFrame.getTextPrimaryColor());
        } else {
            checkBox.setForeground(Color.BLACK);
        }
        checkBox.setOpaque(false);
    }

    private Color darken(Color color, float fraction) {
        if (color == null) return Color.DARK_GRAY;
        int r = Math.round(Math.max(0, color.getRed() * (1.0f - fraction)));
        int g = Math.round(Math.max(0, color.getGreen() * (1.0f - fraction)));
        int b = Math.round(Math.max(0, color.getBlue() * (1.0f - fraction)));
        return new Color(r, g, b);
    }

    private Color lighten(Color color, float factor) {
        if (color == null) return Color.LIGHT_GRAY;
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        r = Math.min(255, r + Math.round((255 - r) * factor));
        g = Math.min(255, g + Math.round((255 - g) * factor));
        b = Math.min(255, b + Math.round((255 - b) * factor));

        return new Color(r, g, b, color.getAlpha());
    }

    // --- Theme Application ---
    public void applyThemeColors() {
        if (parentFrame == null) {
            System.err.println("Error: parentFrame is null in applyThemeColors(). Cannot apply theme.");
            return;
        }

        setBackground(parentFrame.getBackgroundColor()); // Set background for ControlPanel itself
        // Update components recursively
        updateComponentColors(this);

        // Explicitly update top-level styled components that might not be containers
        if (toggleThemeButton != null) {
            toggleThemeButton.setText(parentFrame.isDarkMode() ? "Switch to Light Theme" : "Switch to Dark Theme");
            styleButton(toggleThemeButton, parentFrame.getPrimaryColor());
        }
        if(startButton != null) styleButton(startButton, parentFrame.getSuccessColor());
        if(stopButton != null) styleButton(stopButton, parentFrame.getDangerColor());

        revalidate();
        repaint();
    }

    private void updateComponentColors(Container container) {
        if (parentFrame == null) return;

        boolean isDark = parentFrame.isDarkMode();
        Color themeBg = parentFrame.getBackgroundColor();
        Color cardBg = parentFrame.getCardBackgroundColor();
        Color borderCol = parentFrame.getBorderColor();
        Color textPrimary = parentFrame.getTextPrimaryColor();
        Color textSecondary = parentFrame.getTextSecondaryColor();
        Color warningColor = parentFrame.getWarningColor();

        for (Component component : container.getComponents()) {
            if (component instanceof JLabel) {
                JLabel label = (JLabel) component;
                // Handle specific labels first
                if (label == randomizationValueLabel || label == transparencyValueLabel) {
                    label.setForeground(textSecondary);
                } else if (label.getFont().equals(SECTION_HEADER_FONT) || label.getFont().equals(CARD_TITLE_FONT)) {
                    label.setForeground(textPrimary);
                } else if (label.getForeground().equals(warningColor) || label.getForeground().equals(Color.ORANGE)){ // Check for warning color
                    label.setForeground(warningColor);
                } else { // General labels
                    if (label.getFont().equals(SMALL_ITALIC_FONT) || label.getText().contains("<html>")) {
                        label.setForeground(textSecondary); // Assume html or italic is secondary info
                    } else {
                        label.setForeground(textPrimary);
                    }
                }
            } else if (component instanceof JCheckBox) {
                configureCheckBox((JCheckBox)component, component.getFont()); // Re-apply checkbox styling
            } else if (component instanceof JComboBox) {
                styleComboBox((JComboBox<String>) component); // Re-apply combo box styling
            } else if (component instanceof JSpinner) {
                styleSpinner((JSpinner) component); // Re-apply spinner styling
            } else if (component instanceof JPanel) {
                JPanel panel = (JPanel) component;
                // Check if it's likely a card panel we created
                // Check if border is null before attempting to get its class
                Border currentBorder = panel.getBorder();
                boolean isCard = (currentBorder != null && currentBorder instanceof CompoundBorder) ||
                        panel.getLayout() instanceof BorderLayout; // Heuristic check


                if (isCard && panel.getParent() != null && !(panel.getParent() instanceof JScrollPane)) { // Avoid styling the main panel itself as a card
                    panel.setBackground(cardBg);
                    // Re-apply border based on theme
                    Border paddingBorder = new EmptyBorder(CARD_INTERNAL_PADDING, CARD_INTERNAL_PADDING,
                            CARD_INTERNAL_PADDING, CARD_INTERNAL_PADDING);
                    Border lineBorder = BorderFactory.createLineBorder(borderCol, 1);
                    Border outerBorder = BorderFactory.createCompoundBorder(lineBorder, paddingBorder);

                    if (!isDark) {
                        // Add shadow effect for light mode
                        Border shadow = BorderFactory.createCompoundBorder(
                                BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(0,0,0, 30)),
                                BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(0,0,0, 15))
                        );
                        panel.setBorder(BorderFactory.createCompoundBorder(shadow, outerBorder));
                    } else {
                        panel.setBorder(outerBorder);
                    }
                } else if (panel.isOpaque() && panel != this && !(panel.getParent() instanceof JViewport)){
                    // Style other opaque panels to match main background
                    panel.setBackground(themeBg);
                } else if (!panel.isOpaque()) {
                    panel.setBackground(null); // Keep transparent panels transparent
                }
            } else if (component instanceof JSlider) {
                JSlider slider = (JSlider) component;
                boolean isEnabled = slider.isEnabled();
                slider.setForeground(isEnabled ? textPrimary : textSecondary);
                slider.setOpaque(false); // Ensure sliders are transparent
            } else if (component instanceof JTabbedPane) {
                JTabbedPane tabPane = (JTabbedPane) component;
                tabPane.setBackground(themeBg);
                tabPane.setForeground(textPrimary);
                tabPane.setBorder(new EmptyBorder(10, 15, 5, 15));

                // Style tab components
                for (int i = 0; i < tabPane.getTabCount(); i++) {
                    Component tab = tabPane.getTabComponentAt(i);
                    if (tab == null) { // Create label if needed
                        String title = tabPane.getTitleAt(i);
                        JLabel tabLabel = new JLabel(title);
                        tabLabel.setFont(DEFAULT_FONT);
                        tabLabel.setForeground(textPrimary);
                        tabLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
                        tabPane.setTabComponentAt(i, tabLabel);
                    } else if (tab instanceof JLabel) { // Update existing label
                        ((JLabel) tab).setForeground(textPrimary);
                        ((JLabel) tab).setFont(DEFAULT_FONT);
                    }
                }
            } else if (component instanceof JButton && component != startButton && component != stopButton && component != toggleThemeButton) {
                JButton button = (JButton) component;
                styleButton(button, parentFrame.getPrimaryColor());
            } else if (component instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) component;
                scrollPane.getViewport().setBackground(themeBg);
                scrollPane.setBackground(themeBg);
                // Style scroll bars if needed (more complex)
            }

            // Recurse into containers (skip already handled buttons)
            if (component instanceof Container &&
                    !(component instanceof JButton && (component == startButton || component == stopButton || component == toggleThemeButton))) {
                updateComponentColors((Container) component);
            }
        }
    }

    // --- Settings Loading (Updated) ---
    public void loadSettingsToUI() {
        // Check if components are initialized
        if (cpsSpinner == null || toggleHotkeyComboBox == null || pauseHotkeyComboBox == null) {
            System.err.println("Warning: Attempting to load settings before UI components are initialized.");
            // Optionally, schedule this call later if initialization is asynchronous
            // SwingUtilities.invokeLater(this::loadSettingsToUI);
            return;
        }

        SwingUtilities.invokeLater(() -> {
            // Click Settings
            double currentCps = settingsManager.getCPS();
            cpsSpinner.setValue(currentCps);
            if (currentCps >= SLIDER_MIN_CPS && currentCps <= SLIDER_MAX_CPS) {
                cpsSlider.setValue((int) Math.round(currentCps));
            } else {
                cpsSlider.setValue(currentCps < SLIDER_MIN_CPS ? SLIDER_MIN_CPS : SLIDER_MAX_CPS);
            }
            clickModeComboBox.setSelectedItem(settingsManager.getClickMode());
            mouseButtonComboBox.setSelectedItem(settingsManager.getMouseButton());

            // Humanization
            boolean isRandomInterval = settingsManager.isRandomizeInterval();
            randomizeIntervalCheckBox.setSelected(isRandomInterval);
            randomizationFactorSlider.setValue((int) (settingsManager.getRandomizationFactor() * 100));
            randomizationFactorSlider.setEnabled(isRandomInterval);
            randomizationValueLabel.setText(String.format("%.0f%%", settingsManager.getRandomizationFactor() * 100));
            randomizationValueLabel.setEnabled(isRandomInterval);
            // Update dependent label state
            Component intensityLabel = findComponentByText(randomizationFactorSlider.getParent(), "Intensity:");
            if(intensityLabel != null) intensityLabel.setEnabled(isRandomInterval);

            boolean isRandomMove = settingsManager.isRandomMovement();
            randomMovementCheckBox.setSelected(isRandomMove);
            movementRadiusSpinner.setValue(settingsManager.getMovementRadius());
            movementRadiusSpinner.setEnabled(isRandomMove);
            // Update dependent label state
            Component radiusLabel = findComponentByText(movementRadiusSpinner.getParent().getParent(), "Max Radius (pixels):"); // Check parent's parent
            if(radiusLabel != null) radiusLabel.setEnabled(isRandomMove);


            // Hotkeys (Updated)
            toggleHotkeyComboBox.setSelectedItem(settingsManager.getToggleHotkey());
            visibilityHotkeyComboBox.setSelectedItem(settingsManager.getVisibilityHotkey());
            pauseHotkeyComboBox.setSelectedItem(settingsManager.getPauseHotkey());
            increaseSpeedHotkeyComboBox.setSelectedItem(settingsManager.getIncreaseSpeedHotkey());
            decreaseSpeedHotkeyComboBox.setSelectedItem(settingsManager.getDecreaseSpeedHotkey());

            // Appearance
            alwaysOnTopCheckBox.setSelected(settingsManager.isAlwaysOnTop());
            autoHideCheckBox.setSelected(settingsManager.isAutoHide());
            minimizeToTrayCheckBox.setSelected(settingsManager.isMinimizeToTray());
            transparencySlider.setValue((int) (settingsManager.getTransparency() * 100));
            transparencyValueLabel.setText(String.format("%.0f%%", settingsManager.getTransparency() * 100));

            // Apply auto-hide setting
            if (parentFrame != null) {
                parentFrame.setAutoHide(settingsManager.isAutoHide());
            }

            // Apply theme colors after loading settings
            applyThemeColors();
        });
    }

    // Helper to find components by text (more reliable than name)
    private Component findComponentByText(Container container, String text) {
        if (container == null || text == null) return null;
        for (Component comp : container.getComponents()) {
            if (comp instanceof JLabel && text.equals(((JLabel) comp).getText())) {
                return comp;
            } else if (comp instanceof Container) {
                Component found = findComponentByText((Container) comp, text);
                if (found != null) return found;
            }
        }
        return null;
    }
}
