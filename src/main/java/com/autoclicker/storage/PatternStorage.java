package com.autoclicker.storage;

import com.autoclicker.config.AppConfig;
import com.autoclicker.core.model.ClickPattern;

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
 * Storage class for managing click patterns.
 * Handles persistence of pattern records to disk.
 */
public class PatternStorage {
    private static final Logger LOGGER = Logger.getLogger(PatternStorage.class.getName());

    private final Path patternsDirectory;
    private final Map<String, ClickPattern> patterns = new HashMap<>();

    /**
     * Creates a new PatternStorage instance.
     */
    public PatternStorage() {
        String userHome = System.getProperty("user.home");
        this.patternsDirectory = Paths.get(userHome, AppConfig.SETTINGS_DIR_NAME, AppConfig.PATTERNS_FOLDER_NAME);
        initialize();
    }

    /**
     * Initializes the storage system.
     */
    private void initialize() {
        try {
            if (!Files.exists(patternsDirectory)) {
                Files.createDirectories(patternsDirectory);
                LOGGER.log(Level.INFO, "Created patterns directory: {0}", patternsDirectory);
            }
            loadPatterns();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize pattern storage", e);
        }
    }

    /**
     * Loads all saved patterns from disk.
     */
    public void loadPatterns() {
        patterns.clear();

        try {
            if (!Files.isDirectory(patternsDirectory)) {
                LOGGER.log(Level.WARNING, "Patterns directory does not exist: {0}", patternsDirectory);
                return;
            }

            File[] patternFiles = patternsDirectory.toFile().listFiles(
                    (dir, name) -> name.endsWith(".pattern"));

            if (patternFiles == null) {
                LOGGER.log(Level.WARNING, "Could not list pattern files in: {0}", patternsDirectory);
                return;
            }

            for (File file : patternFiles) {
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                    ClickPattern pattern = (ClickPattern) ois.readObject();
                    if (pattern != null && pattern.getName() != null) {
                        patterns.put(pattern.getName(), pattern);
                        LOGGER.log(Level.FINE, "Loaded pattern: {0}", pattern.getName());
                    }
                } catch (ClassNotFoundException | IOException e) {
                    LOGGER.log(Level.WARNING, "Error loading pattern file: " + file.getName(), e);
                }
            }

            LOGGER.log(Level.INFO, "Loaded {0} patterns", patterns.size());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load patterns", e);
        }
    }

    /**
     * Saves a pattern to disk.
     *
     * @param pattern The pattern to save
     * @return true if successful, false otherwise
     */
    public boolean savePattern(ClickPattern pattern) {
        if (pattern == null || pattern.getName() == null || pattern.getName().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Cannot save null or unnamed pattern");
            return false;
        }

        try {
            // Ensure directory exists
            if (!Files.exists(patternsDirectory)) {
                Files.createDirectories(patternsDirectory);
            }

            // Add to memory cache
            patterns.put(pattern.getName(), pattern);

            // Save to file
            File patternFile = new File(patternsDirectory.toFile(),
                    sanitizeFileName(pattern.getName()) + ".pattern");

            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(patternFile))) {
                oos.writeObject(pattern);
                LOGGER.log(Level.INFO, "Saved pattern: {0}", pattern.getName());
                return true;
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save pattern: " + pattern.getName(), e);
            return false;
        }
    }

    /**
     * Deletes a pattern.
     *
     * @param name The name of the pattern to delete
     * @return true if successful, false otherwise
     */
    public boolean deletePattern(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        // Remove from memory
        patterns.remove(name);

        // Remove file
        try {
            File patternFile = new File(patternsDirectory.toFile(),
                    sanitizeFileName(name) + ".pattern");
            return patternFile.delete();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to delete pattern file: " + name, e);
            return false;
        }
    }

    /**
     * Gets a pattern by name.
     *
     * @param name The pattern name
     * @return The pattern, or null if not found
     */
    public ClickPattern getPattern(String name) {
        return patterns.get(name);
    }

    /**
     * Gets all pattern names.
     *
     * @return List of pattern names
     */
    public List<String> getPatternNames() {
        return new ArrayList<>(patterns.keySet());
    }

    /**
     * Gets all patterns.
     *
     * @return Map of pattern names to patterns
     */
    public Map<String, ClickPattern> getAllPatterns() {
        return new HashMap<>(patterns);
    }

    /**
     * Sanitizes a file name for use in the file system.
     *
     * @param name The name to sanitize
     * @return Sanitized name
     */
    private String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
}