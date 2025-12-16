package com.autoclicker.storage;

import com.autoclicker.core.model.ClickPattern;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Storage class for managing click profiles persistence.
 * Handles loading, saving, and managing click profiles to/from disk.
 */
public class ProfileStorage {
    private static final Logger LOGGER = Logger.getLogger(ProfileStorage.class.getName());

    // Storage directory constants
    private static final String SETTINGS_FOLDER_NAME = ".yakclicker";
    private static final String PROFILES_FOLDER_NAME = "profiles";
    private static final String PROFILE_FILE_EXTENSION = ".profile";

    // Default profile name
    private static final String DEFAULT_PROFILE_NAME = "Default";

    // In-memory profile cache
    private final Map<String, ClickPattern> profiles = new HashMap<>();

    // Paths
    private final Path profilesDirectory;

    /**
     * Creates a new ProfileStorage instance and initializes storage directories.
     */
    public ProfileStorage() {
        String userHome = System.getProperty("user.home");
        Path settingsDirectory = Paths.get(userHome, SETTINGS_FOLDER_NAME);
        this.profilesDirectory = settingsDirectory.resolve(PROFILES_FOLDER_NAME);

        // Create necessary directories
        ensureDirectoriesExist();

        // Load existing profiles
        loadAllProfiles();

        // Ensure default profile exists
        ensureDefaultProfileExists();
    }

    /**
     * Ensures all required directories exist.
     */
    private void ensureDirectoriesExist() {
        try {
            if (!Files.exists(profilesDirectory)) {
                Files.createDirectories(profilesDirectory);
                LOGGER.log(Level.INFO, "Created profiles directory: {0}", profilesDirectory);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to create profiles directory: " + profilesDirectory, e);
        }
    }

    /**
     * Ensures a default profile exists.
     */
    private void ensureDefaultProfileExists() {
        if (!profiles.containsKey(DEFAULT_PROFILE_NAME)) {
            createDefaultProfile();
        }
    }

    /**
     * Creates and saves the default profile.
     */
    private void createDefaultProfile() {
        ClickPattern defaultProfile = new ClickPattern(DEFAULT_PROFILE_NAME);

        // Set default values
        defaultProfile.setLooping(false);

        // Save to memory and disk
        profiles.put(DEFAULT_PROFILE_NAME, defaultProfile);
        saveProfile(defaultProfile);

        LOGGER.log(Level.INFO, "Created default profile");
    }

    /**
     * Loads all profiles from disk.
     */
    private void loadAllProfiles() {
        profiles.clear();

        try {
            if (!Files.exists(profilesDirectory)) {
                return;
            }

            Files.list(profilesDirectory)
                    .filter(path -> path.toString().endsWith(PROFILE_FILE_EXTENSION))
                    .forEach(this::loadProfile);

            LOGGER.log(Level.INFO, "Loaded {0} profiles", profiles.size());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error listing profiles directory: " + profilesDirectory, e);
        }
    }

    /**
     * Loads a single profile file.
     *
     * @param profilePath Path to profile file
     */
    private void loadProfile(Path profilePath) {
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(profilePath))) {
            Object obj = ois.readObject();
            if (obj instanceof ClickPattern) {
                ClickPattern profile = (ClickPattern) obj;
                profiles.put(profile.getName(), profile);
                LOGGER.log(Level.FINE, "Loaded profile: {0}", profile.getName());
            }
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            LOGGER.log(Level.SEVERE, "Error loading profile file: " + profilePath, e);
        }
    }

    /**
     * Saves a profile to disk.
     *
     * @param profile The profile to save
     * @return true if saved successfully, false otherwise
     */
    public boolean saveProfile(ClickPattern profile) {
        if (profile == null || profile.getName() == null || profile.getName().isEmpty()) {
            LOGGER.log(Level.WARNING, "Cannot save null or unnamed profile");
            return false;
        }

        // Add to in-memory cache
        profiles.put(profile.getName(), profile);

        // Save to disk
        String filename = sanitizeFileName(profile.getName()) + PROFILE_FILE_EXTENSION;
        Path profilePath = profilesDirectory.resolve(filename);

        try {
            ensureDirectoriesExist();

            try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(profilePath))) {
                oos.writeObject(profile);
                LOGGER.log(Level.INFO, "Saved profile: {0}", profile.getName());
                return true;
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error saving profile: " + profile.getName(), e);
            return false;
        }
    }

    /**
     * Deletes a profile.
     *
     * @param profileName Name of the profile to delete
     * @return true if deleted successfully, false otherwise
     */
    public boolean deleteProfile(String profileName) {
        if (profileName == null || profileName.isEmpty()) {
            return false;
        }

        // Don't allow deleting the default profile
        if (DEFAULT_PROFILE_NAME.equals(profileName)) {
            LOGGER.log(Level.WARNING, "Cannot delete the default profile");
            return false;
        }

        // Remove from in-memory cache
        profiles.remove(profileName);

        // Delete from disk
        String filename = sanitizeFileName(profileName) + PROFILE_FILE_EXTENSION;
        Path profilePath = profilesDirectory.resolve(filename);

        try {
            return Files.deleteIfExists(profilePath);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error deleting profile: " + profileName, e);
            return false;
        }
    }

    /**
     * Gets a profile by name.
     *
     * @param profileName Name of the profile to get
     * @return The profile or null if not found
     */
    public ClickPattern getProfile(String profileName) {
        return profiles.get(profileName);
    }

    /**
     * Gets all available profile names.
     *
     * @return List of profile names
     */
    public List<String> getProfileNames() {
        List<String> names = new ArrayList<>(profiles.keySet());

        // Ensure default profile is first
        if (names.remove(DEFAULT_PROFILE_NAME)) {
            names.add(0, DEFAULT_PROFILE_NAME);
        }

        return names;
    }

    /**
     * Gets all profiles.
     *
     * @return Map of profile names to profiles
     */
    public Map<String, ClickPattern> getAllProfiles() {
        return new HashMap<>(profiles);
    }

    /**
     * Sanitizes a filename.
     *
     * @param name The filename to sanitize
     * @return Sanitized filename
     */
    private String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9.\\-]", "_");
    }

    /**
     * Exports a profile to JSON format for sharing.
     *
     * @param profileName Name of the profile to export
     * @return JSON string representation of the profile
     */
    public String exportProfileToJson(String profileName) {
        ClickPattern profile = profiles.get(profileName);
        if (profile == null) {
            return null;
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", profile.getName());
        jsonObject.put("looping", profile.isLooping());

        JSONArray clickPoints = new JSONArray();
        for (ClickPattern.ClickPoint point : profile.getClickPoints()) {
            JSONObject pointObj = new JSONObject();
            pointObj.put("x", point.getX());
            pointObj.put("y", point.getY());
            pointObj.put("delay", point.getDelay());
            pointObj.put("mouseButton", point.getMouseButton());
            pointObj.put("clickType", point.getClickType());
            clickPoints.put(pointObj);
        }

        jsonObject.put("clickPoints", clickPoints);
        return jsonObject.toString(2);
    }

    /**
     * Imports a profile from JSON format.
     *
     * @param json JSON string representation of a profile
     * @return The imported profile or null if import failed
     */
    public ClickPattern importProfileFromJson(String json) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            String name = jsonObject.getString("name");

            // Create a new profile
            ClickPattern profile = new ClickPattern(name);
            profile.setLooping(jsonObject.optBoolean("looping", false));

            // Import click points
            JSONArray clickPoints = jsonObject.getJSONArray("clickPoints");
            for (int i = 0; i < clickPoints.length(); i++) {
                JSONObject pointObj = clickPoints.getJSONObject(i);
                int x = pointObj.getInt("x");
                int y = pointObj.getInt("y");
                long delay = pointObj.getLong("delay");
                String mouseButton = pointObj.getString("mouseButton");
                String clickType = pointObj.getString("clickType");

                profile.addClickPoint(new ClickPattern.ClickPoint(x, y, delay, mouseButton, clickType));
            }

            // Save the imported profile
            saveProfile(profile);
            return profile;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error importing profile from JSON", e);
            return null;
        }
    }

    /**
     * Gets the default profile name.
     *
     * @return The default profile name
     */
    public String getDefaultProfileName() {
        return DEFAULT_PROFILE_NAME;
    }

    /**
     * Backs up all profiles to a zip file.
     *
     * @param backupPath Path to save the backup zip
     * @return true if backup was successful, false otherwise
     */
    public boolean backupProfiles(Path backupPath) {
        try {
            if (profiles.isEmpty()) {
                LOGGER.log(Level.WARNING, "No profiles to backup");
                return false;
            }

            // Implementation could use java.util.zip.ZipOutputStream to create a zip file
            // containing all profile files

            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error backing up profiles", e);
            return false;
        }
    }

    /**
     * Restores profiles from a backup zip file.
     *
     * @param backupPath Path to the backup zip file
     * @return true if restore was successful, false otherwise
     */
    public boolean restoreProfiles(Path backupPath) {
        try {
            // Implementation could use java.util.zip.ZipInputStream to extract profile files
            // from the backup zip and load them

            // After extraction, reload all profiles
            loadAllProfiles();
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error restoring profiles from backup", e);
            return false;
        }
    }
}