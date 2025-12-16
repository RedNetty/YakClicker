package com.autoclicker.core.service;

import com.autoclicker.core.model.ClickPattern;
import java.util.List;

/**
 * Interface for services that record and play back click patterns.
 */
public interface PatternService {

    /**
     * Starts recording a new click pattern.
     *
     * @param patternName The name of the pattern to record
     */
    void startRecording(String patternName);

    /**
     * Stops the current recording.
     */
    void stopRecording();

    /**
     * Starts playing a pattern.
     *
     * @param patternName The name of the pattern to play
     * @param loop Whether to loop the pattern
     */
    void playPattern(String patternName, boolean loop);

    /**
     * Stops the current pattern playback.
     */
    void stopPlayback();

    /**
     * Pauses or resumes the current pattern playback.
     */
    void pauseResumePlayback();

    /**
     * Deletes a saved pattern.
     *
     * @param name The name of the pattern to delete
     * @return true if the pattern was deleted, false otherwise
     */
    boolean deletePattern(String name);

    /**
     * Gets a pattern by name.
     *
     * @param name The name of the pattern
     * @return The pattern, or null if not found
     */
    ClickPattern getPattern(String name);

    /**
     * Gets all pattern names.
     *
     * @return A list of all pattern names
     */
    List<String> getPatternNames();

    /**
     * Checks if recording is in progress.
     *
     * @return true if recording, false otherwise
     */
    boolean isRecording();

    /**
     * Checks if playback is in progress.
     *
     * @return true if playing, false otherwise
     */
    boolean isPlaying();

    /**
     * Checks if playback is paused.
     *
     * @return true if paused, false otherwise
     */
    boolean isPaused();

    /**
     * Gets the current pattern being recorded or played.
     *
     * @return The current pattern
     */
    ClickPattern getCurrentPattern();

    /**
     * Adds a listener for pattern service events.
     *
     * @param listener The listener to add
     */
    void addListener(PatternListener listener);

    /**
     * Removes a listener for pattern service events.
     *
     * @param listener The listener to remove
     */
    void removeListener(PatternListener listener);

    /**
     * Interface for pattern service event listeners.
     */
    interface PatternListener {
        /**
         * Called when recording state changes.
         *
         * @param isRecording Whether recording is active
         */
        void onRecordingStateChanged(boolean isRecording);

        /**
         * Called when playback state changes.
         *
         * @param isPlaying Whether playback is active
         * @param isPaused Whether playback is paused
         */
        void onPlaybackStateChanged(boolean isPlaying, boolean isPaused);

        /**
         * Called when a pattern is updated.
         *
         * @param pattern The updated pattern
         */
        void onPatternUpdated(ClickPattern pattern);

        /**
         * Called when the pattern list changes.
         *
         * @param patternNames The new list of pattern names
         */
        void onPatternListChanged(List<String> patternNames);
    }
}