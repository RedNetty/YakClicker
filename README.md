# YakClicker

A Java autoclicker with a modern UI, humanized click patterns, and platform-optimized input simulation. Built for automation, testing, and accessibility use cases.

## Features

- **Modern UI** — clean dark/light interface built with FlatLaf
- **Adjustable CPS** — 0.1 to 500 clicks per second
- **Multiple click modes** — single, double, or triple click
- **Mouse button selection** — left, middle, or right button
- **Humanization** — randomized click intervals and subtle cursor movement to produce natural-looking input
- **Global hotkeys** — control the app from any window with customizable shortcuts
- **Pattern recording** — record and replay complex click sequences
- **Platform optimizations** — tailored behavior for Windows, macOS, and Linux
- **Window utilities** — transparency, always-on-top, minimize to tray
- **Statistics** — real-time CPS readout and session totals

## Requirements

- Java 8+
- Windows, macOS, or Linux

## Running

Download the latest JAR from [Releases](https://github.com/RedNetty/YakClicker/releases) and run:

```bash
java -jar yakclicker.jar
```

Or double-click the JAR if your system supports it.

## Platform Notes

**Windows** — Run as administrator for best compatibility with elevated applications.

**macOS** — Grant Accessibility permissions: *System Settings → Privacy & Security → Accessibility*. Recommended CPS range: 5–15.

**Linux** — Requires X11. Wayland support is limited; XWayland may need additional permissions.

## Usage

1. Set your desired CPS
2. Choose click mode and mouse button
3. Press **Start** or the configured hotkey (default: **F6**)

**Humanization:** enable *Randomize Interval* and/or *Add Slight Mouse Movement* for natural-looking patterns.

**Patterns:** go to the *Patterns* tab → *Record New* → click your sequence → *Stop*. Select and *Play* to replay.

## Hotkeys

| Key | Action |
|-----|--------|
| F6 | Start / Stop |
| F7 | Pause / Resume |
| F8 | Show / Hide window |
| `+` / `-` | Increase / Decrease CPS |

All hotkeys are configurable in the *Hotkeys* tab.

## Building from Source

```bash
git clone https://github.com/RedNetty/YakClicker.git
cd YakClicker
mvn clean package
java -jar target/yakclicker-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Requires JDK 8+ and Maven.

## Dependencies

- [JNativeHook](https://github.com/kwhat/jnativehook) — global keyboard/mouse hooks
- [FlatLaf](https://www.formdev.com/flatlaf/) — modern cross-platform UI
- [JNA](https://github.com/java-native-access/jna) — native platform integration

## Disclaimer

This tool is intended for legitimate purposes such as testing, automation, and accessibility assistance. Users are responsible for complying with applicable laws and the terms of service of any software they interact with.

## License

MIT License. See [LICENSE](LICENSE) for details.
