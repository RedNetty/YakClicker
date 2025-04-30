package com.autoclicker.service.pattern;

import com.autoclicker.config.AppConfig;
import com.autoclicker.core.model.ClickPattern;
import com.autoclicker.core.model.ClickPattern.ClickPoint;
import com.autoclicker.core.service.ClickService;
import com.autoclicker.core.service.PatternService;
import com.autoclicker.storage.SettingsManager;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseListener;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for recording and playing back click patterns.
 * Implements the PatternService interface.
 */
public class PatternRecorderService implements PatternService, NativeMouseListener {
    private static final Logger LOGGER = Logger.getLogger(PatternRecorderService.class.getName());

    private final SettingsManager settingsManager;
    private final ClickService clickerService;

    private final AtomicBoolean isRecording = new AtomicBoolean(false);
    private final AtomicBoolean isPlaying = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);

    private ClickPattern currentPattern;
    private long recordingStartTime;
    private long lastClickTime;

    private ScheduledExecutorService playbackExecutor;
    private ScheduledFuture<?> currentPlaybackTask;

    private final Map<String, ClickPattern> savedPatterns = new HashMap<>();
    private final List<PatternListener> listeners = new ArrayList<>();

    /**
     * Creates a new PatternRecorderService.
     *
     * @param settingsManager The settings manager
     * @param clickerService The click service
     */
    public PatternRecorderService(SettingsManager settingsManager, ClickService clickerService) {
        this.settingsManager = settingsManager;
        this.clickerService = clickerService;

        // Load saved patterns
        loadSavedPatterns();

        // Register mouse listener
        GlobalScreen.addNativeMouseListener(this);
    }

    /**
     * Adds a listener for pattern recorder events.
     */
    @Override
    public void addListener(PatternListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a listener.
     */
    @Override
    public void removeListener(PatternListener listener) {
        listeners.remove(listener);
    }

    /**
     * Starts recording a new click pattern.
     */
    @Override
    public void startRecording(String patternName) {
        if (isRecording.get() || isPlaying.get()) {
            return;
        }

        // Create a new pattern
        currentPattern = new ClickPattern(patternName);

        // Set recording state
        isRecording.set(true);
        recordingStartTime = System.currentTimeMillis();
        lastClickTime = recordingStartTime;

        // Notify listeners
        for (PatternListener listener : listeners) {
            listener.onRecordingStateChanged(true);
        }

        LOGGER.log(Level.INFO, "Pattern recording started: {0}", patternName);
    }

    /**
     * Stops the current recording.
     */
    @Override
    public void stopRecording() {
        if (!isRecording.get()) {
            return;
        }

        isRecording.set(false);

        // Save the pattern if it has at least one click
        if (currentPattern != null && !currentPattern.getClickPoints().isEmpty()) {
            // Add to saved patterns
            savedPatterns.put(currentPattern.getName(), currentPattern);

            // Save to disk
            savePattern(currentPattern);

            // Notify listeners of updated pattern list
            notifyPatternListChanged();
        }

        // Notify listeners
        for (PatternListener listener : listeners) {
            listener.onRecordingStateChanged(false);
            if (currentPattern != null) {
                listener.onPatternUpdated(currentPattern);
            }
        }

        LOGGER.log(Level.INFO, "Pattern recording stopped");
    }

    /**
     * Starts playing a pattern.
     */
    @Override
    public void playPattern(String patternName, boolean loop) {
        if (isRecording.get() || isPlaying.get()) {
            return;
        }

        ClickPattern pattern = savedPatterns.get(patternName);
        if (pattern == null || pattern.getClickPoints().isEmpty()) {
            LOGGER.log(Level.WARNING, "Pattern not found or empty: {0}", patternName);
            return;
        }

        // Set pattern looping state
        pattern.setLooping(loop);

        // Store current pattern being played
        currentPattern = pattern;

        // Set playing state
        isPlaying.set(true);
        isPaused.set(false);

        // Create executor for playback
        playbackExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "pattern-playback-thread");
            thread.setDaemon(true);
            return thread;
        });

        // Start playback
        playbackPattern(pattern);

        // Notify listeners
        for (PatternListener listener : listeners) {
            listener.onPlaybackStateChanged(true, false);
        }

        LOGGER.log(Level.INFO, "Pattern playback started: {0} {1}",
                new Object[]{patternName, loop ? "(looping)" : ""});
    }

    /**
     * Stops the current pattern playback.
     */
    @Override
    public void stopPlayback() {
        if (!isPlaying.get()) {
            return;
        }

        isPlaying.set(false);
        isPaused.set(false);

        // Cancel playback
        if (currentPlaybackTask != null && !currentPlaybackTask.isDone()) {
            currentPlaybackTask.cancel(true);
            currentPlaybackTask = null;
        }

        // Shutdown the executor
        if (playbackExecutor != null && !playbackExecutor.isShutdown()) {
            playbackExecutor.shutdownNow();
            playbackExecutor = null;
        }

        // Notify listeners
        for (PatternListener listener : listeners) {
            listener.onPlaybackStateChanged(false, false);
        }

        LOGGER.log(Level.INFO, "Pattern playback stopped");
    }

    /**
     * Pauses or resumes the current pattern playback.
     */
    @Override
    public void pauseResumePlayback() {
        if (!isPlaying.get()) {
            return;
        }

        isPaused.set(!isPaused.get());

        // Notify listeners
        for (PatternListener listener : listeners) {
            listener.onPlaybackStateChanged(true, isPaused.get());
        }

        LOGGER.log(Level.INFO, "Pattern playback {0}",
                isPaused.get() ? "paused" : "resumed");
    }

    /**
     * Plays back a pattern.
     */
    private void playbackPattern(ClickPattern pattern) {
        final List<ClickPoint> points = pattern.getClickPoints();
        final int[] currentIndex = {0};

        currentPlaybackTask = playbackExecutor.scheduleAtFixedRate(() -> {
            // Skip if paused
            if (isPaused.get()) {
                return;
            }

            // Check if playback should stop
            if (!isPlaying.get() || currentIndex[0] >= points.size()) {
                if (pattern.isLooping() && isPlaying.get()) {
                    // Reset index for looping
                    currentIndex[0] = 0;
                } else {
                    // Stop playback
                    stopPlayback();
                    return;
                }
            }

            try {
                // Get current point
                ClickPoint point = points.get(currentIndex[0]);

                // Perform click
                performClick(point);

                // Increment index
                currentIndex[0]++;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error during pattern playback", e);
            }
        }, 0, 50, TimeUnit.MILLISECONDS); // Check every 50ms for next action
    }

    /**
     * Performs a click at the specified point.
     */
    private void performClick(ClickPoint point) throws AWTException {
        Robot robot = new Robot();

        // Move to position
        robot.mouseMove(point.getX(), point.getY());

        // Delay to simulate human movement
        robot.delay(50);

        // Get mouse button
        int button = getButtonMask(point.getMouseButton());

        // Perform click based on type
        switch (point.getClickType()) {
            case "SINGLE":
                robot.mousePress(button);
                robot.delay(20);
                robot.mouseRelease(button);
                break;

            case "DOUBLE":
                robot.mousePress(button);
                robot.delay(20);
                robot.mouseRelease(button);
                robot.delay(50);
                robot.mousePress(button);
                robot.delay(20);
                robot.mouseRelease(button);
                break;

            case "TRIPLE":
                robot.mousePress(button);
                robot.delay(20);
                robot.mouseRelease(button);
                robot.delay(50);
                robot.mousePress(button);
                robot.delay(20);
                robot.mouseRelease(button);
                robot.delay(50);
                robot.mousePress(button);
                robot.delay(20);
                robot.mouseRelease(button);
                break;
        }

        // Add delay after click based on the pattern timing
        robot.delay((int) point.getDelay());
    }

    /**
     * Gets the button mask for Robot.
     */
    private int getButtonMask(String buttonName) {
        switch (buttonName) {
            case "LEFT":
                return java.awt.event.InputEvent.BUTTON1_DOWN_MASK;
            case "MIDDLE":
                return java.awt.event.InputEvent.BUTTON2_DOWN_MASK;
            case "RIGHT":
                return java.awt.event.InputEvent.BUTTON3_DOWN_MASK;
            default:
                return java.awt.event.InputEvent.BUTTON1_DOWN_MASK;
        }
    }

    /**
     * Saves a pattern to disk.
     */
    private void savePattern(ClickPattern pattern) {
        try {
            Path patternsDir = getPatternsDirectory();
            if (!Files.exists(patternsDir)) {
                Files.createDirectories(patternsDir);
            }

            File patternFile = new File(patternsDir.toFile(),
                    sanitizeFileName(pattern.getName()) + ".pattern");

            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(patternFile))) {
                out.writeObject(pattern);
                LOGGER.log(Level.INFO, "Pattern saved: {0}", pattern.getName());
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error saving pattern: " + pattern.getName(), e);
        }
    }

    /**
     * Loads all saved patterns.
     */
    private void loadSavedPatterns() {
        try {
            Path patternsDir = getPatternsDirectory();
            if (!Files.exists(patternsDir)) {
                Files.createDirectories(patternsDir);
                return;
            }

            File[] patternFiles = patternsDir.toFile().listFiles((dir, name) -> name.endsWith(".pattern"));
            if (patternFiles == null) return;

            for (File file : patternFiles) {
                try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
                    ClickPattern pattern = (ClickPattern) in.readObject();
                    savedPatterns.put(pattern.getName(), pattern);
                    LOGGER.log(Level.INFO, "Pattern loaded: {0}", pattern.getName());
                } catch (IOException | ClassNotFoundException e) {
                    LOGGER.log(Level.SEVERE, "Error loading pattern file " + file.getName(), e);
                }
            }

            // Notify listeners of pattern list
            notifyPatternListChanged();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error loading patterns", e);
        }
    }

    /**
     * Deletes a saved pattern.
     */
    @Override
    public boolean deletePattern(String name) {
        if (!savedPatterns.containsKey(name)) {
            return false;
        }

        try {
            // Remove from memory
            savedPatterns.remove(name);

            // Remove from disk
            File patternFile = new File(getPatternsDirectory().toFile(),
                    sanitizeFileName(name) + ".pattern");
            boolean deleted = patternFile.delete();

            // Notify listeners
            if (deleted) {
                notifyPatternListChanged();
            }

            return deleted;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting pattern", e);
            return false;
        }
    }

    /**
     * Gets a pattern by name.
     */
    @Override
    public ClickPattern getPattern(String name) {
        return savedPatterns.get(name);
    }

    /**
     * Gets all pattern names.
     */
    @Override
    public List<String> getPatternNames() {
        return new ArrayList<>(savedPatterns.keySet());
    }

    /**
     * Gets the patterns directory.
     */
    private Path getPatternsDirectory() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, AppConfig.SETTINGS_DIR_NAME, AppConfig.PATTERNS_FOLDER_NAME);
    }

    /**
     * Sanitizes a file name.
     */
    private String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    /**
     * Notifies listeners that the pattern list has changed.
     */
    private void notifyPatternListChanged() {
        List<String> patternNames = getPatternNames();
        for (PatternListener listener : listeners) {
            listener.onPatternListChanged(patternNames);
        }
    }

    /**
     * Checks if recording is in progress.
     */
    @Override
    public boolean isRecording() {
        return isRecording.get();
    }

    /**
     * Checks if playback is in progress.
     */
    @Override
    public boolean isPlaying() {
        return isPlaying.get();
    }

    /**
     * Checks if playback is paused.
     */
    @Override
    public boolean isPaused() {
        return isPaused.get();
    }

    /**
     * Gets the current pattern being recorded or played.
     */
    @Override
    public ClickPattern getCurrentPattern() {
        return currentPattern;
    }

    /**
     * Cleans up resources.
     */
    public void cleanup() {
        // Stop recording/playback
        if (isRecording.get()) {
            stopRecording();
        }

        if (isPlaying.get()) {
            stopPlayback();
        }

        // Unregister listener
        GlobalScreen.removeNativeMouseListener(this);
    }

    // NativeMouseListener implementation

    @Override
    public void nativeMouseClicked(NativeMouseEvent e) {
        // Not needed
    }

    @Override
    public void nativeMousePressed(NativeMouseEvent e) {
        if (isRecording.get()) {
            // Calculate delay from last click
            long currentTime = System.currentTimeMillis();
            long delay = currentTime - lastClickTime;
            lastClickTime = currentTime;

            // Get mouse button
            String button;
            switch (e.getButton()) {
                case NativeMouseEvent.BUTTON1:
                    button = "LEFT";
                    break;
                case NativeMouseEvent.BUTTON2:
                    button = "MIDDLE";
                    break;
                case NativeMouseEvent.BUTTON3:
                    button = "RIGHT";
                    break;
                default:
                    button = "LEFT";
            }

            // Create click point (using current click mode from settings)
            ClickPoint point = new ClickPoint(
                    e.getX(),
                    e.getY(),
                    delay,
                    button,
                    settingsManager.getClickMode()
            );

            // Add to pattern
            currentPattern.addClickPoint(point);

            // Notify listeners
            for (PatternListener listener : listeners) {
                listener.onPatternUpdated(currentPattern);
            }

            LOGGER.log(Level.FINE, "Recorded click: {0}", point);
        }
    }

    @Override
    public void nativeMouseReleased(NativeMouseEvent e) {
        // Not needed
    }
}