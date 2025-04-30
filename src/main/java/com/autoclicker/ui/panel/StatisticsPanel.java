package com.autoclicker.ui.panel;

import com.autoclicker.core.model.PerformanceMetric;
import com.autoclicker.service.click.AutoClickerService;
import com.autoclicker.storage.SettingsManager;
import com.autoclicker.ui.frame.MainFrame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Panel for displaying performance statistics and metrics.
 * Shows charts, graphs, and numerical data about clicking performance.
 */
public class StatisticsPanel extends JPanel {
    // Dependencies
    private final SettingsManager settingsManager;
    private final AutoClickerService clickerService;
    private final MainFrame parentFrame;

    // UI Components
    private JPanel metricsPanel;
    private JPanel chartPanel;
    private JButton resetStatsButton;
    private JLabel totalClicksLabel;
    private JLabel sessionClicksLabel;
    private JLabel peakCpsLabel;
    private JLabel avgCpsLabel;
    private JLabel accuracyLabel;
    private JLabel uptimeLabel;
    private JLabel totalClicksValueLabel;
    private JLabel sessionClicksValueLabel;
    private JLabel peakCpsValueLabel;
    private JLabel avgCpsValueLabel;
    private JLabel accuracyValueLabel;
    private JLabel uptimeValueLabel;

    // Performance chart
    private List<PerformanceMetric> performanceData;

    // Timer for updating stats
    private Timer updateTimer;

    // Session tracking
    private long sessionStartTime;

    // Constants
    private static final int PADDING = 15;
    private static final int CHART_HEIGHT = 200;
    private static final int UPDATE_INTERVAL_MS = 1000;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.0");
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.0%");

    /**
     * Creates a new statistics panel.
     */
    public StatisticsPanel(SettingsManager settingsManager, AutoClickerService clickerService, MainFrame parentFrame) {
        this.settingsManager = settingsManager;
        this.clickerService = clickerService;
        this.parentFrame = parentFrame;

        // Initialize session timing
        this.sessionStartTime = System.currentTimeMillis();

        initializeComponents();
        startUpdateTimer();
    }

    /**
     * Initializes the panel components.
     */
    private void initializeComponents() {
        setLayout(new BorderLayout(PADDING, PADDING));
        setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));

        // Create metrics panel (top)
        metricsPanel = new JPanel(new GridLayout(2, 3, PADDING, PADDING));

        // Key metrics cards
        totalClicksValueLabel = createValueLabel("0");
        JPanel totalClicksCard = createMetricCard("Total Clicks", totalClicksValueLabel);

        sessionClicksValueLabel = createValueLabel("0");
        JPanel sessionClicksCard = createMetricCard("Session Clicks", sessionClicksValueLabel);

        peakCpsValueLabel = createValueLabel("0.0");
        JPanel peakCpsCard = createMetricCard("Peak CPS", peakCpsValueLabel);

        avgCpsValueLabel = createValueLabel("0.0");
        JPanel avgCpsCard = createMetricCard("Average CPS", avgCpsValueLabel);

        accuracyValueLabel = createValueLabel("0.0%");
        JPanel accuracyCard = createMetricCard("Click Accuracy", accuracyValueLabel);

        uptimeValueLabel = createValueLabel("00:00:00");
        JPanel uptimeCard = createMetricCard("Session Uptime", uptimeValueLabel);

        // Add cards to metrics panel
        metricsPanel.add(totalClicksCard);
        metricsPanel.add(sessionClicksCard);
        metricsPanel.add(peakCpsCard);
        metricsPanel.add(avgCpsCard);
        metricsPanel.add(accuracyCard);
        metricsPanel.add(uptimeCard);

        // Create chart panel (center)
        chartPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawPerformanceChart(g);
            }
        };
        chartPanel.setPreferredSize(new Dimension(0, CHART_HEIGHT));

        // Create controls panel (bottom)
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, PADDING, 0));

        resetStatsButton = new JButton("Reset Statistics");
        resetStatsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetStatistics();
            }
        });

        controlsPanel.add(resetStatsButton);

        // Add all sections to the main panel
        add(createTitledPanel("Key Metrics", metricsPanel), BorderLayout.NORTH);
        add(createTitledPanel("Performance Chart", chartPanel), BorderLayout.CENTER);
        add(controlsPanel, BorderLayout.SOUTH);

        // Apply theme colors initially
        applyThemeColors();
    }

    /**
     * Creates a panel with a title and content.
     */
    private JPanel createTitledPanel(String title, JPanel content) {
        JPanel panel = new JPanel(new BorderLayout(0, PADDING/2));
        panel.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Creates a card displaying a metric.
     */
    private JPanel createMetricCard(String title, JLabel valueLabel) {
        JPanel card = new JPanel(new BorderLayout());

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(new EmptyBorder(5, 0, 0, 0));

        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        valueLabel.setBorder(new EmptyBorder(0, 0, 5, 0));

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    /**
     * Creates a value label with standard styling.
     */
    private JLabel createValueLabel(String initialValue) {
        JLabel label = new JLabel(initialValue);
        label.setFont(new Font("Arial", Font.BOLD, 20));
        return label;
    }

    /**
     * Starts the timer for updating statistics.
     */
    private void startUpdateTimer() {
        if (updateTimer != null && updateTimer.isRunning()) {
            updateTimer.stop();
        }

        updateTimer = new Timer(UPDATE_INTERVAL_MS, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateStatistics();
            }
        });
        updateTimer.start();
    }

    /**
     * Updates the displayed statistics.
     */
    private void updateStatistics() {
        // Fetch the latest performance data
        performanceData = settingsManager.getPerformanceMetrics();

        // Update labels with current stats
        totalClicksValueLabel.setText(String.format("%,d", settingsManager.getTotalClicks()));
        sessionClicksValueLabel.setText(String.format("%,d", clickerService.getSessionClicks()));
        peakCpsValueLabel.setText(DECIMAL_FORMAT.format(clickerService.getMaxCPS()));
        avgCpsValueLabel.setText(DECIMAL_FORMAT.format(clickerService.getAverageCPS()));
        accuracyValueLabel.setText(PERCENT_FORMAT.format(clickerService.getClickAccuracy()));

        // Calculate and display uptime
        long uptimeMs = System.currentTimeMillis() - sessionStartTime;
        uptimeValueLabel.setText(formatUptime(uptimeMs));

        // Repaint the chart with new data
        chartPanel.repaint();
    }

    /**
     * Formats uptime into HH:MM:SS.
     */
    private String formatUptime(long uptimeMs) {
        long hours = TimeUnit.MILLISECONDS.toHours(uptimeMs);
        uptimeMs -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(uptimeMs);
        uptimeMs -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(uptimeMs);

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Resets the statistics.
     */
    private void resetStatistics() {
        clickerService.resetStatistics();
        settingsManager.clearPerformanceMetrics();
        sessionStartTime = System.currentTimeMillis();
        updateStatistics();
    }

    /**
     * Draws the performance chart.
     */
    private void drawPerformanceChart(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        // Enable anti-aliasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = chartPanel.getWidth();
        int height = chartPanel.getHeight();

        // Get theme colors
        Color bgColor = parentFrame.getCardBackgroundColor();
        Color borderColor = parentFrame.getBorderColor();
        Color gridColor = new Color(borderColor.getRed(), borderColor.getGreen(), borderColor.getBlue(), 60);
        Color textColor = parentFrame.getTextSecondaryColor();
        Color primaryColor = parentFrame.getPrimaryColor();
        Color targetLineColor = parentFrame.getSecondaryColor();

        // Draw background
        g2d.setColor(bgColor);
        g2d.fillRect(0, 0, width, height);

        // Draw border
        g2d.setColor(borderColor);
        g2d.drawRect(0, 0, width - 1, height - 1);

        // Define chart margins
        int marginLeft = 50;
        int marginRight = 20;
        int marginTop = 20;
        int marginBottom = 30;
        int chartWidth = width - marginLeft - marginRight;
        int chartHeight = height - marginTop - marginBottom;

        // Check if we have performance data
        if (performanceData == null || performanceData.isEmpty()) {
            // Draw "No Data" message
            g2d.setColor(textColor);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            String message = "No performance data available";
            FontMetrics fm = g2d.getFontMetrics();
            int textX = (width - fm.stringWidth(message)) / 2;
            int textY = height / 2;
            g2d.drawString(message, textX, textY);
            return;
        }

        // Find max and min CPS values
        double maxCps = 0;
        double targetCps = settingsManager.getCPS();
        for (PerformanceMetric metric : performanceData) {
            maxCps = Math.max(maxCps, metric.getActualCps());
        }
        maxCps = Math.max(maxCps, targetCps);
        maxCps = Math.ceil(maxCps * 1.1); // Add 10% padding and round up

        // Calculate scale factors
        double xScale = (double) chartWidth / Math.max(1, performanceData.size() - 1);
        double yScale = (double) chartHeight / Math.max(1, maxCps);

        // Draw grid lines
        g2d.setColor(gridColor);
        g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{2}, 0));

        // Horizontal grid lines (CPS)
        int numGridLines = 5;
        for (int i = 0; i <= numGridLines; i++) {
            int y = marginTop + chartHeight - (int)((chartHeight * i) / numGridLines);
            g2d.drawLine(marginLeft, y, width - marginRight, y);

            // Draw y-axis labels
            double cpsValue = (maxCps * i) / numGridLines;
            g2d.setColor(textColor);
            g2d.setFont(new Font("Arial", Font.PLAIN, 10));
            String label = DECIMAL_FORMAT.format(cpsValue);
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(label, marginLeft - fm.stringWidth(label) - 5, y + fm.getAscent()/2);

            g2d.setColor(gridColor);
        }

        // Draw target CPS line
        int targetY = marginTop + chartHeight - (int)(targetCps * yScale);
        g2d.setColor(targetLineColor);
        g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5, 3}, 0));
        g2d.drawLine(marginLeft, targetY, width - marginRight, targetY);

        // Draw target CPS label
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        String targetLabel = "Target: " + DECIMAL_FORMAT.format(targetCps) + " CPS";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(targetLabel, width - marginRight - fm.stringWidth(targetLabel) - 5, targetY - 5);

        // Draw actual CPS line
        g2d.setColor(primaryColor);
        g2d.setStroke(new BasicStroke(2));

        // Use Path2D for smoother line drawing
        Path2D.Double path = new Path2D.Double();
        boolean started = false;

        for (int i = 0; i < performanceData.size(); i++) {
            PerformanceMetric metric = performanceData.get(i);
            int x = marginLeft + (int)(i * xScale);
            int y = marginTop + chartHeight - (int)(metric.getActualCps() * yScale);

            if (!started) {
                path.moveTo(x, y);
                started = true;
            } else {
                path.lineTo(x, y);
            }
        }

        g2d.draw(path);

        // Draw time labels
        g2d.setColor(textColor);
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));

        // Only show a few time labels to avoid clutter
        int numTimeLabels = Math.min(5, performanceData.size());
        if (numTimeLabels > 1) {
            for (int i = 0; i < numTimeLabels; i++) {
                int index = i * (performanceData.size() - 1) / (numTimeLabels - 1);
                if (index < performanceData.size()) {
                    int x = marginLeft + (int)(index * xScale);
                    int y = marginTop + chartHeight + 15;

                    String timeLabel = String.format("%ds", i * 5);
                    FontMetrics tfm = g2d.getFontMetrics();
                    g2d.drawString(timeLabel, x - tfm.stringWidth(timeLabel)/2, y);
                }
            }
        }

        // Draw axis labels
        g2d.setFont(new Font("Arial", Font.BOLD, 11));

        // Y-axis label
        String yLabel = "CPS (Clicks Per Second)";
        FontMetrics yfm = g2d.getFontMetrics();
        Graphics2D g2dY = (Graphics2D) g2d.create();
        g2dY.rotate(-Math.PI/2);
        g2dY.drawString(yLabel, -((marginTop + chartHeight/2) + yfm.stringWidth(yLabel)/2), marginLeft/2);
        g2dY.dispose();

        // X-axis label
        String xLabel = "Time (seconds)";
        FontMetrics xfm = g2d.getFontMetrics();
        g2d.drawString(xLabel, marginLeft + (chartWidth - xfm.stringWidth(xLabel))/2, height - 5);
    }

    /**
     * Applies theme colors to all components.
     */
    public void applyThemeColors() {
        if (parentFrame == null) return;

        // Get theme colors
        Color bgColor = parentFrame.getBackgroundColor();
        Color cardBgColor = parentFrame.getCardBackgroundColor();
        Color textColor = parentFrame.getTextPrimaryColor();
        Color secondaryTextColor = parentFrame.getTextSecondaryColor();
        Color primaryColor = parentFrame.getPrimaryColor();
        Color borderColor = parentFrame.getBorderColor();
        boolean isDarkMode = parentFrame.isDarkMode();

        // Apply to panel
        setBackground(bgColor);

        // Apply to metric cards
        for (Component c : metricsPanel.getComponents()) {
            if (c instanceof JPanel) {
                JPanel card = (JPanel) c;
                card.setBackground(cardBgColor);
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(borderColor, 1),
                        BorderFactory.createEmptyBorder(10, 10, 10, 10)
                ));

                // Update text colors in the card
                if (card.getComponentCount() >= 2) {
                    Component titleComp = card.getComponent(0);
                    Component valueComp = card.getComponent(1);

                    if (titleComp instanceof JLabel) {
                        ((JLabel) titleComp).setForeground(secondaryTextColor);
                    }

                    if (valueComp instanceof JLabel) {
                        ((JLabel) valueComp).setForeground(textColor);
                    }
                }
            }
        }

        // Apply to chart panel
        chartPanel.setBackground(bgColor);
        chartPanel.setBorder(BorderFactory.createLineBorder(borderColor, 1));

        // Apply to title labels
        Component northPanel = getComponent(0);
        if (northPanel instanceof JPanel) {
            Component titleLabel = ((JPanel) northPanel).getComponent(0);
            if (titleLabel instanceof JLabel) {
                ((JLabel) titleLabel).setForeground(textColor);
            }
        }

        Component centerPanel = getComponent(1);
        if (centerPanel instanceof JPanel) {
            Component titleLabel = ((JPanel) centerPanel).getComponent(0);
            if (titleLabel instanceof JLabel) {
                ((JLabel) titleLabel).setForeground(textColor);
            }
        }

        // Style the reset button
        if (resetStatsButton != null) {
            resetStatsButton.setBackground(primaryColor);

            // Set text color based on background brightness
            double luminance = (0.299 * primaryColor.getRed() + 0.587 * primaryColor.getGreen() + 0.114 * primaryColor.getBlue()) / 255;
            resetStatsButton.setForeground(luminance > 0.5 ? Color.BLACK : Color.WHITE);

            resetStatsButton.setFocusPainted(false);
            resetStatsButton.setBorderPainted(false);
            resetStatsButton.setOpaque(true);
            resetStatsButton.setBorder(new EmptyBorder(8, 12, 8, 12));

            // Remove existing listeners to prevent duplicates
            for (java.awt.event.MouseListener ml : resetStatsButton.getMouseListeners()) {
                if (ml != null) {
                    resetStatsButton.removeMouseListener(ml);
                }
            }

            resetStatsButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (resetStatsButton.isEnabled()) {
                        resetStatsButton.setBackground(primaryColor);
                    }
                }
            });
        }

        // Repaint to apply changes
        revalidate();
        repaint();
    }

    /**
     * Cleans up resources.
     */
    public void cleanup() {
        if (updateTimer != null && updateTimer.isRunning()) {
            updateTimer.stop();
            updateTimer = null;
        }
    }
}