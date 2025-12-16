package com.autoclicker.ui.dialog;

import com.autoclicker.util.PlatformDetector;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Dialog displaying platform-specific information and configuration help.
 */
public class PlatformInfoDialog extends JDialog {

    public PlatformInfoDialog(JFrame parent) {
        super(parent, "Platform Information", true);

        // Set up the dialog
        setSize(500, 400);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        // Create content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Platform detection
        PlatformDetector.OS os = PlatformDetector.getOperatingSystem();

        // Title panel
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new FlowLayout(FlowLayout.CENTER));

        JLabel iconLabel = new JLabel(getPlatformIcon(os));
        iconLabel.setFont(new Font("Arial", Font.PLAIN, 48));

        JLabel titleLabel = new JLabel(getPlatformName(os));
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));

        titlePanel.add(iconLabel);
        titlePanel.add(titleLabel);

        // Info panel
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BorderLayout());
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        JTextArea infoTextArea = new JTextArea(getPlatformInstructions(os));
        infoTextArea.setEditable(false);
        infoTextArea.setLineWrap(true);
        infoTextArea.setWrapStyleWord(true);
        infoTextArea.setFont(new Font("Arial", Font.PLAIN, 14));
        infoTextArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(infoTextArea);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        infoPanel.add(scrollPane, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        if (os == PlatformDetector.OS.MACOS) {
            JButton accessibilityButton = new JButton("Open Accessibility Settings");
            accessibilityButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    openMacOSAccessibilitySettings();
                }
            });
            buttonPanel.add(accessibilityButton);
        }

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        buttonPanel.add(closeButton);

        // Add components to content panel
        contentPanel.add(titlePanel);
        contentPanel.add(infoPanel);
        contentPanel.add(buttonPanel);

        // Add content to dialog
        add(contentPanel, BorderLayout.CENTER);
    }

    private String getPlatformIcon(PlatformDetector.OS os) {
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

    private String getPlatformName(PlatformDetector.OS os) {
        switch (os) {
            case WINDOWS:
                return "Windows Configuration";
            case MACOS:
                return "macOS Configuration";
            case LINUX:
                return "Linux Configuration";
            default:
                return "Platform Configuration";
        }
    }

    private String getPlatformInstructions(PlatformDetector.OS os) {
        switch (os) {
            case WINDOWS:
                return "Windows Configuration Tips:\n\n" +
                        "1. For best performance, run YakClicker as administrator (right-click the application and select 'Run as administrator').\n\n" +
                        "2. If you experience issues with the cursor moving unexpectedly, try reducing the CPS (clicks per second) value.\n\n" +
                        "3. Some applications and games may have anti-cheat mechanisms that detect and block automated clicking.\n\n" +
                        "4. For Windows-specific optimizations, ensure the 'Use Platform-Specific Optimizations' option is enabled in the settings.\n\n" +
                        "5. If you're experiencing issues, try disabling any other mouse or keyboard enhancement software.";

            case MACOS:
                return "macOS Configuration Tips:\n\n" +
                        "1. IMPORTANT: You must grant accessibility permissions for YakClicker to work properly.\n" +
                        "   Go to System Preferences > Security & Privacy > Privacy > Accessibility and add YakClicker to the list of allowed apps.\n\n" +
                        "2. macOS has stricter security settings compared to other operating systems. If the clicks aren't working, check that accessibility permissions are properly granted.\n\n" +
                        "3. If your cursor gets locked when clicking is active, try enabling the 'Prevent Cursor Lock' option in the settings.\n\n" +
                        "4. On macOS, a lower CPS (below 20) tends to work more reliably.\n\n" +
                        "5. If issues persist, try disabling randomization features and restarting the application.\n\n" +
                        "Click the 'Open Accessibility Settings' button below to access these settings directly.";

            case LINUX:
                return "Linux Configuration Tips:\n\n" +
                        "1. Linux versions may have different requirements depending on your window system (X11 or Wayland).\n\n" +
                        "2. For X11, ensure that you have proper permissions for input simulation.\n\n" +
                        "3. Wayland has more restrictions on input control, and may limit some functionality.\n\n" +
                        "4. For certain desktop environments, you may need to disable compositor effects for better performance.\n\n" +
                        "5. If clicks aren't registering in certain applications, try running YakClicker with elevated privileges.";

            default:
                return "General Configuration Tips:\n\n" +
                        "1. For optimal performance, ensure you have the latest Java version installed.\n\n" +
                        "2. If you experience issues with clicks not registering, try reducing the CPS value.\n\n" +
                        "3. Some applications may block automated input. Test in different applications to confirm functionality.\n\n" +
                        "4. If you're experiencing high CPU usage, disable randomization features and use lower CPS values.";
        }
    }

    private void openMacOSAccessibilitySettings() {
        try {
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
                            "Please open System Preferences > Security & Privacy > Privacy > Accessibility.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}