
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

import static com.backup.util.DriveUtils.isRemovableDrive;

public class UsbDriveService {

    private static final Logger logger = LoggerFactory.getLogger(UsbDriveService.class);

    private final ObservableList<Path> availableUsbDrives = FXCollections.observableArrayList();
    private final Set<Path> knownDrives = new HashSet<>();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public UsbDriveService() {
        startDriveMonitoring();
    }

    public ObservableList<Path> getAvailableUsbDrives() {
        return FXCollections.unmodifiableObservableList(availableUsbDrives);
    }

    private void startDriveMonitoring() {
        scanForUsbDrives();
        executor.scheduleAtFixedRate(this::scanForUsbDrives, 3, 3, TimeUnit.SECONDS);
        logger.info("USB drive monitoring started");
    }

    private void scanForUsbDrives() {
        try {
            Set<Path> currentDrives = new HashSet<>();

            for (Path root : FileSystems.getDefault().getRootDirectories()) {
                try {
                    FileStore fileStore = Files.getFileStore(root);
                    
                    // Check if it's a USB/removable drive
                    if (isUsbDrive(root, fileStore)) {
                        currentDrives.add(root);
                    }
                } catch (IOException e) {
                    continue;
                }
            }

            // Check for mounted drives on Unix-like systems
            if (!System.getProperty("os.name").toLowerCase().contains("win")) {
                currentDrives.addAll(scanUnixMountPoints());
            }

            javafx.application.Platform.runLater(() -> updateDriveList(currentDrives));

        } catch (Exception e) {
            logger.error("Error during USB drive scan", e);
        }
    }

    private boolean isUsbDrive(Path root, FileStore fileStore) {
        try {
            String osName = System.getProperty("os.name").toLowerCase();
            
            if (osName.contains("win")) {
                return isWindowsUsbDrive(root, fileStore);
            } else {
                return isUnixUsbDrive(root, fileStore);
            }
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isWindowsUsbDrive(Path root, FileStore fileStore) {
        try {
            String rootString = root.toString();
            if (rootString.length() >= 2 && rootString.charAt(1) == ':') {
                char driveLetter = rootString.charAt(0);
                
                // Exclude system drives (typically C: and D:)
                if (driveLetter == 'C' || driveLetter == 'D') {
                    return false;
                }
                
                // Check file system type (USB drives typically use FAT32, exFAT, NTFS)
                String type = fileStore.type().toLowerCase();
                return type.contains("fat") || type.contains("exfat") || type.contains("ntfs");
            }
        } catch (Exception e) {
            logger.debug("Error checking Windows USB drive", e);
        }
        return false;
    }

    private boolean isUnixUsbDrive(Path root, FileStore fileStore) {
        try {
            String mountPoint = root.toString();
            String fileStoreType = fileStore.type().toLowerCase();

            // Check mount point patterns typical for USB drives
            boolean isMountPointUsb = mountPoint.startsWith("/media/") ||
                    mountPoint.startsWith("/mnt/") ||
                    mountPoint.startsWith("/Volumes/") ||
                    mountPoint.contains("/usb");

            // Check file system types commonly used by USB drives
            boolean isUsbFileSystem = fileStoreType.equals("vfat") ||
                    fileStoreType.equals("exfat") ||
                    fileStoreType.equals("ntfs") ||
                    fileStoreType.equals("fat32");

            return isMountPointUsb || isUsbFileSystem;
        } catch (Exception e) {
            logger.debug("Error checking Unix USB drive", e);
        }
        return false;
    }

    private Set<Path> scanUnixMountPoints() {
        Set<Path> usbDrives = new HashSet<>();
        
        // Common USB mount points
        String[] mountPaths = {"/media", "/mnt", "/Volumes"};
        
        for (String mountPath : mountPaths) {
            try {
                Path mountDir = Path.of(mountPath);
                if (Files.exists(mountDir) && Files.isDirectory(mountDir)) {
                    Files.list(mountDir)
                            .filter(Files::isDirectory)
                            .forEach(dir -> {
                                try {
                                    FileStore fs = Files.getFileStore(dir);
                                    if (isUsbDrive(dir, fs)) {
                                        usbDrives.add(dir);
                                    }
                                } catch (IOException e) {
                                    // Ignore
                                }
                            });
                }
            } catch (Exception e) {
                logger.debug("Error scanning mount path: {}", mountPath, e);
            }
        }
        
        return usbDrives;
    }

    private void updateDriveList(Set<Path> currentDrives) {
        availableUsbDrives.removeIf(drive -> !currentDrives.contains(drive));

        for (Path drive : currentDrives) {
            if (!knownDrives.contains(drive)) {
                availableUsbDrives.add(drive);
                knownDrives.add(drive);
                logger.info("New USB drive detected: {}", drive);
            }
        }

        knownDrives.retainAll(currentDrives);
        knownDrives.addAll(currentDrives);
    }

    public long getAvailableSpace(Path drive) {
        try {
            FileStore fileStore = Files.getFileStore(drive);
            return fileStore.getUsableSpace();
        } catch (IOException e) {
            logger.error("Could not get available space for USB drive: {}", drive, e);
            return 0;
        }
    }

    public long getTotalSpace(Path drive) {
        try {
            FileStore fileStore = Files.getFileStore(drive);
            return fileStore.getTotalSpace();
        } catch (IOException e) {
            logger.error("Could not get total space for USB drive: {}", drive, e);
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
        logger.info("USB drive service shut down");
    }
}
