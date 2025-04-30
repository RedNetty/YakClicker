package com.autoclicker.ui.panel;

import com.autoclicker.storage.SettingsManager;
import com.autoclicker.ui.frame.MainFrame;
import com.autoclicker.util.PlatformDetector;

import javax.swing.*;
import java.awt.*;

/**
 * Settings panel for platform-specific configurations.
 */
public class PlatformSettingsPanel extends JPanel {
    private final SettingsManager settingsManager;
    private final MainFrame parentFrame;

    // UI Components
    private JCheckBox platformOptimizationsCheckBox;
    private JCheckBox preventCursorLockCheckBox;
    private JButton openAccessibilitySettingsButton;

    public PlatformSettingsPanel(SettingsManager settingsManager, MainFrame parentFrame) {
        this.settingsManager = settingsManager;
        this.parentFrame = parentFrame;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(parentFrame.getBackgroundColor());

        initializeComponents();
    }

    private void initializeComponents() {
        // Platform detection header
        add(createSectionHeader("Platform Information"));

        // Platform info panel
        JPanel platformInfoPanel = createCard();
        platformInfoPanel.setLayout(new BorderLayout());

        // Platform icon and name
        String platformIconText = getPlatformIcon();
        String platformName = PlatformDetector.getOSNameAndVersion();

        JLabel platformIconLabel = new JLabel(platformIconText);
        platformIconLabel.setFont(new Font("Arial", Font.BOLD, 32));
        platformIconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        platformIconLabel.setForeground(parentFrame.getPrimaryColor());

        JLabel platformNameLabel = new JLabel(platformName);
        platformNameLabel.setFont(new Font("Arial", Font.BOLD, 16));
        platformNameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        platformNameLabel.setForeground(parentFrame.getTextPrimaryColor());

        JPanel platformHeaderPanel = new JPanel(new BorderLayout());
        platformHeaderPanel.setOpaque(false);
        platformHeaderPanel.add(platformIconLabel, BorderLayout.NORTH);
        platformHeaderPanel.add(platformNameLabel, BorderLayout.CENTER);

        // Platform-specific description
        String platformDescription = getPlatformDescription();
        JLabel descriptionLabel = new JLabel("<html><div style='text-align: center;'>" + platformDescription + "</div></html>");
        descriptionLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        descriptionLabel.setForeground(parentFrame.getTextSecondaryColor());
        descriptionLabel.setHorizontalAlignment(SwingConstants.CENTER);

        platformInfoPanel.add(platformHeaderPanel, BorderLayout.NORTH);
        platformInfoPanel.add(descriptionLabel, BorderLayout.CENTER);

        add(platformInfoPanel);
        add(Box.createRigidArea(new Dimension(0, 15)));

        // Platform-specific settings
        add(createSectionHeader("Platform-Specific Settings"));

        JPanel settingsPanel = createCard();
        settingsPanel.setLayout(new GridLayout(0, 1, 0, 10));

        // Use platform optimizations
        platformOptimizationsCheckBox = new JCheckBox("Use Platform-Specific Optimizations");
        platformOptimizationsCheckBox.setForeground(parentFrame.getTextPrimaryColor());
        platformOptimizationsCheckBox.setFont(new Font("Arial", Font.PLAIN, 13));
        platformOptimizationsCheckBox.setOpaque(false);
        platformOptimizationsCheckBox.setSelected(settingsManager.isPlatformSpecificSettings());
        platformOptimizationsCheckBox.addActionListener(e -> {
            settingsManager.setPlatformSpecificSettings(platformOptimizationsCheckBox.isSelected());
            preventCursorLockCheckBox.setEnabled(platformOptimizationsCheckBox.isSelected());
        });

        // Prevent cursor lock (macOS specific)
        preventCursorLockCheckBox = new JCheckBox("Prevent Cursor Lock (macOS)");
        preventCursorLockCheckBox.setForeground(parentFrame.getTextPrimaryColor());
        preventCursorLockCheckBox.setFont(new Font("Arial", Font.PLAIN, 13));
        preventCursorLockCheckBox.setOpaque(false);
        preventCursorLockCheckBox.setSelected(true);
        preventCursorLockCheckBox.setEnabled(platformOptimizationsCheckBox.isSelected());
        preventCursorLockCheckBox.setVisible(PlatformDetector.isMacOS());

        // Add components to panel
        settingsPanel.add(platformOptimizationsCheckBox);

        if (PlatformDetector.isMacOS()) {
            settingsPanel.add(preventCursorLockCheckBox);

            // Add button to open accessibility settings on macOS
            openAccessibilitySettingsButton = new JButton("Open macOS Accessibility Settings");
            openAccessibilitySettingsButton.addActionListener(e -> openMacOSAccessibilitySettings());
            settingsPanel.add(openAccessibilitySettingsButton);
        }

        add(settingsPanel);
        add(Box.createRigidArea(new Dimension(0, 15)));

        // Add troubleshooting section
        add(createSectionHeader("Troubleshooting"));

        JPanel troubleshootingPanel = createCard();
        troubleshootingPanel.setLayout(new BorderLayout());

        JLabel troubleshootingTitle = new JLabel("Having Issues?");
        troubleshootingTitle.setForeground(parentFrame.getTextPrimaryColor());
        troubleshootingTitle.setFont(new Font("Arial", Font.BOLD, 13));

        String troubleshootingHTML = getPlatformTroubleshooting();

        JLabel troubleshootingText = new JLabel(troubleshootingHTML);
        troubleshootingText.setForeground(parentFrame.getTextSecondaryColor());
        troubleshootingText.setFont(new Font("Arial", Font.PLAIN, 12));

        troubleshootingPanel.add(troubleshootingTitle, BorderLayout.NORTH);
        troubleshootingPanel.add(troubleshootingText, BorderLayout.CENTER);

        add(troubleshootingPanel);
    }

    private String getPlatformIcon() {
        PlatformDetector.OS os = PlatformDetector.getOperatingSystem();
        switch (os) {
            case WINDOWS:
                return "🪟";
            case MACOS:
                return "🍎";
            case LINUX:
                return "🐧";
            default:
                return "🖥️";
        }
    }

    private String getPlatformDescription() {
        PlatformDetector.OS os = PlatformDetector.getOperatingSystem();
        switch (os) {
            case WINDOWS:
                return "YakClicker is using Windows-optimized click methods.";
            case MACOS:
                return "YakClicker is using macOS-optimized click methods to prevent cursor locking.";
            case LINUX:
                return "YakClicker is using Linux-optimized click methods.";
            default:
                return "YakClicker is using generic click methods.";
        }
    }

    private String getPlatformTroubleshooting() {
        PlatformDetector.OS os = PlatformDetector.getOperatingSystem();
        switch (os) {
            case WINDOWS:
                return "<html><ul>" +
                        "<li>Try running as administrator if clicks aren't working</li>" +
                        "<li>Reduce CPS if you experience performance issues</li>" +
                        "<li>Some games may have anti-cheat that blocks automatic clicking</li>" +
                        "</ul></html>";
            case MACOS:
                return "<html><ul>" +
                        "<li>Ensure accessibility permissions are granted in System Preferences</li>" +
                        "<li>On macOS, slower CPS (below 20) works more reliably</li>" +
                        "<li>If cursor locks, try toggling the 'Prevent Cursor Lock' option</li>" +
                        "</ul></html>";
            case LINUX:
                return "<html><ul>" +
                        "<li>Ensure X11 permissions are properly configured</li>" +
                        "<li>Wayland may have limitations with mouse automation</li>" +
                        "<li>Try disabling compositor effects if experiencing issues</li>" +
                        "</ul></html>";
            default:
                return "<html><ul>" +
                        "<li>Try reducing CPS to improve reliability</li>" +
                        "<li>Disable randomization features if experiencing issues</li>" +
                        "</ul></html>";
        }
    }

    private void openMacOSAccessibilitySettings() {
        try {
            // Use Runtime to execute AppleScript
            String[] cmd = {
                    "osascript",
                    "-e", "tell application \"System Preferences\"",
                    "-e", "activate",
                    "-e", "set current pane to pane \"com.apple.preference.security\"",
                    "-e", "reveal anchor \"Privacy_Accessibility\" of pane \"com.apple.preference.security\"",
                    "-e", "end tell"
            };
            Runtime.getRuntime().exec(cmd);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Could not open Accessibility settings automatically.\n" +
                            "Please open System Preferences > Security & Privacy > Privacy > Accessibility",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private JLabel createSectionHeader(String text) {
        JLabel header = new JLabel(text);
        header.setFont(new Font("Arial", Font.BOLD, 14));
        header.setForeground(parentFrame.getTextPrimaryColor());
        header.setBorder(BorderFactory.createEmptyBorder(5, 8, 8, 0));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        return header;
    }

    private JPanel createCard() {
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout());
        card.setBackground(parentFrame.isDarkMode() ? new Color(31, 41, 55) : Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        parentFrame.isDarkMode() ? new Color(55, 65, 81) : new Color(226, 232, 240), 1),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));

        if (!parentFrame.isDarkMode()) {
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(0, 0, 0, 5), 1),
                            BorderFactory.createLineBorder(new Color(0, 0, 0, 0), 0)
                    ),
                    BorderFactory.createEmptyBorder(12, 15, 12, 15)
            ));
        }

        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return card;
    }
}