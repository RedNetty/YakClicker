# YakClicker


YakClicker is a modern, feature-rich auto-clicker application with a sleek web-app style UI. It provides precise control over automated mouse clicks with advanced humanization features, pattern recording, and platform-specific optimizations.

## Features

- **Modern UI**: Clean, responsive interface with dark and light themes
- **Adjustable Click Speed**: Set clicks-per-second (CPS) from 0.1 to 500
- **Multiple Click Modes**: Single, double, or triple click
- **Mouse Button Selection**: Left, middle, or right mouse button
- **Humanization Features**:
    - Click interval randomization with adjustable intensity
    - Random mouse movement to avoid detection
- **Global Hotkeys**: Control the application from anywhere with customizable keyboard shortcuts
- **Pattern Recording**: Record and replay complex click patterns
- **Platform-Specific Optimizations**: Tailored performance for Windows, macOS, and Linux
- **Window Controls**: Transparency, always-on-top, auto-hide, and minimize to tray
- **Statistics Tracking**: Real-time CPS measurement and click statistics

## Installation

### System Requirements

- Java 8 or higher
- Windows, macOS, or Linux

### Download

Download the latest release from the [Releases](https://github.com/YourUsername/yakclicker/releases) page.

### Running the Application

```
java -jar yakclicker-1.0-jar-with-dependencies.jar
```

Alternatively, double-click the JAR file if your system supports it.

## Platform-Specific Requirements

### Windows

- Running as administrator provides the best performance
- For some applications, you may need to disable "Run as administrator" on the clicker to work properly

### macOS

- **Important**: You must grant Accessibility permissions for YakClicker to work properly
- Go to **System Settings → Privacy & Security → Privacy → Accessibility** and add YakClicker
- A CPS value between 5-15 works most reliably on macOS

### Linux

- X11 required for full functionality (Wayland has limited support)
- For XWayland, additional permissions may be needed

## Usage

### Basic Usage

1. Set your desired CPS (clicks per second)
2. Select click mode (single, double, triple)
3. Choose mouse button (left, middle, right)
4. Press "Start Clicking" or use the configured hotkey (default: F6)

### Humanization

For more natural clicking patterns:

1. Enable "Randomize Click Interval" and adjust the intensity
2. Enable "Add Slight Mouse Movement" to add small cursor movements between clicks

### Recording Click Patterns

1. Navigate to the "Patterns" tab
2. Click "Record New" and enter a pattern name
3. Perform the clicks you want to record
4. Click "Stop" when finished
5. To replay, select the pattern and click "Play"

### Hotkey Configuration

Default hotkeys:
- F6: Start/stop clicking
- F7: Pause/resume clicking
- F8: Show/hide application window
- =/-: Increase/decrease CPS

You can customize these in the "Hotkeys" tab.

## Building from Source

### Prerequisites

- JDK 8 or higher
- Maven

### Build Steps

1. Clone the repository:
   ```
   git clone https://github.com/YourUsername/yakclicker.git
   cd yakclicker
   ```

2. Build with Maven:
   ```
   mvn clean package
   ```

3. Find the executable JAR in the `target` directory:
   ```
   target/yakclicker-1.0-SNAPSHOT-jar-with-dependencies.jar
   ```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- [JNativeHook](https://github.com/kwhat/jnativehook) for global keyboard and mouse hooks
- [FlatLaf](https://www.formdev.com/flatlaf/) for the modern UI look and feel
- [JNA](https://github.com/java-native-access/jna) for native platform integration

## Disclaimer

This tool is intended for legitimate use cases such as testing, automation, and accessibility. The user is responsible for complying with all applicable laws and terms of service when using this application.

Some games and applications may consider auto-clicking as a form of cheating. Use responsibly and at your own risk.
