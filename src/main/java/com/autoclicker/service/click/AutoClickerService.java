package com.autoclicker.service.click;

import com.autoclicker.core.service.ClickService;
import com.autoclicker.service.factory.ClickServiceFactory;
import com.autoclicker.storage.SettingsManager;

/**
 * Main auto-clicker service class that delegates to platform-specific implementations.
 * This class maintains the original API expected by the UI components.
 * Enhanced with statistics tracking and click event listeners.
 */
public class AutoClickerService implements ClickService {
    private final ClickService clickService;
    private final SettingsManager settingsManager;

    /**
     * Creates a new AutoClickerService that uses the appropriate platform-specific implementation.
     *
     * @param settingsManager The settings manager instance
     */
    public AutoClickerService(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
        this.clickService = ClickServiceFactory.createClickService(settingsManager);
    }

    @Override
    public void startClicking() {
        clickService.startClicking();
    }

    @Override
    public void stopClicking() {
        clickService.stopClicking();
    }

    @Override
    public void toggleClicking() {
        clickService.toggleClicking();
    }

    @Override
    public void pause() {
        clickService.pause();
    }

    @Override
    public void resume() {
        clickService.resume();
    }

    @Override
    public boolean isRunning() {
        return clickService.isRunning();
    }

    @Override
    public boolean isPaused() {
        return clickService.isPaused();
    }

    @Override
    public double getMeasuredCPS() {
        return clickService.getMeasuredCPS();
    }

    @Override
    public void updateMeasuredCPS() {
        clickService.updateMeasuredCPS();
    }

    @Override
    public long getTotalClicks() {
        return clickService.getTotalClicks();
    }

    @Override
    public long getSessionClicks() {
        return clickService.getSessionClicks();
    }

    @Override
    public double getAverageCPS() {
        return clickService.getAverageCPS();
    }

    @Override
    public double getMaxCPS() {
        return clickService.getMaxCPS();
    }

    @Override
    public double getClickAccuracy() {
        return clickService.getClickAccuracy();
    }

    @Override
    public void resetStatistics() {
        clickService.resetStatistics();
    }

    @Override
    public void addClickListener(ClickListener listener) {
        clickService.addClickListener(listener);
    }

    @Override
    public void removeClickListener(ClickListener listener) {
        clickService.removeClickListener(listener);
    }
}