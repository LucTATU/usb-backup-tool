
package com.backup;

import com.backup.service.*;
import com.backup.ui.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackupApplication extends Application {

    private static final Logger logger = LoggerFactory.getLogger(BackupApplication.class);

    private ConfigurationService configService;
    private SmbDriveService smbDriveService;
    private UsbDriveService usbDriveService;
    private BackupService backupService;

    @Override
    public void start(Stage primaryStage) {
        try {
            // Initialize services
            configService = new ConfigurationService();
            smbDriveService = new SmbDriveService();
            usbDriveService = new UsbDriveService();
            backupService = new BackupService();

            // Load FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Scene scene = new Scene(loader.load());

            // Initialize controller with services
            MainController controller = loader.getController();
            controller.initialize(configService, smbDriveService, usbDriveService, backupService);

            // Setup stage
            primaryStage.setTitle("Backup Tool - SMB to USB");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);

            // Handle application close
            primaryStage.setOnCloseRequest(e -> shutdown());

            primaryStage.show();

            logger.info("Backup application started successfully");

        } catch (Exception e) {
            logger.error("Failed to start application", e);
            System.exit(1);
        }
    }

    private void shutdown() {
        try {
            if (smbDriveService != null) {
                smbDriveService.shutdown();
            }
            if (usbDriveService != null) {
                usbDriveService.shutdown();
            }
            if (backupService != null) {
                backupService.shutdown();
            }
            logger.info("Application shutdown completed");
        } catch (Exception e) {
            logger.error("Error during shutdown", e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
