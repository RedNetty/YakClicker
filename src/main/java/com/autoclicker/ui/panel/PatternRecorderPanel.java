package com.autoclicker.ui.panel;

import com.autoclicker.core.model.ClickPattern;
import com.autoclicker.core.service.PatternService;
import com.autoclicker.service.pattern.PatternRecorderService;
import com.autoclicker.ui.frame.MainFrame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit; // For formatting duration
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listener interface specific to the PatternRecorderPanel UI.
 * Extends the base PatternListener with UI-specific callbacks like progress.
 */
interface PatternRecorderListener extends PatternService.PatternListener {
    void onPlaybackProgress(int currentStep, int totalSteps);
}

/**
 * Panel for recording, managing, and playing back click patterns.
 * Interacts with PatternRecorderService and updates the UI accordingly.
 * Fixed issues with JScrollPane initialization for FlatLaf compatibility.
 */
public class PatternRecorderPanel extends JPanel implements PatternRecorderListener {
    private static final Logger LOGGER = Logger.getLogger(PatternRecorderPanel.class.getName());

    private final PatternRecorderService recorderService;
    private final MainFrame parentFrame; // For theme access

    // UI Components
    private JList<String> patternList;
    private DefaultListModel<String> patternListModel;
    private JButton recordButton;
    private JButton stopButton;
    private JButton playButton;
    private JButton pauseButton;
    private JButton deleteButton;
    private JCheckBox loopCheckBox;
    private JTable clicksTable;
    private ClicksTableModel clicksTableModel;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private Timer statusUpdateTimer; // Timer for periodic UI updates (e.g., recording time)

    // State Variables
    private ClickPattern selectedPatternDetail; // Holds details of the pattern selected in the list
    private long recordingStartTime; // To display elapsed recording time
    private int playbackCurrentStep = 0; // Current step for progress bar
    private int playbackTotalSteps = 0; // Total steps for progress bar

    // Constants
    private static final int VERTICAL_GAP = 10;
    private static final int HORIZONTAL_GAP = 15;
    private static final Font SECTION_HEADER_FONT = new Font("Arial", Font.BOLD, 14);
    private static final Font BUTTON_FONT = new Font("Arial", Font.BOLD, 12);
    private static final Color BORDER_COLOR_LIGHT = new Color(220, 220, 220);
    private static final Color BORDER_COLOR_DARK = new Color(80, 80, 80);

    /**
     * Table model for displaying the clicks within a selected pattern.
     */
    private class ClicksTableModel extends AbstractTableModel {
        private final String[] COLUMN_NAMES = {"#", "Position (X, Y)", "Delay (ms)", "Button", "Click Type"};
        private List<ClickPattern.ClickPoint> clickPoints = new ArrayList<>();

        public void setClickPoints(List<ClickPattern.ClickPoint> clickPoints) {
            this.clickPoints = (clickPoints != null) ? new ArrayList<>(clickPoints) : new ArrayList<>();
            fireTableDataChanged(); // Notify table UI of data change
        }

        @Override
        public int getRowCount() {
            return clickPoints.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMN_NAMES[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex >= clickPoints.size()) return null;

            ClickPattern.ClickPoint point = clickPoints.get(rowIndex);
            if (point == null) return null;

            switch (columnIndex) {
                case 0: return rowIndex + 1; // Click number (1-based)
                case 1: return String.format("(%d, %d)", point.getX(), point.getY());
                case 2: return point.getDelay(); // Delay *before* this click
                case 3: return point.getMouseButton();
                case 4: return point.getClickType();
                default: return null;
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            // Helps with sorting and rendering
            if (columnIndex == 0 || columnIndex == 2) {
                return Integer.class; // Or Long.class for delay if needed
            }
            return String.class;
        }
    }

    /**
     * Creates a new PatternRecorderPanel.
     *
     * @param recorderService The service handling pattern logic.
     * @param parentFrame     The main application window for theme access.
     */
    public PatternRecorderPanel(PatternRecorderService recorderService, MainFrame parentFrame) {
        if (recorderService == null || parentFrame == null) {
            throw new IllegalArgumentException("Service and parentFrame cannot be null.");
        }
        this.recorderService = recorderService;
        this.parentFrame = parentFrame;

        // Use a simple BorderLayout initially, populate components later
        setLayout(new BorderLayout());

        // Initialize components on the EDT to avoid UI threading issues
        SwingUtilities.invokeLater(this::initializeComponents);

        // Register this panel as a listener to the service
        this.recorderService.addListener(this);
    }

    /**
     * Initializes all Swing components and lays them out.
     * Called from the EDT to ensure proper component initialization.
     */
    private void initializeComponents() {
        setBorder(new EmptyBorder(15, 15, 15, 15)); // Padding around the panel

        // --- Left Panel (Pattern List & Controls) ---
        JPanel leftPanel = new JPanel(new BorderLayout(0, VERTICAL_GAP));
        leftPanel.setBorder(new EmptyBorder(0, 0, 0, HORIZONTAL_GAP / 2)); // Padding right
        leftPanel.setOpaque(false); // Use parent background

        // Header
        JLabel patternsHeader = createSectionHeader("Saved Patterns");
        leftPanel.add(patternsHeader, BorderLayout.NORTH);

        // List - Create model and list first, before scroll pane
        patternListModel = new DefaultListModel<>();
        patternList = new JList<>(patternListModel);
        patternList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        patternList.setVisibleRowCount(10); // Suggest height

        // Create scroll pane AFTER the list is fully initialized
        JScrollPane patternScrollPane = createScrollPane(patternList);
        leftPanel.add(patternScrollPane, BorderLayout.CENTER);

        // Controls (Record, Play, Stop, Delete)
        JPanel patternControlsPanel = new JPanel(new GridLayout(2, 2, 8, 8)); // Grid layout for buttons
        patternControlsPanel.setOpaque(false);

        recordButton = createStyledButton("Record New");
        playButton = createStyledButton("Play");
        stopButton = createStyledButton("Stop");
        deleteButton = createStyledButton("Delete");

        patternControlsPanel.add(recordButton);
        patternControlsPanel.add(playButton);
        patternControlsPanel.add(stopButton);
        patternControlsPanel.add(deleteButton);
        leftPanel.add(patternControlsPanel, BorderLayout.SOUTH);

        // --- Right Panel (Pattern Details & Status) ---
        JPanel rightPanel = new JPanel(new BorderLayout(0, VERTICAL_GAP));
        rightPanel.setBorder(new EmptyBorder(0, HORIZONTAL_GAP / 2, 0, 0)); // Padding left
        rightPanel.setOpaque(false);

        // Header
        JLabel detailsHeader = createSectionHeader("Pattern Details");
        rightPanel.add(detailsHeader, BorderLayout.NORTH);

        // Clicks Table - Create model and table first, before scroll pane
        clicksTableModel = new ClicksTableModel();
        clicksTable = new JTable(clicksTableModel);
        clicksTable.setFillsViewportHeight(true); // Table uses available space
        clicksTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        clicksTable.getTableHeader().setReorderingAllowed(false); // Prevent column drag
        clicksTable.setAutoCreateRowSorter(true); // Enable sorting
        clicksTable.setRowHeight(22); // Comfortable row height

        // Center-align specific columns (Number, Delay)
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        clicksTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer); // #
        clicksTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer); // Delay

        // Set preferred widths
        clicksTable.getColumnModel().getColumn(0).setPreferredWidth(40);  // #
        clicksTable.getColumnModel().getColumn(1).setPreferredWidth(120); // Position
        clicksTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // Delay
        clicksTable.getColumnModel().getColumn(3).setPreferredWidth(70);  // Button
        clicksTable.getColumnModel().getColumn(4).setPreferredWidth(90);  // Click Type

        // Create scroll pane AFTER table is fully initialized
        JScrollPane tableScrollPane = createScrollPane(clicksTable);
        rightPanel.add(tableScrollPane, BorderLayout.CENTER);

        // --- Status Area (Bottom Right) ---
        JPanel statusAreaPanel = new JPanel(new BorderLayout(HORIZONTAL_GAP, 5));
        statusAreaPanel.setOpaque(false);
        statusAreaPanel.setBorder(new EmptyBorder(VERTICAL_GAP, 0, 0, 0)); // Padding top

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.ITALIC));
        statusAreaPanel.add(statusLabel, BorderLayout.NORTH);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("No pattern selected");
        progressBar.setEnabled(false);
        statusAreaPanel.add(progressBar, BorderLayout.CENTER);

        // Options (Loop, Pause)
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        optionsPanel.setOpaque(false);
        loopCheckBox = new JCheckBox("Loop playback");
        loopCheckBox.setOpaque(false);
        pauseButton = createStyledButton("Pause");
        optionsPanel.add(loopCheckBox);
        optionsPanel.add(pauseButton);
        statusAreaPanel.add(optionsPanel, BorderLayout.SOUTH);

        rightPanel.add(statusAreaPanel, BorderLayout.SOUTH);

        // --- Main Layout ---
        // Use a Split Pane for resizable layout - create it last after both panels are fully initialized
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setResizeWeight(0.35); // Adjust initial split ratio if needed
        splitPane.setBorder(null); // Remove split pane border
        splitPane.setOpaque(false);
        splitPane.setContinuousLayout(true); // Update layout while dragging divider

        // Add the split pane to the main panel
        add(splitPane, BorderLayout.CENTER);

        // Setup listeners - do this after all components are created
        setupListeners();

        // Start the status update timer
        startStatusUpdateTimer();

        // Apply initial theme colors
        applyThemeColors();

        // Update the initial button states
        updateButtonStates();

        // Update the display with initial status
        updateStatusDisplay();

        // Load the patterns from the service
        List<String> patternNames = recorderService.getPatternNames();
        updatePatternList(patternNames);
    }

    /**
     * Creates a JScrollPane for a component with safe initialization for FlatLaf.
     * This method avoids the NPE in BasicScrollBarUI.layoutVScrollbar.
     */
    private JScrollPane createScrollPane(Component view) {
        // First create a basic scroll pane without the view
        JScrollPane scrollPane = new JScrollPane();

        // Then apply minimal styling
        scrollPane.setBorder(BorderFactory.createLineBorder(
                parentFrame.isDarkMode() ? BORDER_COLOR_DARK : BORDER_COLOR_LIGHT
        ));

        // Set the view once the scroll pane is fully initialized
        scrollPane.setViewportView(view);

        // Set policies after the view is set
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        return scrollPane;
    }

    /**
     * Sets up action listeners for buttons and list selection.
     */
    private void setupListeners() {
        // Pattern List Selection
        patternList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) { // Handle selection change only once
                updatePatternSelection();
            }
        });

        // Button Actions
        recordButton.addActionListener(e -> startRecording());
        playButton.addActionListener(e -> playSelectedPattern());
        stopButton.addActionListener(e -> stopRecordingOrPlayback());
        deleteButton.addActionListener(e -> deleteSelectedPattern());
        pauseButton.addActionListener(e -> pauseResumePlayback());

        // Loop checkbox doesn't need immediate action, state checked on play
    }

    /** Helper to create a styled button */
    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setFocusPainted(false); // Nicer look
        button.setOpaque(false); // Will be handled by styleButton
        button.setBorder(new EmptyBorder(8, 15, 8, 15)); // Padding inside button
        return button;
    }

    /** Helper to create a section header label */
    private JLabel createSectionHeader(String text) {
        JLabel header = new JLabel(text);
        header.setFont(SECTION_HEADER_FONT);
        header.setBorder(new EmptyBorder(0, 0, 5, 0)); // Bottom padding
        return header;
    }

    /**
     * Applies theme colors from the MainFrame to the panel components.
     */
    public void applyThemeColors() {
        if (parentFrame == null) return;

        Color bgColor = parentFrame.getBackgroundColor();
        Color textColor = parentFrame.getTextPrimaryColor();
        Color cardBgColor = parentFrame.getCardBackgroundColor();
        Color borderColor = parentFrame.isDarkMode() ? BORDER_COLOR_DARK : BORDER_COLOR_LIGHT;
        Color primaryColor = parentFrame.getPrimaryColor(); // Play
        Color successColor = parentFrame.getSuccessColor(); // Record
        Color dangerColor = parentFrame.getDangerColor();   // Stop, Delete
        Color warningColor = parentFrame.getWarningColor();  // Pause

        // Apply to panel background
        setBackground(bgColor);

        // Update list
        patternList.setBackground(cardBgColor);
        patternList.setForeground(textColor);
        patternList.setSelectionBackground(primaryColor);
        patternList.setSelectionForeground(Color.WHITE); // Ensure contrast on selection
        Component patternScrollPane = patternList.getParent() != null && patternList.getParent().getParent() != null ?
                patternList.getParent().getParent() : null; // Get the JScrollPane

        if (patternScrollPane instanceof JScrollPane) {
            ((JScrollPane) patternScrollPane).setBorder(BorderFactory.createLineBorder(borderColor));
        }

        // Update table
        clicksTable.setBackground(cardBgColor);
        clicksTable.setForeground(textColor);
        clicksTable.setGridColor(borderColor);
        clicksTable.setSelectionBackground(primaryColor);
        clicksTable.setSelectionForeground(Color.WHITE);

        if (clicksTable.getTableHeader() != null) {
            clicksTable.getTableHeader().setBackground(parentFrame.isDarkMode() ? new Color(55, 65, 81) : new Color(240, 240, 240));
            clicksTable.getTableHeader().setForeground(textColor);
        }

        Component tableScrollPane = clicksTable.getParent() != null && clicksTable.getParent().getParent() != null ?
                clicksTable.getParent().getParent() : null; // Get the JScrollPane

        if (tableScrollPane instanceof JScrollPane) {
            ((JScrollPane) tableScrollPane).setBorder(BorderFactory.createLineBorder(borderColor));
        }

        // Update buttons (pass color based on function)
        styleButton(recordButton, successColor);
        styleButton(playButton, primaryColor);
        styleButton(stopButton, dangerColor);
        styleButton(pauseButton, warningColor);
        styleButton(deleteButton, dangerColor);

        // Update other components
        statusLabel.setForeground(textColor);
        loopCheckBox.setForeground(textColor);
        loopCheckBox.setBackground(bgColor); // Match panel background

        // Update progress bar
        UIManager.put("ProgressBar.selectionBackground", textColor); // Text color
        UIManager.put("ProgressBar.selectionForeground", bgColor);   // Background for text
        progressBar.setForeground(primaryColor); // Bar color
        progressBar.setBackground(parentFrame.isDarkMode() ? new Color(55, 65, 81) : new Color(230, 230, 230));
        progressBar.setBorder(BorderFactory.createLineBorder(borderColor));

        // Force repaint
        revalidate();
        repaint();
    }

    /**
     * Styles a button with a background color and appropriate foreground color.
     * Handles enabled/disabled look.
     */
    private void styleButton(JButton button, Color enabledColor) {
        if (button == null) return;

        Color disabledColor = enabledColor.darker(); // Or a specific gray
        Color currentBgColor = button.isEnabled() ? enabledColor : disabledColor;

        button.setBackground(currentBgColor);

        // Calculate luminance to determine text color (black or white)
        double luminance = (0.299 * currentBgColor.getRed() + 0.587 * currentBgColor.getGreen() + 0.114 * currentBgColor.getBlue()) / 255.0;
        button.setForeground(luminance > 0.5 ? Color.BLACK : Color.WHITE);

        button.setOpaque(true); // Required for background color to show
        button.setBorderPainted(false); // Flat look
    }

    /**
     * Starts the timer that periodically calls `updateStatusDisplay`.
     */
    private void startStatusUpdateTimer() {
        if (statusUpdateTimer != null && statusUpdateTimer.isRunning()) {
            statusUpdateTimer.stop();
        }
        // Update status roughly 5 times per second
        statusUpdateTimer = new Timer(200, e -> updateStatusDisplay());
        statusUpdateTimer.setInitialDelay(0);
        statusUpdateTimer.start();
    }

    /**
     * Updates the status label and progress bar based on the recorder service state.
     * Called periodically by the `statusUpdateTimer`.
     */
    private void updateStatusDisplay() {
        boolean recording = recorderService.isRecording();
        boolean playing = recorderService.isPlaying();
        boolean paused = recorderService.isPaused();

        if (recording) {
            // --- Recording State ---
            long elapsedTimeMillis = System.currentTimeMillis() - recordingStartTime;
            String durationStr = formatDuration(elapsedTimeMillis);
            int clicks = (selectedPatternDetail != null) ? selectedPatternDetail.getClickCount() : 0;
            String patternName = (selectedPatternDetail != null && selectedPatternDetail.getName() != null)
                    ? selectedPatternDetail.getName() : "New Pattern";

            statusLabel.setText(String.format("Recording: %s (%d clicks, %s)",
                    patternName, clicks, durationStr));

            // Use indeterminate progress bar during recording
            progressBar.setIndeterminate(true);
            progressBar.setString("Recording...");
            progressBar.setEnabled(true);

        } else if (playing) {
            // --- Playing State ---
            progressBar.setIndeterminate(false); // Not indeterminate when playing
            ClickPattern currentPlayPattern = recorderService.getCurrentPattern(); // Get potentially updated pattern info
            String patternName = (currentPlayPattern != null && currentPlayPattern.getName() != null)
                    ? currentPlayPattern.getName() : "Unknown";
            String pausedStatus = paused ? " [PAUSED]" : "";

            statusLabel.setText(String.format("Playing: %s%s - Click %d / %d",
                    patternName, pausedStatus, playbackCurrentStep, playbackTotalSteps));

            if (playbackTotalSteps > 0) {
                int progress = (int) (((double) playbackCurrentStep / playbackTotalSteps) * 100);
                progressBar.setValue(progress);
                progressBar.setString(String.format("%d%%", progress));
                progressBar.setEnabled(true);
            } else {
                // Pattern might be empty or starting
                progressBar.setValue(0);
                progressBar.setString("Starting...");
                progressBar.setEnabled(true);
            }

        } else {
            // --- Idle State ---
            progressBar.setIndeterminate(false);
            if (selectedPatternDetail != null) {
                // Pattern selected in the list
                statusLabel.setText(String.format("Selected: %s", selectedPatternDetail.getName()));
                progressBar.setValue(100); // Show as "complete" or "ready"
                progressBar.setString(String.format("%d clicks, %.1f s",
                        selectedPatternDetail.getClickCount(),
                        selectedPatternDetail.getTotalDuration() / 1000.0));
                progressBar.setEnabled(true);
            } else {
                // No pattern selected
                statusLabel.setText("Ready");
                progressBar.setValue(0);
                progressBar.setString("No pattern selected");
                progressBar.setEnabled(false);
            }
        }
    }

    /** Formats milliseconds into MM:SS.ms */
    private String formatDuration(long millis) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long remainingMillis = millis % 1000;
        return String.format("%02d:%02d.%03d", minutes, seconds, remainingMillis);
    }

    /**
     * Updates the list of pattern names displayed.
     * Called by the listener when the service's pattern list changes.
     *
     * @param patternNames List of names from the service.
     */
    private void updatePatternList(List<String> patternNames) {
        if (patternList == null || patternListModel == null) return;

        // Get current selection before updating
        String selectedValue = patternList.getSelectedValue();

        // Update the model with new pattern names
        SwingUtilities.invokeLater(() -> {
            patternListModel.clear();
            if (patternNames != null) {
                for (String name : patternNames) {
                    patternListModel.addElement(name);
                }
            }

            // Try to re-select the previously selected item
            if (selectedValue != null && patternListModel.contains(selectedValue)) {
                patternList.setSelectedValue(selectedValue, true); // Scroll to selection
            } else {
                // If previous selection gone, clear details
                selectedPatternDetail = null;
                if (clicksTableModel != null) {
                    clicksTableModel.setClickPoints(null);
                }
            }

            updateButtonStates(); // Enable/disable buttons based on list content/selection
            updateStatusDisplay(); // Update status based on selection
        });
    }

    /**
     * Handles the selection change in the pattern list.
     * Fetches the details of the selected pattern and updates the table.
     */
    private void updatePatternSelection() {
        if (patternList == null) return;

        String selectedName = patternList.getSelectedValue();

        if (selectedName != null) {
            // Fetch pattern details from the service (returns a copy)
            selectedPatternDetail = recorderService.getPattern(selectedName);

            if (selectedPatternDetail != null && clicksTableModel != null) {
                clicksTableModel.setClickPoints(selectedPatternDetail.getClickPoints());
                if (clicksTable != null) {
                    clicksTable.setEnabled(true); // Enable table interaction
                }
            } else {
                // Should not happen if name is in list, but handle defensively
                LOGGER.warning("Selected pattern name '" + selectedName + "' not found in service.");
                if (clicksTableModel != null) {
                    clicksTableModel.setClickPoints(null);
                }
                if (clicksTable != null) {
                    clicksTable.setEnabled(false);
                }
            }
        } else {
            // No selection
            selectedPatternDetail = null;
            if (clicksTableModel != null) {
                clicksTableModel.setClickPoints(null);
            }
            if (clicksTable != null) {
                clicksTable.setEnabled(false);
            }
        }

        // Update UI elements based on the new selection state
        updateButtonStates();
        updateStatusDisplay();
    }

    /**
     * Updates the enabled/disabled state and text of buttons based on the
     * current application state (recording, playing, selection).
     */
    private void updateButtonStates() {
        boolean isRecording = recorderService.isRecording();
        boolean isPlaying = recorderService.isPlaying();
        boolean isPaused = recorderService.isPaused();
        boolean hasSelection = selectedPatternDetail != null; // Based on detailed pattern loaded
        boolean isActive = isRecording || isPlaying; // If either recording or playing

        // Enable/Disable buttons - check for null to avoid NPEs
        if (recordButton != null) recordButton.setEnabled(!isActive);
        if (stopButton != null) stopButton.setEnabled(isActive);
        if (playButton != null) playButton.setEnabled(!isActive && hasSelection);
        if (pauseButton != null) {
            pauseButton.setEnabled(isPlaying);
            pauseButton.setText(isPaused ? "Resume" : "Pause");
        }
        if (deleteButton != null) deleteButton.setEnabled(!isActive && hasSelection);

        // Disable list interaction and loop checkbox when active
        if (patternList != null) patternList.setEnabled(!isActive);
        if (loopCheckBox != null) loopCheckBox.setEnabled(!isActive && hasSelection);

        // Disable table interaction during active states
        if (clicksTable != null) clicksTable.setEnabled(!isActive);

        // Re-apply theme styles to reflect enabled/disabled state visually
        applyThemeColors();
    }

    // --- Action Handlers ---

    /**
     * Prompts for a pattern name and starts recording via the service.
     */
    private void startRecording() {
        String patternName = JOptionPane.showInputDialog(
                this,
                "Enter a name for the new pattern:",
                "New Pattern Name",
                JOptionPane.PLAIN_MESSAGE
        );

        if (patternName == null || patternName.trim().isEmpty()) {
            return; // User cancelled or entered empty name
        }
        patternName = patternName.trim();

        // Check if name already exists
        if (patternListModel.contains(patternName)) {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "A pattern named '" + patternName + "' already exists.\nOverwrite it?",
                    "Confirm Overwrite",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (confirm != JOptionPane.YES_OPTION) {
                return; // User chose not to overwrite
            }
            // If overwriting, we might want to delete the old one first,
            // but the service likely handles replacement.
        }

        // Clear current selection and details before starting
        if (patternList != null) patternList.clearSelection();
        selectedPatternDetail = null;
        if (clicksTableModel != null) clicksTableModel.setClickPoints(null);
        recordingStartTime = System.currentTimeMillis(); // Reset timer start

        // Tell the service to start recording
        recorderService.startRecording(patternName);

        // UI updates (disabling buttons etc.) will be handled by the
        // onRecordingStateChanged listener callback.
    }

    /**
     * Plays the currently selected pattern using the service.
     */
    private void playSelectedPattern() {
        if (selectedPatternDetail != null) {
            LOGGER.info("Requesting playback for: " + selectedPatternDetail.getName());
            boolean loop = (loopCheckBox != null) && loopCheckBox.isSelected();
            recorderService.playPattern(selectedPatternDetail.getName(), loop);
            // UI updates handled by onPlaybackStateChanged listener
        } else {
            LOGGER.warning("Play button clicked but no pattern selected.");
            // Optionally show a message to the user
            JOptionPane.showMessageDialog(this, "Please select a pattern to play.", "No Pattern Selected", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Stops either recording or playback via the service.
     */
    private void stopRecordingOrPlayback() {
        if (recorderService.isRecording()) {
            LOGGER.info("Requesting stop recording...");
            recorderService.stopRecording();
        } else if (recorderService.isPlaying()) {
            LOGGER.info("Requesting stop playback...");
            recorderService.stopPlayback();
        }
        // UI updates handled by listener callbacks
    }

    /**
     * Pauses or resumes playback via the service.
     */
    private void pauseResumePlayback() {
        if (recorderService.isPlaying()) {
            recorderService.pauseResumePlayback();
            // UI updates (button text, status) handled by listener callbacks and timer
        }
    }

    /**
     * Prompts for confirmation and deletes the selected pattern via the service.
     */
    private void deleteSelectedPattern() {
        if (selectedPatternDetail != null) {
            String nameToDelete = selectedPatternDetail.getName();
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to permanently delete the pattern '" + nameToDelete + "'?",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (confirm == JOptionPane.YES_OPTION) {
                boolean deleted = recorderService.deletePattern(nameToDelete);
                if (deleted) {
                    LOGGER.info("Pattern deleted successfully: " + nameToDelete);
                    // UI update (list removal) handled by onPatternListChanged listener
                    JOptionPane.showMessageDialog(
                            this,
                            "Pattern '" + nameToDelete + "' deleted.",
                            "Delete Successful",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    // Explicitly clear selection details after delete
                    selectedPatternDetail = null;
                    if (clicksTableModel != null) clicksTableModel.setClickPoints(null);
                    updateButtonStates();
                    updateStatusDisplay();

                } else {
                    LOGGER.warning("Failed to delete pattern: " + nameToDelete);
                    JOptionPane.showMessageDialog(
                            this,
                            "Could not delete pattern '" + nameToDelete + "'.\nCheck logs for details.",
                            "Delete Failed",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        } else {
            LOGGER.warning("Delete button clicked but no pattern selected.");
        }
    }

    /**
     * Cleans up resources like the status timer and removes listeners.
     * Should be called when the panel is being removed or the application closes.
     */
    public void cleanup() {
        LOGGER.info("Cleaning up PatternRecorderPanel...");
        // Stop status update timer
        if (statusUpdateTimer != null && statusUpdateTimer.isRunning()) {
            statusUpdateTimer.stop();
            LOGGER.fine("Status update timer stopped.");
        }
        // Remove listener from the service
        if (recorderService != null) {
            recorderService.removeListener(this);
            LOGGER.fine("Removed listener from PatternRecorderService.");
        }
        LOGGER.info("PatternRecorderPanel cleanup complete.");
    }

    // --- PatternRecorderListener Implementation ---

    @Override
    public void onRecordingStateChanged(boolean isRecording) {
        // Ensure updates happen on the EDT
        SwingUtilities.invokeLater(() -> {
            LOGGER.fine("UI Listener: Recording state changed -> " + isRecording);
            if (isRecording) {
                // Reset playback progress when recording starts
                playbackCurrentStep = 0;
                playbackTotalSteps = 0;
            }
            updateButtonStates();
            updateStatusDisplay(); // Update status immediately on state change
        });
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying, boolean isPaused) {
        SwingUtilities.invokeLater(() -> {
            LOGGER.fine("UI Listener: Playback state changed -> playing=" + isPlaying + ", paused=" + isPaused);
            if (!isPlaying) {
                // Reset progress when playback stops
                playbackCurrentStep = 0;
                playbackTotalSteps = 0;
            }
            updateButtonStates();
            updateStatusDisplay(); // Update status immediately
        });
    }

    @Override
    public void onPatternUpdated(ClickPattern pattern) {
        // This is called frequently during recording with the latest pattern state
        SwingUtilities.invokeLater(() -> {
            // If recording, update the details being displayed
            if (recorderService.isRecording() && pattern != null) {
                // Check if the pattern being updated matches the one we started recording
                // (This check might be fragile if names change, but good for now)
                if (selectedPatternDetail == null || pattern.getName().equals(selectedPatternDetail.getName())) {
                    selectedPatternDetail = pattern; // Update the local copy
                    if (clicksTableModel != null) {
                        clicksTableModel.setClickPoints(pattern.getClickPoints());
                    }
                    LOGGER.finest("UI Listener: Pattern updated (recording) -> " + pattern.getName() + ", clicks=" + pattern.getClickCount());
                }
            }
            // No need to call updateStatusDisplay here, timer handles periodic updates
        });
    }

    @Override
    public void onPatternListChanged(List<String> patternNames) {
        SwingUtilities.invokeLater(() -> {
            LOGGER.fine("UI Listener: Pattern list changed");
            updatePatternList(patternNames);
            // updatePatternSelection might be implicitly called if selection changes
        });
    }

    @Override
    public void onPlaybackProgress(int currentStep, int totalSteps) {
        // This is called by the service during playback
        SwingUtilities.invokeLater(() -> {
            this.playbackCurrentStep = currentStep;
            this.playbackTotalSteps = totalSteps;
            LOGGER.finest("UI Listener: Playback progress -> " + currentStep + "/" + totalSteps);
            // Status display will be updated by the timer
        });
    }
}