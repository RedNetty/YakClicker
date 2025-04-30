package com.autoclicker.service.click;

import com.autoclicker.core.service.ClickService;
import com.autoclicker.storage.SettingsManager;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Abstract base class for platform-specific click service implementations.
 * With improved fixed-rate scheduling system for more reliable clicking.
 */
public abstract class AbstractClickService implements ClickService {
    protected final SettingsManager settingsManager;
    protected final Random random = new Random();
    protected final AtomicBoolean isRunning = new AtomicBoolean(false);
    protected final AtomicBoolean isPaused = new AtomicBoolean(false);

    protected ScheduledExecutorService clickExecutor;
    protected ScheduledFuture<?> currentTask;
    protected Robot robot;

    // Constants for click events
    protected static final int LEFT_BUTTON = InputEvent.BUTTON1_DOWN_MASK;
    protected static final int MIDDLE_BUTTON = InputEvent.BUTTON2_DOWN_MASK;
    protected static final int RIGHT_BUTTON = InputEvent.BUTTON3_DOWN_MASK;

    // Click metrics
    private final AtomicLong clickCounter = new AtomicLong(0);
    private long lastResetTime = System.currentTimeMillis();
    private final Object cpsLock = new Object();
    private double measuredCps = 0.0;

    // Statistics
    private long totalClicks = 0;
    private long sessionClicks = 0;
    private double peakCps = 0.0;
    private double avgCps = 0.0;
    private int clickMeasureCount = 0;

    // Click listeners
    private final List<ClickListener> clickListeners = new ArrayList<>();

    public AbstractClickService(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;

        try {
            // Configure Robot
            this.robot = createRobot();
        } catch (AWTException e) {
            System.err.println("Error creating Robot: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Creates and configures a Robot instance.
     * Can be overridden by subclasses to provide platform-specific configuration.
     */
    protected Robot createRobot() throws AWTException {
        Robot robot = new Robot();
        robot.setAutoDelay(0);
        robot.setAutoWaitForIdle(false);
        return robot;
    }

    /**
     * Creates an optimized thread for click scheduling based on the required CPS.
     *
     * @param threadName The name for the thread
     * @param cps The clicks per second rate
     * @return A configured thread factory for optimal performance
     */
    protected ThreadFactory createOptimizedThreadFactory(String threadName, double cps) {
        return r -> {
            Thread thread = new Thread(r, threadName);
            thread.setDaemon(true);

            // For high CPS rates, use higher priority
            if (cps > 50) {
                thread.setPriority(Thread.MAX_PRIORITY);
            } else if (cps > 20) {
                thread.setPriority(Thread.NORM_PRIORITY + 1);
            }

            return thread;
        };
    }

    @Override
    public void startClicking() {
        if (isRunning.get() || robot == null) {
            return;
        }

        isRunning.set(true);
        isPaused.set(false);

        double cps = settingsManager.getCPS();
        long baseIntervalMs = Math.round(1000.0 / cps);

        // Create a new executor service with optimized thread factory
        if (clickExecutor == null || clickExecutor.isShutdown()) {
            clickExecutor = Executors.newSingleThreadScheduledExecutor(
                    createOptimizedThreadFactory("click-service-thread", cps));
        }

        // Schedule clicking at a fixed rate with optimized timing
        currentTask = clickExecutor.scheduleAtFixedRate(() -> {
            try {
                // Only perform click if running and not paused
                if (isRunning.get() && !isPaused.get()) {
                    // Perform the click operation
                    performClick();

                    // Add randomized delay if enabled
                    if (settingsManager.isRandomizeInterval()) {
                        long randomDelay = calculateRandomDelay(baseIntervalMs);
                        if (randomDelay > 0) {
                            // For better performance at high CPS, use a more efficient sleep method
                            if (cps > 50) {
                                // Use a busy-wait for very short delays
                                busyWaitSleep(randomDelay);
                            } else {
                                try {
                                    Thread.sleep(randomDelay);
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error in click task: " + e.getMessage());
                e.printStackTrace();
                // Continue execution despite errors
            }
        }, 0, baseIntervalMs, TimeUnit.MILLISECONDS);

        System.out.println("Clicking started at " + cps + " CPS");
    }

    /**
     * More precise sleep method for very small delays.
     * Uses busy-waiting for high precision at the cost of CPU usage.
     *
     * @param millis The number of milliseconds to wait
     */
    private void busyWaitSleep(long millis) {
        long startTime = System.nanoTime();
        long waitTimeNanos = millis * 1_000_000;

        while (System.nanoTime() - startTime < waitTimeNanos) {
            // Busy wait - more accurate for very short delays
            // For extremely high performance, can add Thread.onSpinWait() in Java 9+
        }
    }

    /**
     * Calculates a random delay based on the configured randomization factor.
     *
     * @param baseIntervalMs The base interval between clicks
     * @return A random delay in milliseconds
     */
    protected long calculateRandomDelay(long baseIntervalMs) {
        if (!settingsManager.isRandomizeInterval()) {
            return 0;
        }

        double factor = settingsManager.getRandomizationFactor();
        if (factor <= 0) {
            return 0;
        }

        // Use triangular distribution for more natural randomness
        double randomValue = triangularDistribution() * factor;
        return Math.round(baseIntervalMs * randomValue);
    }

    /**
     * Generates a random value using triangular distribution between -1 and 1.
     * This provides more natural randomness than uniform distribution.
     *
     * @return A value between -1 and 1, with higher probability near 0
     */
    protected double triangularDistribution() {
        double u = random.nextDouble(); // Uniform between 0 and 1
        double v = random.nextDouble(); // Another uniform between 0 and 1
        return u - v; // Results in triangular distribution between -1 and 1
    }

    @Override
    public void stopClicking() {
        isRunning.set(false);

        // Cancel the current task if it exists
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(false);
            currentTask = null;
        }

        // Shutdown the executor service
        if (clickExecutor != null && !clickExecutor.isShutdown()) {
            clickExecutor.shutdown();
            try {
                // Wait for tasks to terminate
                if (!clickExecutor.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                    clickExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                clickExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            clickExecutor = null;
        }

        System.out.println("Clicking stopped");
    }

    @Override
    public void toggleClicking() {
        if (isRunning.get()) {
            stopClicking();
        } else {
            startClicking();
        }
    }

    @Override
    public void pause() {
        isPaused.set(true);
    }

    @Override
    public void resume() {
        isPaused.set(false);
    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
    }

    @Override
    public boolean isPaused() {
        return isPaused.get();
    }

    /**
     * Performs a click operation.
     * Implemented by platform-specific subclasses.
     */
    protected abstract void performClick();

    /**
     * Performs a click operation and updates metrics.
     * Called by subclasses after performing the actual click.
     */
    protected void onClickPerformed() {
        // Increment click counter
        clickCounter.incrementAndGet();
        totalClicks++;
        sessionClicks++;

        // Notify settings manager of click for statistics
        settingsManager.recordClick();

        // Notify listeners
        for (ClickListener listener : clickListeners) {
            listener.onClickPerformed();
        }
    }

    /**
     * Maps the mouse button name to its corresponding InputEvent mask.
     */
    protected int getButtonMask(String buttonName) {
        switch (buttonName) {
            case "LEFT":
                return LEFT_BUTTON;
            case "MIDDLE":
                return MIDDLE_BUTTON;
            case "RIGHT":
                return RIGHT_BUTTON;
            default:
                return LEFT_BUTTON;
        }
    }

    @Override
    public void addClickListener(ClickListener listener) {
        clickListeners.add(listener);
    }

    @Override
    public void removeClickListener(ClickListener listener) {
        clickListeners.remove(listener);
    }

    @Override
    public double getMeasuredCPS() {
        synchronized (cpsLock) {
            return measuredCps;
        }
    }

    @Override
    public void updateMeasuredCPS() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastResetTime;

        // Only update if at least 1 second has passed
        if (elapsedTime >= 1000) {
            long clicks = clickCounter.getAndSet(0);
            double cps = (double) clicks * 1000 / elapsedTime;

            synchronized (cpsLock) {
                measuredCps = cps;

                // Update statistics
                if (cps > peakCps) {
                    peakCps = cps;
                }

                // Update average CPS
                avgCps = ((avgCps * clickMeasureCount) + cps) / (clickMeasureCount + 1);
                clickMeasureCount++;
            }

            // Record performance metric
            settingsManager.recordPerformanceMetric(cps);

            // Reset timer
            lastResetTime = currentTime;
        }
    }

    // Statistics methods

    @Override
    public long getTotalClicks() {
        return totalClicks;
    }

    @Override
    public long getSessionClicks() {
        return sessionClicks;
    }

    @Override
    public double getAverageCPS() {
        return avgCps;
    }

    @Override
    public double getMaxCPS() {
        return peakCps;
    }

    @Override
    public double getClickAccuracy() {
        double targetCps = settingsManager.getCPS();
        return targetCps > 0 ? Math.min(1.0, getMeasuredCPS() / targetCps) : 1.0;
    }

    @Override
    public void resetStatistics() {
        sessionClicks = 0;
        peakCps = 0.0;
        avgCps = 0.0;
        clickMeasureCount = 0;
        clickCounter.set(0);
    }
}