
package com.backup.ui;

import com.backup.model.*;
import com.backup.service.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static com.backup.Utils.formatBytes;
import static com.backup.Utils.formatDateTime;

public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    private static final DateTimeFormatter DATETIME_FORMATTER = formatDateTime();

    // SMB Source Configuration
    @FXML
    private ComboBox<SmbShare> smbShareComboBox;
    @FXML
    private Button scanSmbButton;
    @FXML
    private Button connectSmbButton;
    @FXML
    private TextField smbUsernameField;
    @FXML
    private PasswordField smbPasswordField;
    @FXML
    private TreeView<FileInfo> smbDirectoryTree;
    @FXML
    private Label smbStatusLabel;

    // USB Destination Configuration
    @FXML
    private ComboBox<Path> usbDriveComboBox;
    @FXML
    private Button refreshUsbButton;
    @FXML
    private TreeView<Path> usbDirectoryTree;
    @FXML
    private Label usbInfoLabel;

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
    private SmbDriveService smbDriveService;
    private UsbDriveService usbDriveService;
    private BackupService backupService;

    private String selectedSmbPath = "";
    private Path selectedUsbPath;

    public void initialize(ConfigurationService configService,
                           SmbDriveService smbDriveService,
                           UsbDriveService usbDriveService,
                           BackupService backupService) {
        this.configService = configService;
        this.smbDriveService = smbDriveService;
        this.usbDriveService = usbDriveService;
        this.backupService = backupService;

        setupUI();
        loadConfiguration();
        setupSmbMonitoring();
        setupUsbMonitoring();
        setupHistoryTable();
    }

    private void setupUI() {
        // Setup SMB share combo box
        smbShareComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(SmbShare share) {
                return share != null ? share.toString() : "";
            }

            @Override
            public SmbShare fromString(String string) {
                return null;
            }
        });

        // Setup USB drive combo box
        usbDriveComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Path path) {
                if (path == null) return "";

                long totalSpace = usbDriveService.getTotalSpace(path);
                long availableSpace = usbDriveService.getAvailableSpace(path);

                return String.format("%s (%s / %s)",
                        path,
                        formatBytes(availableSpace),
                        formatBytes(totalSpace));
            }

            @Override
            public Path fromString(String string) {
                return null;
            }
        });

        // Setup directory trees
        setupSmbDirectoryTree();
        setupUsbDirectoryTree();

        // Setup drive selection listeners
        usbDriveComboBox.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> updateUsbDirectoryTree(newVal));
    }

    private void setupSmbDirectoryTree() {
        smbDirectoryTree.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(FileInfo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });

        smbDirectoryTree.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null && newVal.getValue() != null) {
                        selectedSmbPath = newVal.getValue().getPath();
                    }
                });
    }

    private void setupUsbDirectoryTree() {
        usbDirectoryTree.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(Path item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getFileName() != null ? item.getFileName().toString() : item.toString());
                }
            }
        });

        usbDirectoryTree.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null && newVal.getValue() != null) {
                        selectedUsbPath = newVal.getValue();
                    }
                });
    }

    private void loadConfiguration() {
        Configuration config = configService.getConfiguration();
        historyTable.getItems().setAll(config.getHistories());
    }

    private void setupSmbMonitoring() {
        smbShareComboBox.setItems(smbDriveService.getAvailableShares());

        smbDriveService.getAvailableShares().addListener(
                (ListChangeListener<SmbShare>) change -> {
                    Platform.runLater(() -> {
                        if (change.getList().isEmpty()) {
                            smbStatusLabel.setText("No SMB shares detected");
                        } else {
                            smbStatusLabel.setText(change.getList().size() + " SMB shares found");
                        }
                    });
                });
    }

    private void setupUsbMonitoring() {
        usbDriveComboBox.setItems(usbDriveService.getAvailableUsbDrives());

        usbDriveService.getAvailableUsbDrives().addListener(
                (ListChangeListener<Path>) change -> {
                    Platform.runLater(() -> {
                        if (change.getList().isEmpty()) {
                            usbInfoLabel.setText("No USB drives detected");
                            startBackupButton.setDisable(true);
                        } else {
                            if (usbDriveComboBox.getSelectionModel().isEmpty()) {
                                usbDriveComboBox.getSelectionModel().selectFirst();
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
    private void scanSmbShares() {
        scanSmbButton.setDisable(true);
        scanSmbButton.setText("Scanning...");
        smbStatusLabel.setText("Scanning network for SMB shares...");

        smbDriveService.scanNetworkForShares().thenRun(() -> {
            Platform.runLater(() -> {
                scanSmbButton.setDisable(false);
                scanSmbButton.setText("Scan Network");
                smbStatusLabel.setText("Scan completed");
            });
        });
    }

    @FXML
    private void connectToSmb() {
        SmbShare selectedShare = smbShareComboBox.getSelectionModel().getSelectedItem();
        if (selectedShare == null) {
            showAlert("Please select an SMB share first.");
            return;
        }

        String username = smbUsernameField.getText().trim();
        String password = smbPasswordField.getText();

        if (username.isEmpty()) {
            username = "guest";
        }

        connectSmbButton.setDisable(true);
        smbStatusLabel.setText("Connecting...");

        Thread connectThread = new Thread(() -> {
            boolean success = smbDriveService.connectToShare(selectedShare, username, password);

            Platform.runLater(() -> {
                connectSmbButton.setDisable(false);
                if (success) {
                    smbStatusLabel.setText("Connected successfully");
                    loadSmbDirectoryTree(selectedShare);
                } else {
                    smbStatusLabel.setText("Connection failed");
                }
            });
        });

        connectThread.setDaemon(true);
        connectThread.start();
    }

    private void loadSmbDirectoryTree(SmbShare share) {
        try {
            String rootPath = smbDriveService.buildSmbUrl(share, "");
            FileInfo rootInfo = new FileInfo();
            rootInfo.setName(share.getDisplayName());
            rootInfo.setPath(rootPath);
            rootInfo.setDirectory(true);

            TreeItem<FileInfo> rootItem = new TreeItem<>(rootInfo);
            loadSmbChildren(rootItem, rootPath);
            
            smbDirectoryTree.setRoot(rootItem);
            rootItem.setExpanded(true);
        } catch (Exception e) {
            logger.error("Failed to load SMB directory tree", e);
            showAlert("Failed to load directory tree: " + e.getMessage());
        }
    }

    private void loadSmbChildren(TreeItem<FileInfo> parentItem, String parentPath) {
        try {
            List<FileInfo> children = smbDriveService.listDirectory(parentPath);
            for (FileInfo child : children) {
                if (child.isDirectory()) {
                    TreeItem<FileInfo> childItem = new TreeItem<>(child);
                    parentItem.getChildren().add(childItem);
                    
                    // Add a dummy child to make it expandable
                    childItem.getChildren().add(new TreeItem<>());
                    
                    childItem.expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
                        if (isExpanded && childItem.getChildren().size() == 1 && childItem.getChildren().get(0).getValue() == null) {
                            childItem.getChildren().clear();
                            loadSmbChildren(childItem, child.getPath());
                        }
                    });
                }
            }
        } catch (IOException e) {
            logger.error("Failed to load SMB children for: {}", parentPath, e);
        }
    }

    private void updateUsbDirectoryTree(Path drive) {
        if (drive == null) {
            usbDirectoryTree.setRoot(null);
            return;
        }

        TreeItem<Path> rootItem = new TreeItem<>(drive);
        loadUsbChildren(rootItem);
        
        usbDirectoryTree.setRoot(rootItem);
        rootItem.setExpanded(true);

        updateUsbInfo(drive);
    }

    private void loadUsbChildren(TreeItem<Path> parentItem) {
        try {
            Path parentPath = parentItem.getValue();
            if (Files.isDirectory(parentPath)) {
                Files.list(parentPath)
                        .filter(Files::isDirectory)
                        .forEach(childPath -> {
                            TreeItem<Path> childItem = new TreeItem<>(childPath);
                            parentItem.getChildren().add(childItem);
                            
                            // Add dummy child to make expandable if it has subdirectories
                            try {
                                if (Files.list(childPath).anyMatch(Files::isDirectory)) {
                                    childItem.getChildren().add(new TreeItem<>());
                                    
                                    childItem.expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
                                        if (isExpanded && childItem.getChildren().size() == 1 && childItem.getChildren().get(0).getValue() == null) {
                                            childItem.getChildren().clear();
                                            loadUsbChildren(childItem);
                                        }
                                    });
                                }
                            } catch (IOException e) {
                                // Ignore
                            }
                        });
            }
        } catch (IOException e) {
            logger.error("Failed to load USB children for: {}", parentItem.getValue(), e);
        }
    }

    @FXML
    private void refreshUsbDrives() {
        refreshUsbButton.setText("Refreshing...");
        refreshUsbButton.setDisable(true);

        Platform.runLater(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            refreshUsbButton.setText("Refresh USB");
            refreshUsbButton.setDisable(false);
        });
    }

    @FXML
    private void startBackup() {
        if (selectedSmbPath.isEmpty()) {
            showAlert("Please select a source folder from SMB share.");
            return;
        }

        if (selectedUsbPath == null) {
            showAlert("Please select a destination folder on USB drive.");
            return;
        }

        if (!Files.exists(selectedUsbPath)) {
            showAlert("Selected USB path is no longer available.");
            return;
        }

        // Create a minimal configuration for backup
        Configuration config = new Configuration();
        
        startBackupButton.setDisable(true);
        cancelBackupButton.setDisable(false);
        progressBar.setProgress(0);
        progressLabel.setText("");

        // Start backup with selected paths
        backupService.startBackup(config, selectedUsbPath,
                progress -> {
                    double fileProgress = progress.getFileProgress();
                    progressBar.setProgress(fileProgress);
                    progressLabel.setText(String.format("Files: %d/%d (%s/%s)",
                            progress.filesProcessed, progress.totalFiles,
                            formatBytes(progress.bytesProcessed), formatBytes(progress.totalBytes)));
                },
                status -> statusLabel.setText(status),
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

    private void updateUsbInfo(Path drive) {
        if (drive == null) {
            usbInfoLabel.setText("No USB drive selected");
            return;
        }

        long totalSpace = usbDriveService.getTotalSpace(drive);
        long availableSpace = usbDriveService.getAvailableSpace(drive);
        long usedSpace = totalSpace - availableSpace;

        double usagePercent = totalSpace > 0 ? (double) usedSpace / totalSpace * 100 : 0;

        usbInfoLabel.setText(String.format("Space: %s used, %s available (%.1f%% full)",
                formatBytes(usedSpace), formatBytes(availableSpace), usagePercent));
    }

    private void saveConfiguration() {
        try {
            Configuration config = configService.getConfiguration();
            configService.saveConfiguration(config);
        } catch (Exception e) {
            logger.warn("Could not save configuration", e);
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Backup Tool");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
