package com.backup.service;

import com.backup.model.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationService.class);
    private static final String CONFIG_FILE = "backup-config.json";

    private final ObjectMapper objectMapper;
    private final Path configPath;
    private Configuration currentConfig;

    public ConfigurationService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.configPath = getConfigDirectory().resolve(CONFIG_FILE);
        loadConfiguration();
    }

    private Path getConfigDirectory() {
        String userHome = System.getProperty("user.home");
        Path configDir = Paths.get(userHome, ".nas-backup");

        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
        } catch (IOException e) {
            logger.warn("Could not create config directory, using temp directory", e);
            return Paths.get(System.getProperty("java.io.tmpdir"));
        }

        return configDir;
    }

    public Configuration getConfiguration() {
        return currentConfig;
    }

    public void saveConfiguration(Configuration config) {
        try {
            objectMapper.writeValue(configPath.toFile(), config);
            this.currentConfig = config;
            logger.info("Configuration saved to: {}", configPath);
        } catch (IOException e) {
            logger.error("Failed to save configuration", e);
            throw new RuntimeException("Could not save configuration", e);
        }
    }

    private void loadConfiguration() {
        try {
            if (Files.exists(configPath)) {
                currentConfig = objectMapper.readValue(configPath.toFile(), Configuration.class);
                logger.info("Configuration loaded from: {}", configPath);
            } else {
                currentConfig = new Configuration();
                logger.info("Using default configuration");
            }
        } catch (IOException e) {
            logger.warn("Could not load configuration, using defaults", e);
            currentConfig = new Configuration();
        }
    }
}