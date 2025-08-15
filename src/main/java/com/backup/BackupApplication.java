package com.backup;

import com.backup.config.Properties;
import com.backup.service.BackupService;
import com.backup.service.ConfigurationService;
import com.backup.service.DriveDetectionService;
import com.backup.ui.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackupApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(BackupApplication.class);

    private static final Properties config = new Properties();
    private static final String APPLICATION_NAME = config.getProperty("application.name");
    private static final int WIDTH = Integer.parseInt(config.getProperty("application.width"));
    private static final int HEIGHT = Integer.parseInt(config.getProperty("application.height"));
    private static final String RESOURCE_FILE = "/fxml/main.fxml";

    private ConfigurationService configService;
    private DriveDetectionService driveDetectionService;
    private BackupService backupService;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Starting {}...", APPLICATION_NAME);

            // Initialize services
            initializeServices();

            // Load FXML and create the main window
            FXMLLoader loader = new FXMLLoader(getClass().getResource(RESOURCE_FILE));
            Scene scene = new Scene(loader.load(), WIDTH, HEIGHT);

            // Get controller and inject dependencies
            MainController controller = loader.getController();
            controller.initialize(configService, driveDetectionService, backupService);

            // Setup window
            primaryStage.setTitle(APPLICATION_NAME);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(WIDTH);
            primaryStage.setMinHeight(HEIGHT);
            primaryStage.show();

            logger.info("Application started successfully");

        } catch (Exception e) {
            logger.error("Failed to start application", e);
            throw new RuntimeException("Application startup failed", e);
        }
    }

    @Override
    public void stop() {
        logger.info("Shutting down {}...", APPLICATION_NAME);
        if (driveDetectionService != null) {
            driveDetectionService.shutdown();
        }
        if (backupService != null) {
            backupService.shutdown();
        }
    }

    private void initializeServices() {
        configService = new ConfigurationService();
        driveDetectionService = new DriveDetectionService();
        backupService = new BackupService();
    }
}