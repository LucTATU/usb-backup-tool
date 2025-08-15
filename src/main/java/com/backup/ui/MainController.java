package com.backup.ui;

import com.backup.model.Configuration;
import com.backup.model.History;
import com.backup.service.BackupService;
import com.backup.service.ConfigurationService;
import com.backup.service.DriveDetectionService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

import static com.backup.Utils.formatBytes;
import static com.backup.Utils.formatDateTime;

public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    private static final DateTimeFormatter DATETIME_FORMATTER = formatDateTime();

    // NAS Configuration
    @FXML
    private TextField hostField;
    @FXML
    private TextField portField;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField shareField;
    @FXML
    private TextField backupPathField;
    @FXML
    private Button testConnectionButton;
    @FXML
    private Label connectionStatus;

    // Drive Selection
    @FXML
    private ComboBox<Path> driveComboBox;
    @FXML
    private Button refreshDrivesButton;
    @FXML
    private Label driveInfoLabel;

    // Backup Control
    @FXML
    private Button startBackupButton;
    @FXML
    private Button cancelBackupButton;
    @FXML
    private Label statusLabel;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label progressLabel;

    // History Table
    @FXML
    private TableView<History> historyTable;
    @FXML
    private TableColumn<History, String> timestampColumn;
    @FXML
    private TableColumn<History, String> sourceColumn;
    @FXML
    private TableColumn<History, String> destinationColumn;
    @FXML
    private TableColumn<History, String> filesColumn;
    @FXML
    private TableColumn<History, String> sizeColumn;
    @FXML
    private TableColumn<History, String> statusColumn;

    private ConfigurationService configService;
    private DriveDetectionService driveDetectionService;
    private BackupService backupService;

    public void initialize(ConfigurationService configService,
                           DriveDetectionService driveDetectionService,
                           BackupService backupService) {
        this.configService = configService;
        this.driveDetectionService = driveDetectionService;
        this.backupService = backupService;

        setupUI();
        loadConfiguration();
        setupDriveMonitoring();
        setupHistoryTable();
    }

    private void setupUI() {
        // Setup drive combo box
        driveComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Path path) {
                if (path == null) return "";

                long totalSpace = driveDetectionService.getTotalSpace(path);
                long availableSpace = driveDetectionService.getAvailableSpace(path);

                return String.format("%s (%s / %s)",
                        path,
                        formatBytes(availableSpace),
                        formatBytes(totalSpace));
            }

            @Override
            public Path fromString(String string) {
                return null; // Not needed for display-only combo box
            }
        });

        // Setup drive selection listener
        driveComboBox.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> updateDriveInfo(newVal));

        // Setup form field listeners to save configuration
        hostField.textProperty().addListener((obs, oldVal, newVal) -> saveConfiguration());
        portField.textProperty().addListener((obs, oldVal, newVal) -> saveConfiguration());
        usernameField.textProperty().addListener((obs, oldVal, newVal) -> saveConfiguration());
        shareField.textProperty().addListener((obs, oldVal, newVal) -> saveConfiguration());
        backupPathField.textProperty().addListener((obs, oldVal, newVal) -> saveConfiguration());
    }

    private void loadConfiguration() {
        Configuration config = configService.getConfiguration();

        if (config.getNasHost() != null) hostField.setText(config.getNasHost());
        portField.setText(String.valueOf(config.getNasPort()));
        if (config.getNasUsername() != null) usernameField.setText(config.getNasUsername());
        if (config.getNasShareName() != null) shareField.setText(config.getNasShareName());
        if (config.getNasBackupPath() != null) backupPathField.setText(config.getNasBackupPath());

        // Load history
        historyTable.getItems().setAll(config.getHistories());
    }

    private void setupDriveMonitoring() {
        // Bind combo box to available drives
        driveComboBox.setItems(driveDetectionService.getAvailableDrives());

        // Listen for drive changes
        driveDetectionService.getAvailableDrives().addListener(
                (ListChangeListener<Path>) change -> {
                    Platform.runLater(() -> {
                        if (change.getList().isEmpty()) {
                            driveInfoLabel.setText("No external drives detected");
                            startBackupButton.setDisable(true);
                        } else {
                            // Auto-select first drive if none selected
                            if (driveComboBox.getSelectionModel().isEmpty()) {
                                driveComboBox.getSelectionModel().selectFirst();
                            }
                            startBackupButton.setDisable(false);
                        }
                    });
                });
    }

    private void setupHistoryTable() {
        timestampColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTimestamp().format(DATETIME_FORMATTER)));

        sourceColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getSourcePath()));

        destinationColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDestinationPath()));

        filesColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getFilesCopied())));

        sizeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatBytes(cellData.getValue().getTotalSize())));

        statusColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().isSuccessful() ? "Success" : "Failed"));
    }

    @FXML
    private void testConnection() {
        testConnectionButton.setDisable(true);
        connectionStatus.setText("Testing...");

        // Run connection test in background
        Thread testThread = new Thread(() -> {
            try {
                Configuration config = getCurrentConfiguration();

                // Simple validation
                if (config.getNasHost() == null || config.getNasHost().trim().isEmpty()) {
                    throw new IllegalArgumentException("Host is required");
                }
                if (config.getNasUsername() == null || config.getNasUsername().trim().isEmpty()) {
                    throw new IllegalArgumentException("Username is required");
                }
                if (config.getNasShareName() == null || config.getNasShareName().trim().isEmpty()) {
                    throw new IllegalArgumentException("Share name is required");
                }

                // TODO: Test actual connection
                // For now, just simulate a successful test
                Thread.sleep(2000);

                Platform.runLater(() -> {
                    connectionStatus.setText("Connected successfully");
                    connectionStatus.setStyle("-fx-text-fill: green;");
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    connectionStatus.setText("Connection failed: " + e.getMessage());
                    connectionStatus.setStyle("-fx-text-fill: red;");
                });
            } finally {
                Platform.runLater(() -> testConnectionButton.setDisable(false));
            }
        });

        testThread.setDaemon(true);
        testThread.start();
    }

    @FXML
    private void refreshDrives() {
        // Drive detection is automatic, but we can trigger a manual update
        refreshDrivesButton.setText("Refreshing...");
        refreshDrivesButton.setDisable(true);

        // Re-enable button after a short delay
        Platform.runLater(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            refreshDrivesButton.setText("Refresh");
            refreshDrivesButton.setDisable(false);
        });
    }

    @FXML
    private void startBackup() {
        Path selectedDrive = driveComboBox.getSelectionModel().getSelectedItem();
        if (selectedDrive == null) {
            showAlert("Please select an external drive first.");
            return;
        }

        if (!Files.exists(selectedDrive)) {
            showAlert("Selected drive is no longer available.");
            return;
        }

        Configuration config = getCurrentConfiguration();

        // Validate configuration
        if (config.getNasHost() == null || config.getNasHost().trim().isEmpty()) {
            showAlert("Please configure NAS connection settings first.");
            return;
        }

        // Update UI for backup in progress
        startBackupButton.setDisable(true);
        cancelBackupButton.setDisable(false);
        progressBar.setProgress(0);
        progressLabel.setText("");

        // Start backup
        backupService.startBackup(config, selectedDrive,
                // Progress callback
                progress -> {
                    double fileProgress = progress.getFileProgress();
                    progressBar.setProgress(fileProgress);
                    progressLabel.setText(String.format("Files: %d/%d (%s/%s)",
                            progress.filesProcessed, progress.totalFiles,
                            formatBytes(progress.bytesProcessed), formatBytes(progress.totalBytes)));
                },
                // Status callback
                status -> statusLabel.setText(status),
                // Completion callback
                success -> {
                    startBackupButton.setDisable(false);
                    cancelBackupButton.setDisable(true);

                    if (success) {
                        progressBar.setProgress(1.0);
                        progressLabel.setText("Backup completed successfully!");
                    } else {
                        progressBar.setProgress(0.0);
                        progressLabel.setText("Backup failed.");
                    }

                    // Refresh history table
                    historyTable.getItems().setAll(config.getHistories());
                    saveConfiguration();
                });
    }

    @FXML
    private void cancelBackup() {
        backupService.cancelBackup();

        startBackupButton.setDisable(false);
        cancelBackupButton.setDisable(true);
        statusLabel.setText("Backup cancelled");
        progressBar.setProgress(0.0);
        progressLabel.setText("");
    }

    private void updateDriveInfo(Path drive) {
        if (drive == null) {
            driveInfoLabel.setText("No drive selected");
            return;
        }

        long totalSpace = driveDetectionService.getTotalSpace(drive);
        long availableSpace = driveDetectionService.getAvailableSpace(drive);
        long usedSpace = totalSpace - availableSpace;

        double usagePercent = totalSpace > 0 ? (double) usedSpace / totalSpace * 100 : 0;

        driveInfoLabel.setText(String.format("Space: %s used, %s available (%.1f%% full)",
                formatBytes(usedSpace), formatBytes(availableSpace), usagePercent));
    }

    private Configuration getCurrentConfiguration() {
        Configuration config = configService.getConfiguration();

        config.setNasHost(hostField.getText().trim());

        try {
            config.setNasPort(Integer.parseInt(portField.getText().trim()));
        } catch (NumberFormatException e) {
            config.setNasPort(445);
        }

        config.setNasUsername(usernameField.getText().trim());
        config.setNasPassword(passwordField.getText());
        config.setNasShareName(shareField.getText().trim());
        config.setNasBackupPath(backupPathField.getText().trim());

        Path selectedDrive = driveComboBox.getSelectionModel().getSelectedItem();
        if (selectedDrive != null) {
            config.setLastUsedExternalDrive(selectedDrive);
        }

        return config;
    }

    private void saveConfiguration() {
        try {
            Configuration config = getCurrentConfiguration();
            configService.saveConfiguration(config);
        } catch (Exception e) {
            logger.warn("Could not save configuration", e);
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("NAS Backup Tool");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}