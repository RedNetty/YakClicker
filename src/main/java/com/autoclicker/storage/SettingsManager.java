package com.autoclicker.storage;

import com.autoclicker.config.AppConfig;
import com.autoclicker.core.model.ClickPattern;
import com.autoclicker.core.model.PerformanceMetric;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Manages application settings, user profiles, and performance metrics persistence.
 */
public class SettingsManager {

    // Logger for handling errors and information
    private static final Logger LOGGER = Logger.getLogger(SettingsManager.class.getName());

    // --- Settings Fields (Initialized with Defaults) ---
    private double cps = AppConfig.DEFAULT_CPS;
    private String clickMode = AppConfig.DEFAULT_CLICK_MODE;
    private String mouseButton = AppConfig.DEFAULT_MOUSE_BUTTON;
    private boolean randomizeInterval = AppConfig.DEFAULT_RANDOMIZE_INTERVAL;
    private double randomizationFactor = AppConfig.DEFAULT_RANDOMIZATION_FACTOR;
    private boolean randomMovement = AppConfig.DEFAULT_RANDOM_MOVEMENT;
    private int movementRadius = AppConfig.DEFAULT_MOVEMENT_RADIUS;
    private String toggleHotkey = AppConfig.DEFAULT_TOGGLE_HOTKEY;
    private String visibilityHotkey = AppConfig.DEFAULT_VISIBILITY_HOTKEY;
    private String pauseHotkey = AppConfig.DEFAULT_PAUSE_HOTKEY;
    private String increaseSpeedHotkey = AppConfig.DEFAULT_INCREASE_SPEED_HOTKEY;
    private String decreaseSpeedHotkey = AppConfig.DEFAULT_DECREASE_SPEED_HOTKEY;
    private boolean alwaysOnTop = AppConfig.DEFAULT_ALWAYS_ON_TOP;
    private double transparency = AppConfig.DEFAULT_TRANSPARENCY;
    private boolean minimizeToTray = AppConfig.DEFAULT_MINIMIZE_TO_TRAY;
    private boolean autoHide = AppConfig.DEFAULT_AUTO_HIDE;
    private boolean darkMode = AppConfig.DEFAULT_DARK_MODE;
    private boolean platformSpecificSettings = AppConfig.DEFAULT_PLATFORM_SPECIFIC_SETTINGS;

    // --- Profile Management ---
    // Use ConcurrentHashMap for thread-safe profile access
    private final Map<String, ClickPattern> profiles = new ConcurrentHashMap<>();
    private String currentProfileName = AppConfig.DEFAULT_PROFILE_NAME;

    // --- Performance Metrics ---
    private static final int MAX_PERFORMANCE_SAMPLES = 100;
    // Use CopyOnWriteArrayList for thread-safe reads and writes
    private final List<PerformanceMetric> performanceMetrics = new CopyOnWriteArrayList<>();
    private final AtomicLong totalClickCounter = new AtomicLong(0);

    // --- Paths ---
    private final Path settingsDirectory;
    private final Path settingsFile;
    private final Path profilesDirectory;

    /**
     * Default constructor. Initializes paths.
     * Settings are loaded lazily or explicitly via loadSettings().
     */
    public SettingsManager() {
        String userHome = System.getProperty("user.home");
        this.settingsDirectory = Paths.get(userHome, AppConfig.SETTINGS_DIR_NAME);
        this.settingsFile = settingsDirectory.resolve(AppConfig.SETTINGS_FILE_NAME);
        this.profilesDirectory = settingsDirectory.resolve(AppConfig.PATTERNS_FOLDER_NAME);
        // Note: Settings loading is deferred until needed or explicitly called.
    }

    // --- Initialization ---

    /**
     * Initializes the profiles system by creating the directory if needed
     * and loading existing profiles. Ensures a default profile exists.
     */
    public void initProfiles() {
        try {
            // Ensure profiles directory exists
            if (!Files.exists(profilesDirectory)) {
                Files.createDirectories(profilesDirectory);
                LOGGER.log(Level.INFO, "Created profiles directory: {0}", profilesDirectory);
                // Create a default profile since the directory was just created
                createDefaultProfile();
            } else {
                // Load existing profiles if the directory exists
                loadProfilesInternal();
            }
            // Ensure default profile exists even if loading failed or was empty
            if (!profiles.containsKey(AppConfig.DEFAULT_PROFILE_NAME)) {
                createDefaultProfile();
            }
            // Validate and potentially reset current profile name
            if (!profiles.containsKey(currentProfileName)) {
                LOGGER.log(Level.WARNING, "Current profile '{0}' not found, resetting to default.", currentProfileName);
                currentProfileName = AppConfig.DEFAULT_PROFILE_NAME;
                // Apply the default profile settings if the originally set one was invalid
                applyProfileSettings(profiles.get(AppConfig.DEFAULT_PROFILE_NAME));
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize profiles directory: " + profilesDirectory, e);
            // Attempt to create a default profile in memory anyway
            if (!profiles.containsKey(AppConfig.DEFAULT_PROFILE_NAME)) {
                createDefaultProfileInMemory();
            }
        }
    }

    /**
     * Creates the default profile based on the current default constants
     * and saves it to disk.
     */
    private void createDefaultProfile() {
        ClickPattern defaultProfile = createDefaultProfileInMemory();
        saveProfile(defaultProfile); // Save it to disk
    }

    /**
     * Creates the default profile in memory.
     */
    private ClickPattern createDefaultProfileInMemory() {
        ClickPattern defaultProfile = new ClickPattern(AppConfig.DEFAULT_PROFILE_NAME);

        // This is a placeholder until we properly implement the profile system
        profiles.put(AppConfig.DEFAULT_PROFILE_NAME, defaultProfile);
        LOGGER.log(Level.INFO, "Created default profile '{0}' in memory.", AppConfig.DEFAULT_PROFILE_NAME);
        return defaultProfile;
    }

    // --- Settings Persistence ---

    /**
     * Loads settings from the properties file. If the file doesn't exist
     * or an error occurs, default settings are used and saved.
     */
    public void loadSettings() {
        Properties properties = new Properties();

        if (Files.exists(settingsFile)) {
            try (InputStream in = Files.newInputStream(settingsFile)) {
                properties.load(in);
                LOGGER.log(Level.INFO, "Loading settings from: {0}", settingsFile);
                loadSettingsFromProperties(properties);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error loading settings file: " + settingsFile + ". Using defaults.", e);
                resetToDefaultsAndSave(); // Reset to defaults and save if loading failed
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.SEVERE, "Error parsing settings value. Using defaults.", e);
                resetToDefaultsAndSave(); // Reset to defaults and save if parsing failed
            }
        } else {
            LOGGER.log(Level.INFO, "Settings file not found: {0}. Using default settings and creating file.", settingsFile);
            resetToDefaultsAndSave(); // Use defaults and create the file
        }

        // After loading settings, ensure the profile system is initialized
        // and the correct profile's settings are potentially applied.
        initProfiles(); // Ensure profiles are loaded/initialized
        loadProfile(currentProfileName); // Apply settings from the current profile
    }

    /**
     * Helper method to parse properties and update settings fields.
     * Throws IllegalArgumentException for parsing errors.
     */
    private void loadSettingsFromProperties(Properties properties) {
        // Use helper methods for parsing and validation
        setCPS(getPropertyAsDouble(properties, "clicksPerSecond", AppConfig.DEFAULT_CPS));
        setClickMode(properties.getProperty("clickMode", AppConfig.DEFAULT_CLICK_MODE));
        setMouseButton(properties.getProperty("mouseButton", AppConfig.DEFAULT_MOUSE_BUTTON));
        setRandomizeInterval(getPropertyAsBoolean(properties, "randomizeInterval", AppConfig.DEFAULT_RANDOMIZE_INTERVAL));
        setRandomizationFactor(getPropertyAsDouble(properties, "randomizationFactor", AppConfig.DEFAULT_RANDOMIZATION_FACTOR));
        setRandomMovement(getPropertyAsBoolean(properties, "randomMovement", AppConfig.DEFAULT_RANDOM_MOVEMENT));
        setMovementRadius(getPropertyAsInt(properties, "movementRadius", AppConfig.DEFAULT_MOVEMENT_RADIUS));
        setToggleHotkey(properties.getProperty("toggleHotkey", AppConfig.DEFAULT_TOGGLE_HOTKEY));
        setVisibilityHotkey(properties.getProperty("visibilityHotkey", AppConfig.DEFAULT_VISIBILITY_HOTKEY));
        setPauseHotkey(properties.getProperty("pauseHotkey", AppConfig.DEFAULT_PAUSE_HOTKEY));
        setIncreaseSpeedHotkey(properties.getProperty("increaseSpeedHotkey", AppConfig.DEFAULT_INCREASE_SPEED_HOTKEY));
        setDecreaseSpeedHotkey(properties.getProperty("decreaseSpeedHotkey", AppConfig.DEFAULT_DECREASE_SPEED_HOTKEY));
        setAlwaysOnTop(getPropertyAsBoolean(properties, "alwaysOnTop", AppConfig.DEFAULT_ALWAYS_ON_TOP));
        setTransparency(getPropertyAsDouble(properties, "transparency", AppConfig.DEFAULT_TRANSPARENCY));
        setMinimizeToTray(getPropertyAsBoolean(properties, "minimizeToTray", AppConfig.DEFAULT_MINIMIZE_TO_TRAY));
        setAutoHide(getPropertyAsBoolean(properties, "autoHide", AppConfig.DEFAULT_AUTO_HIDE));
        setDarkMode(getPropertyAsBoolean(properties, "darkMode", AppConfig.DEFAULT_DARK_MODE));
        setPlatformSpecificSettings(getPropertyAsBoolean(properties, "platformSpecificSettings", AppConfig.DEFAULT_PLATFORM_SPECIFIC_SETTINGS));
        setCurrentProfileName(properties.getProperty("currentProfile", AppConfig.DEFAULT_PROFILE_NAME));
    }

    /**
     * Saves the current settings to the properties file.
     */
    public void saveSettings() {
        Properties properties = new Properties();

        // Store all settings using current field values
        properties.setProperty("clicksPerSecond", String.valueOf(this.cps));
        properties.setProperty("clickMode", this.clickMode);
        properties.setProperty("mouseButton", this.mouseButton);
        properties.setProperty("randomizeInterval", String.valueOf(this.randomizeInterval));
        properties.setProperty("randomizationFactor", String.valueOf(this.randomizationFactor));
        properties.setProperty("randomMovement", String.valueOf(this.randomMovement));
        properties.setProperty("movementRadius", String.valueOf(this.movementRadius));
        properties.setProperty("toggleHotkey", this.toggleHotkey);
        properties.setProperty("visibilityHotkey", this.visibilityHotkey);
        properties.setProperty("pauseHotkey", this.pauseHotkey);
        properties.setProperty("increaseSpeedHotkey", this.increaseSpeedHotkey);
        properties.setProperty("decreaseSpeedHotkey", this.decreaseSpeedHotkey);
        properties.setProperty("alwaysOnTop", String.valueOf(this.alwaysOnTop));
        properties.setProperty("transparency", String.valueOf(this.transparency));
        properties.setProperty("minimizeToTray", String.valueOf(this.minimizeToTray));
        properties.setProperty("autoHide", String.valueOf(this.autoHide));
        properties.setProperty("darkMode", String.valueOf(this.darkMode));
        properties.setProperty("platformSpecificSettings", String.valueOf(this.platformSpecificSettings));
        properties.setProperty("currentProfile", this.currentProfileName);

        try {
            // Ensure parent directory exists
            Files.createDirectories(settingsDirectory);

            // Save the properties file
            try (OutputStream out = Files.newOutputStream(settingsFile)) {
                properties.store(out, "YakClicker Settings");
                LOGGER.log(Level.INFO, "Settings saved to: {0}", settingsFile);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error saving settings to: " + settingsFile, e);
        }
    }

    /**
     * Resets all settings to their default values.
     * Does not automatically save; call saveSettings() explicitly if persistence is needed.
     */
    public void resetToDefaults() {
        LOGGER.log(Level.INFO, "Resetting settings to defaults.");
        this.cps = AppConfig.DEFAULT_CPS;
        this.clickMode = AppConfig.DEFAULT_CLICK_MODE;
        this.mouseButton = AppConfig.DEFAULT_MOUSE_BUTTON;
        this.randomizeInterval = AppConfig.DEFAULT_RANDOMIZE_INTERVAL;
        this.randomizationFactor = AppConfig.DEFAULT_RANDOMIZATION_FACTOR;
        this.randomMovement = AppConfig.DEFAULT_RANDOM_MOVEMENT;
        this.movementRadius = AppConfig.DEFAULT_MOVEMENT_RADIUS;
        this.toggleHotkey = AppConfig.DEFAULT_TOGGLE_HOTKEY;
        this.visibilityHotkey = AppConfig.DEFAULT_VISIBILITY_HOTKEY;
        this.pauseHotkey = AppConfig.DEFAULT_PAUSE_HOTKEY;
        this.increaseSpeedHotkey = AppConfig.DEFAULT_INCREASE_SPEED_HOTKEY;
        this.decreaseSpeedHotkey = AppConfig.DEFAULT_DECREASE_SPEED_HOTKEY;
        this.alwaysOnTop = AppConfig.DEFAULT_ALWAYS_ON_TOP;
        this.transparency = AppConfig.DEFAULT_TRANSPARENCY;
        this.minimizeToTray = AppConfig.DEFAULT_MINIMIZE_TO_TRAY;
        this.autoHide = AppConfig.DEFAULT_AUTO_HIDE;
        this.darkMode = AppConfig.DEFAULT_DARK_MODE;
        this.platformSpecificSettings = AppConfig.DEFAULT_PLATFORM_SPECIFIC_SETTINGS;
        this.currentProfileName = AppConfig.DEFAULT_PROFILE_NAME;

        // After resetting general settings, reload the default profile settings
        initProfiles(); // Ensure profiles are loaded/initialized
        loadProfile(AppConfig.DEFAULT_PROFILE_NAME);
    }

    /**
     * Resets settings to defaults and immediately saves them.
     */
    private void resetToDefaultsAndSave() {
        resetToDefaults();
        saveSettings();
    }

    // --- Property Parsing Helper Methods ---

    private double getPropertyAsDouble(Properties props, String key, double defaultValue) {
        String value = props.getProperty(key);
        if (value != null) {
            try {
                // Apply validation similar to the setter during load
                double parsedValue = Double.parseDouble(value);
                if (key.equals("clicksPerSecond")) return Math.max(0.1, Math.min(500.0, parsedValue));
                if (key.equals("randomizationFactor")) return Math.max(0.0, Math.min(1.0, parsedValue));
                if (key.equals("transparency")) return Math.max(0.2, Math.min(1.0, parsedValue));
                return parsedValue; // Return parsed value if no specific validation needed here
            } catch (NumberFormatException e) {
                LOGGER.log(Level.WARNING, "Invalid double value for key '{0}': '{1}'. Using default: {2}", new Object[]{key, value, defaultValue});
                // Throwing an exception allows the caller (loadSettings) to handle the overall failure
                throw new IllegalArgumentException("Invalid double value for key '" + key + "': '" + value + "'", e);
            }
        }
        return defaultValue;
    }

    private int getPropertyAsInt(Properties props, String key, int defaultValue) {
        String value = props.getProperty(key);
        if (value != null) {
            try {
                // Apply validation similar to the setter during load
                int parsedValue = Integer.parseInt(value);
                if (key.equals("movementRadius")) return Math.max(0, Math.min(20, parsedValue));
                return parsedValue; // Return parsed value if no specific validation needed here
            } catch (NumberFormatException e) {
                LOGGER.log(Level.WARNING, "Invalid integer value for key '{0}': '{1}'. Using default: {2}", new Object[]{key, value, defaultValue});
                throw new IllegalArgumentException("Invalid integer value for key '" + key + "': '" + value + "'", e);
            }
        }
        return defaultValue;
    }

    private boolean getPropertyAsBoolean(Properties props, String key, boolean defaultValue) {
        String value = props.getProperty(key, String.valueOf(defaultValue));
        // Boolean.parseBoolean is quite lenient, returns false for anything other than "true" (case-insensitive)
        return Boolean.parseBoolean(value);
    }

    // --- Profile Management Methods ---

    /**
     * Internal method to load all profiles from the profiles directory.
     */
    private void loadProfilesInternal() {
        profiles.clear(); // Clear existing in-memory profiles before loading
        LOGGER.log(Level.INFO, "Loading profiles from: {0}", profilesDirectory);

        // Check if directory exists before attempting to list files
        if (!Files.isDirectory(profilesDirectory)) {
            LOGGER.log(Level.WARNING, "Profiles directory does not exist or is not a directory: {0}", profilesDirectory);
            return; // Nothing to load
        }

        try (Stream<Path> stream = Files.list(profilesDirectory)) {
            stream.filter(path -> path.toString().endsWith(".pattern"))
                    .forEach(this::loadSingleProfile);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error listing profiles in directory: " + profilesDirectory, e);
        }
        LOGGER.log(Level.INFO, "Loaded {0} profiles.", profiles.size());
    }

    /**
     * Loads a single profile file.
     * @param profilePath Path to the .profile file.
     */
    private void loadSingleProfile(Path profilePath) {
        try (InputStream fis = Files.newInputStream(profilePath);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            ClickPattern profile = (ClickPattern) ois.readObject();
            if (profile != null && profile.getName() != null) {
                profiles.put(profile.getName(), profile);
                LOGGER.log(Level.FINE, "Successfully loaded profile: {0}", profile.getName());
            } else {
                LOGGER.log(Level.WARNING, "Loaded profile file {0} contained null or unnamed profile.", profilePath);
            }
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            LOGGER.log(Level.SEVERE, "Error loading profile file: " + profilePath, e);
            // Consider moving or renaming the corrupted file to prevent repeated load errors
        }
    }

    /**
     * Saves a ClickPattern object to a file in the profiles directory.
     * The filename is derived from the profile name.
     *
     * @param profile The ClickPattern to save.
     */
    public void saveProfile(ClickPattern profile) {
        if (profile == null || profile.getName() == null || profile.getName().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to save a null or unnamed profile.");
            return;
        }

        String sanitizedFileName = sanitizeFileName(profile.getName()) + ".pattern";
        Path profileFile = profilesDirectory.resolve(sanitizedFileName);

        try {
            // Ensure the directory exists
            Files.createDirectories(profilesDirectory);
            // Write the profile object
            try (OutputStream fos = Files.newOutputStream(profileFile);
                 ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                oos.writeObject(profile);
                LOGGER.log(Level.INFO, "Profile saved: {0} to {1}", new Object[]{profile.getName(), profileFile});
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error saving profile: " + profile.getName() + " to " + profileFile, e);
        }
    }

    /**
     * Creates a new ClickPattern instance based on the current core settings
     * (CPS, click mode, mouse button, randomization, movement).
     *
     * @param name The name for the new profile.
     * @return A new ClickPattern instance.
     */
    public ClickPattern createProfileFromCurrentSettings(String name) {
        ClickPattern profile = new ClickPattern(name);
        // Set properties based on current settings - this is a placeholder
        // until we properly implement the profile system
        return profile;
    }

    /**
     * Saves the current core settings as a new profile with the given name.
     * Updates the current profile to this new one and saves general settings.
     *
     * @param name The name for the new profile. Must not be null or empty.
     */
    public void saveCurrentSettingsAsProfile(String name) {
        if (name == null || name.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Profile name cannot be empty.");
            return; // Or throw IllegalArgumentException
        }

        ClickPattern profile = createProfileFromCurrentSettings(name);
        profiles.put(name, profile); // Add to in-memory map
        saveProfile(profile);       // Save profile object to its file
        setCurrentProfileName(name); // Set as current (this also saves settings)
        LOGGER.log(Level.INFO, "Saved current settings as new profile: {0}", name);
    }

    /**
     * Updates the currently active profile file with the current core settings values.
     * If the current profile doesn't exist, it logs a warning.
     */
    public void updateCurrentProfile() {
        if (!profiles.containsKey(currentProfileName)) {
            LOGGER.log(Level.WARNING, "Cannot update profile. Current profile '{0}' does not exist.", currentProfileName);
            return;
        }

        ClickPattern profile = profiles.get(currentProfileName);
        if (profile == null) {
            LOGGER.log(Level.SEVERE, "Current profile '{0}' is null in the map unexpectedly.", currentProfileName);
            return;
        }

        // Save the updated profile object back to its file
        saveProfile(profile);
        LOGGER.log(Level.INFO, "Updated profile: {0}", currentProfileName);
    }

    /**
     * Loads settings from the profile with the specified name and applies them.
     * Updates the current profile name and saves the general settings file
     * to persist the change in the active profile.
     *
     * @param name The name of the profile to load.
     */
    public void loadProfile(String name) {
        if (name == null) {
            LOGGER.log(Level.WARNING, "Attempted to load a null profile name. Using default.");
            name = AppConfig.DEFAULT_PROFILE_NAME;
        }

        ClickPattern profile = profiles.get(name);

        if (profile != null) {
            applyProfileSettings(profile);
            this.currentProfileName = name;
            saveSettings(); // Persist the change of the current profile name
            LOGGER.log(Level.INFO, "Loaded and applied profile: {0}", name);
        } else {
            LOGGER.log(Level.WARNING, "Profile '{0}' not found. Cannot load.", name);
        }
    }

    /**
     * Applies the settings from a ClickPattern object to the SettingsManager fields.
     * @param profile The profile whose settings should be applied.
     */
    private void applyProfileSettings(ClickPattern profile) {
        if (profile == null) return;

        // This is a placeholder method until we properly implement profile settings
    }

    /**
     * Deletes the profile with the given name, both from memory and disk.
     * Cannot delete the "Default" profile. If the deleted profile was active,
     * it switches to the "Default" profile.
     *
     * @param name The name of the profile to delete.
     * @return true if the profile was successfully deleted, false otherwise.
     */
    public boolean deleteProfile(String name) {
        if (AppConfig.DEFAULT_PROFILE_NAME.equalsIgnoreCase(name)) {
            LOGGER.log(Level.WARNING, "Cannot delete the default profile.");
            return false;
        }

        if (profiles.containsKey(name)) {
            String sanitizedFileName = sanitizeFileName(name) + ".pattern";
            Path profileFile = profilesDirectory.resolve(sanitizedFileName);
            boolean deleted = false;
            try {
                deleted = Files.deleteIfExists(profileFile);
                if (deleted) {
                    profiles.remove(name); // Remove from memory map
                    LOGGER.log(Level.INFO, "Deleted profile: {0}", name);

                    // If the deleted profile was the current one, switch to default
                    if (name.equals(currentProfileName)) {
                        LOGGER.log(Level.INFO, "Current profile deleted, switching to Default.");
                        loadProfile(AppConfig.DEFAULT_PROFILE_NAME); // This loads settings and saves the change
                    }
                    return true;
                } else {
                    LOGGER.log(Level.WARNING, "Profile file not found or could not be deleted: {0}", profileFile);
                    // If file wasn't there, still remove from map if it exists?
                    if(profiles.containsKey(name)){
                        profiles.remove(name);
                        // Check if current profile needs reset if map had it but file didn't exist
                        if (name.equals(currentProfileName)) {
                            loadProfile(AppConfig.DEFAULT_PROFILE_NAME);
                        }
                        return true; // Consider it "deleted" if it wasn't on disk but was in memory
                    }
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error deleting profile file: " + profileFile, e);
            }
        } else {
            LOGGER.log(Level.WARNING, "Profile '{0}' not found, cannot delete.", name);
        }

        return false; // Deletion failed or profile didn't exist
    }

    /**
     * Gets a sorted list of all available profile names, with "Default" always first.
     *
     * @return A list of profile names.
     */
    public List<String> getProfileNames() {
        // Create a list from the keyset of the profile map
        List<String> names = new ArrayList<>(profiles.keySet());
        // Sort alphabetically
        Collections.sort(names);

        // Ensure "Default" profile is moved to the beginning if it exists
        if (names.remove(AppConfig.DEFAULT_PROFILE_NAME)) {
            names.add(0, AppConfig.DEFAULT_PROFILE_NAME);
        }

        return names;
    }

    /**
     * Gets the name of the currently active profile.
     *
     * @return The current profile name.
     */
    public String getCurrentProfileName() {
        // Ensure the current profile actually exists, otherwise return default
        if (!profiles.containsKey(currentProfileName)) {
            LOGGER.log(Level.WARNING,"Current profile '{0}' seems invalid, returning default name.", currentProfileName);
            return AppConfig.DEFAULT_PROFILE_NAME;
        }
        return currentProfileName;
    }

    /**
     * Sets the current profile name. This does *not* load the profile's settings;
     * call loadProfile() for that. It primarily updates the name to be saved
     * in the main settings file.
     *
     * @param name The name of the profile to set as current.
     */
    public void setCurrentProfileName(String name) {
        if (name != null && profiles.containsKey(name)) {
            if (!this.currentProfileName.equals(name)) {
                this.currentProfileName = name;
                // Save the settings file to persist the change in the current profile name
                saveSettings();
                LOGGER.log(Level.FINE, "Current profile name set to: {0}", name);
            }
        } else {
            LOGGER.log(Level.WARNING, "Attempted to set current profile to an invalid or non-existent name: {0}", name);
        }
    }

    /**
     * Gets the ClickPattern object for the given name.
     *
     * @param name The name of the profile.
     * @return The ClickPattern object, or null if not found.
     */
    public ClickPattern getProfile(String name) {
        return profiles.get(name);
    }

    /**
     * Sanitizes a string to be used as part of a filename.
     * Replaces characters potentially problematic in filenames with underscores.
     *
     * @param name The original string.
     * @return A sanitized string suitable for filenames.
     */
    private String sanitizeFileName(String name) {
        // Keep alphanumeric, dots, hyphens. Replace others with underscore.
        return name.replaceAll("[^a-zA-Z0-9.\\-]", "_");
    }

    // --- Performance Metrics Methods ---

    /**
     * Increments the total click counter.
     */
    public void recordClick() {
        totalClickCounter.incrementAndGet();
    }

    /**
     * Gets the total number of clicks recorded since application start or reset.
     *
     * @return The total click count.
     */
    public long getTotalClicks() {
        return totalClickCounter.get();
    }

    /**
     * Records a performance data point including target CPS, actual CPS, and CPU usage.
     * Keeps only the latest MAX_PERFORMANCE_SAMPLES data points.
     *
     * @param actualCps The measured actual clicks per second for the period.
     */
    public void recordPerformanceMetric(double actualCps) {
        long cpuUsage = getProcessCpuUsage(); // Get current CPU usage

        PerformanceMetric metric = new PerformanceMetric(
                System.currentTimeMillis(),
                getCPS(), // Target CPS from current settings
                actualCps,
                cpuUsage
        );

        performanceMetrics.add(metric);

        // Maintain the size limit - remove oldest entries if exceeding max samples
        while (performanceMetrics.size() > MAX_PERFORMANCE_SAMPLES) {
            performanceMetrics.remove(0); // Remove the oldest metric
        }
        LOGGER.log(Level.FINEST, "Recorded performance metric: {0}", metric);
    }

    /**
     * Gets a snapshot of the recent performance metrics.
     * Returns a copy to prevent modification of the internal list.
     *
     * @return A list of recent PerformanceMetric objects.
     */
    public List<PerformanceMetric> getPerformanceMetrics() {
        // CopyOnWriteArrayList provides a thread-safe iterator, creating a snapshot.
        // Creating a new ArrayList ensures the caller gets a mutable copy independent of the internal list.
        return new ArrayList<>(performanceMetrics);
    }

    /**
     * Clears all recorded performance metrics.
     */
    public void clearPerformanceMetrics() {
        performanceMetrics.clear();
        LOGGER.log(Level.INFO, "Performance metrics cleared.");
    }

    /**
     * Attempts to get the current JVM process CPU usage as a percentage * 100.
     * Relies on com.sun.management MXBean, which might not be available on all JVMs.
     *
     * @return CPU usage (0-10000, e.g., 1500 for 15%), or 0 if unable to retrieve.
     */
    private long getProcessCpuUsage() {
        try {
            java.lang.management.OperatingSystemMXBean osBean =
                    ManagementFactory.getOperatingSystemMXBean();

            // Check if it's the extended Sun/Oracle/OpenJDK bean
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean =
                        (com.sun.management.OperatingSystemMXBean) osBean;

                // getProcessCpuLoad returns a value between 0.0 and 1.0
                double cpuLoad = sunOsBean.getProcessCpuLoad();
                if (cpuLoad >= 0) {
                    return Math.round(cpuLoad * 10000); // Convert to percentage * 100
                }
            }
        } catch (Exception e) {
            // Log the first time this fails, then maybe suppress subsequent logs?
            LOGGER.log(Level.FINE, "Could not retrieve process CPU usage via com.sun.management.OperatingSystemMXBean.", e);
        }
        // Return 0 if unavailable or an error occurred
        return 0;
    }

    // --- Getters and Setters for Settings (with validation) ---

    public double getCPS() { return cps; }
    public void setCPS(double cps) {
        this.cps = Math.max(0.1, Math.min(500.0, cps)); // Validate range
    }

    public String getClickMode() { return clickMode; }
    public void setClickMode(String clickMode) {
        // Basic validation, could be expanded (e.g., check against a Set of valid modes)
        if (clickMode != null && !clickMode.trim().isEmpty()) {
            this.clickMode = clickMode;
        } else {
            this.clickMode = AppConfig.DEFAULT_CLICK_MODE;
            LOGGER.log(Level.WARNING, "Attempted to set invalid click mode, using default.");
        }
    }

    public String getMouseButton() { return mouseButton; }
    public void setMouseButton(String mouseButton) {
        // Basic validation
        if (mouseButton != null && !mouseButton.trim().isEmpty()) {
            this.mouseButton = mouseButton;
        } else {
            this.mouseButton = AppConfig.DEFAULT_MOUSE_BUTTON;
            LOGGER.log(Level.WARNING, "Attempted to set invalid mouse button, using default.");
        }
    }

    public boolean isRandomizeInterval() { return randomizeInterval; }
    public void setRandomizeInterval(boolean randomizeInterval) { this.randomizeInterval = randomizeInterval; }

    public double getRandomizationFactor() { return randomizationFactor; }
    public void setRandomizationFactor(double randomizationFactor) {
        this.randomizationFactor = Math.max(0.0, Math.min(1.0, randomizationFactor)); // Validate range
    }

    public boolean isRandomMovement() { return randomMovement; }
    public void setRandomMovement(boolean randomMovement) { this.randomMovement = randomMovement; }

    public int getMovementRadius() { return movementRadius; }
    public void setMovementRadius(int movementRadius) {
        this.movementRadius = Math.max(0, Math.min(20, movementRadius)); // Validate range
    }

    public String getToggleHotkey() { return toggleHotkey; }
    public void setToggleHotkey(String toggleHotkey) { this.toggleHotkey = (toggleHotkey != null) ? toggleHotkey : AppConfig.DEFAULT_TOGGLE_HOTKEY; }

    public String getVisibilityHotkey() { return visibilityHotkey; }
    public void setVisibilityHotkey(String visibilityHotkey) { this.visibilityHotkey = (visibilityHotkey != null) ? visibilityHotkey : AppConfig.DEFAULT_VISIBILITY_HOTKEY; }

    public String getPauseHotkey() { return pauseHotkey; }
    public void setPauseHotkey(String pauseHotkey) { this.pauseHotkey = (pauseHotkey != null) ? pauseHotkey : AppConfig.DEFAULT_PAUSE_HOTKEY; }

    public String getIncreaseSpeedHotkey() { return increaseSpeedHotkey; }
    public void setIncreaseSpeedHotkey(String increaseSpeedHotkey) { this.increaseSpeedHotkey = (increaseSpeedHotkey != null) ? increaseSpeedHotkey : AppConfig.DEFAULT_INCREASE_SPEED_HOTKEY; }

    public String getDecreaseSpeedHotkey() { return decreaseSpeedHotkey; }
    public void setDecreaseSpeedHotkey(String decreaseSpeedHotkey) { this.decreaseSpeedHotkey = (decreaseSpeedHotkey != null) ? decreaseSpeedHotkey : AppConfig.DEFAULT_DECREASE_SPEED_HOTKEY; }

    public boolean isAlwaysOnTop() { return alwaysOnTop; }
    public void setAlwaysOnTop(boolean alwaysOnTop) { this.alwaysOnTop = alwaysOnTop; }

    public double getTransparency() { return transparency; }
    public void setTransparency(double transparency) {
        this.transparency = Math.max(0.2, Math.min(1.0, transparency)); // Validate range
    }

    public boolean isMinimizeToTray() { return minimizeToTray; }
    public void setMinimizeToTray(boolean minimizeToTray) { this.minimizeToTray = minimizeToTray; }

    public boolean isAutoHide() { return autoHide; }
    public void setAutoHide(boolean autoHide) { this.autoHide = autoHide; }

    public boolean isDarkMode() { return darkMode; }
    public void setDarkMode(boolean darkMode) { this.darkMode = darkMode; }

    public boolean isPlatformSpecificSettings() { return platformSpecificSettings; }
    public void setPlatformSpecificSettings(boolean platformSpecificSettings) { this.platformSpecificSettings = platformSpecificSettings; }
}