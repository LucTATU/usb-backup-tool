package com.backup.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class Properties {
    private static final Logger logger = LoggerFactory.getLogger(Properties.class);
    private static final String PROPERTIES_FILE = "application.properties";

    private final java.util.Properties properties;

    public Properties() {
        properties = new java.util.Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input == null) {
                throw new FileNotFoundException(PROPERTIES_FILE + " not found in classpath");
            }
            properties.load(input);
        } catch (IOException e) {
            logger.error("Failed to load {}", PROPERTIES_FILE, e);
        }
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }
}
