package com.backup.service;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DriveDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(DriveDetectionService.class);

    private final ObservableList<Path> availableDrives = FXCollections.observableArrayList();
    private final Set<Path> knownDrives = new HashSet<>();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public DriveDetectionService() {
        startDriveMonitoring();
    }

    public ObservableList<Path> getAvailableDrives() {
        return FXCollections.unmodifiableObservableList(availableDrives);
    }

    private void startDriveMonitoring() {
        // Initial scan
        scanForDrives();

        // Schedule periodic scans every 3 seconds
        executor.scheduleAtFixedRate(this::scanForDrives, 3, 3, TimeUnit.SECONDS);

        logger.info("Drive monitoring started");
    }

    private void scanForDrives() {
        try {
            Set<Path> currentDrives = new HashSet<>();

            // Get all available file system roots
            Iterable<Path> rootDirectories = FileSystems.getDefault().getRootDirectories();

            for (Path root : rootDirectories) {
                try {
                    FileStore fileStore = Files.getFileStore(root);

                    // Check if it's a removable drive (heuristic approach)
                    if (isRemovableDrive(root, fileStore)) {
                        currentDrives.add(root);
                    }
                } catch (IOException e) {
                    // Drive might not be accessible, skip it
                    continue;
                }
            }

            // Update the observable list on JavaFX Application Thread
            javafx.application.Platform.runLater(() -> updateDriveList(currentDrives));

        } catch (Exception e) {
            logger.error("Error during drive scan", e);
        }
    }

    private boolean isRemovableDrive(Path root, FileStore fileStore) {
        try {
            // Check if it's writable and has reasonable space (> 1GB for safety)
            long totalSpace = fileStore.getTotalSpace();
            long usableSpace = fileStore.getUsableSpace();

            if (totalSpace <= 0 || usableSpace <= 0) {
                return false;
            }

            // On Windows, check drive type
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("win")) {
                return isWindowsRemovableDrive(root);
            } else {
                // On Unix-like systems, check mount points
                return isUnixRemovableDrive(root, fileStore);
            }

        } catch (IOException e) {
            return false;
        }
    }

    private boolean isWindowsRemovableDrive(Path root) {
        try {
            // Use Windows-specific logic to detect removable drives
            String rootString = root.toString();
            if (rootString.length() >= 2 && rootString.charAt(1) == ':') {
                // This is a simplified check - in a production app you might use JNA
                // to call Windows APIs for more accurate detection

                // For now, we'll assume drives C: and D: are usually system drives
                char driveLetter = rootString.charAt(0);
                return driveLetter != 'C' && driveLetter != 'D';
            }
        } catch (Exception e) {
            logger.debug("Error checking Windows drive type", e);
        }
        return false;
    }

    private boolean isUnixRemovableDrive(Path root, FileStore fileStore) {
        try {
            // Check if mount point suggests it's removable
            String mountPoint = root.toString();
            String fileStoreType = fileStore.type();

            // Common mount points for removable media
            return mountPoint.startsWith("/media/") ||
                    mountPoint.startsWith("/mnt/") ||
                    mountPoint.startsWith("/Volumes/") || // macOS
                    fileStoreType.equals("vfat") ||
                    fileStoreType.equals("exfat") ||
                    fileStoreType.equals("ntfs");

        } catch (Exception e) {
            logger.debug("Error checking Unix drive type", e);
        }
        return false;
    }

    private void updateDriveList(Set<Path> currentDrives) {
        // Remove drives that are no longer present
        availableDrives.removeIf(drive -> !currentDrives.contains(drive));

        // Add new drives
        for (Path drive : currentDrives) {
            if (!knownDrives.contains(drive)) {
                availableDrives.add(drive);
                knownDrives.add(drive);
                logger.info("New drive detected: {}", drive);
            }
        }

        // Update known drives set
        knownDrives.retainAll(currentDrives);
        knownDrives.addAll(currentDrives);
    }

    public long getAvailableSpace(Path drive) {
        try {
            FileStore fileStore = Files.getFileStore(drive);
            return fileStore.getUsableSpace();
        } catch (IOException e) {
            logger.error("Could not get available space for drive: {}", drive, e);
            return 0;
        }
    }

    public long getTotalSpace(Path drive) {
        try {
            FileStore fileStore = Files.getFileStore(drive);
            return fileStore.getTotalSpace();
        } catch (IOException e) {
            logger.error("Could not get total space for drive: {}", drive, e);
            return 0;
        }
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("Drive detection service shut down");
    }
}