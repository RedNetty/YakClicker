package com.autoclicker.core.service;

/**
 * Interface defining the common operations for all click service implementations.
 * Enhanced with statistics tracking methods.
 */
public interface ClickService {

    /**
     * Starts the clicking process.
     */
    void startClicking();

    /**
     * Stops the clicking process.
     */
    void stopClicking();

    /**
     * Toggles between running and stopped states.
     */
    void toggleClicking();

    /**
     * Pauses clicking temporarily.
     */
    void pause();

    /**
     * Resumes clicking from a paused state.
     */
    void resume();

    /**
     * Checks if the clicker is currently running.
     * @return true if running, false otherwise
     */
    boolean isRunning();

    /**
     * Checks if the clicker is currently paused.
     * @return true if paused, false otherwise
     */
    boolean isPaused();

    /**
     * Gets the measured clicks per second.
     * @return The actual CPS being achieved
     */
    double getMeasuredCPS();

    /**
     * Updates internal CPS measurement.
     * Should be called periodically.
     */
    void updateMeasuredCPS();

    /**
     * Gets the total number of clicks performed.
     * @return Total clicks since application start
     */
    long getTotalClicks();

    /**
     * Gets the number of clicks in the current session.
     * @return Clicks in this session
     */
    long getSessionClicks();

    /**
     * Gets the average CPS.
     * @return Average CPS over time
     */
    double getAverageCPS();

    /**
     * Gets the maximum CPS achieved.
     * @return Peak CPS achieved
     */
    double getMaxCPS();

    /**
     * Gets the click accuracy.
     * @return Click accuracy as a ratio (0.0 to 1.0)
     */
    double getClickAccuracy();

    /**
     * Resets the statistics.
     */
    void resetStatistics();

    /**
     * Adds a click event listener.
     * @param listener The listener to add
     */
    void addClickListener(ClickListener listener);

    /**
     * Removes a click event listener.
     * @param listener The listener to remove
     */
    void removeClickListener(ClickListener listener);

    /**
     * Interface for click event listeners.
     */
    interface ClickListener {
        /**
         * Called when a click is performed.
         */
        void onClickPerformed();
    }
}