
package com.backup.service;

import com.backup.model.FileInfo;
import com.backup.model.SmbShare;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SmbDriveService {

    private static final Logger logger = LoggerFactory.getLogger(SmbDriveService.class);

    private final ExecutorService executor;
    private final ScheduledExecutorService scheduledExecutor;
    private final ObservableList<SmbShare> availableShares = FXCollections.observableArrayList();
    private final Set<Path> knownNetworkDrives = new HashSet<>();

    public SmbDriveService() {
        this.executor = Executors.newCachedThreadPool();
        this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        startNetworkDriveMonitoring();
    }

    public ObservableList<SmbShare> getAvailableShares() {
        return FXCollections.unmodifiableObservableList(availableShares);
    }

    private void startNetworkDriveMonitoring() {
        scanForNetworkDrives();
        scheduledExecutor.scheduleAtFixedRate(this::scanForNetworkDrives, 3, 3, TimeUnit.SECONDS);
        logger.info("Network drive monitoring started");
    }

    private void scanForNetworkDrives() {
        try {
            Set<Path> currentNetworkDrives = new HashSet<>();

            for (Path root : FileSystems.getDefault().getRootDirectories()) {
                try {
                    FileStore fileStore = Files.getFileStore(root);
                    
                    if (isNetworkDrive(root, fileStore)) {
                        currentNetworkDrives.add(root);
                    }
                } catch (IOException e) {
                    continue;
                }
            }

            javafx.application.Platform.runLater(() -> updateNetworkDriveList(currentNetworkDrives));

        } catch (Exception e) {
            logger.error("Error during network drive scan", e);
        }
    }

    private boolean isNetworkDrive(Path root, FileStore fileStore) {
        try {
            String osName = System.getProperty("os.name").toLowerCase();
            
            if (osName.contains("win")) {
                return isWindowsNetworkDrive(root, fileStore);
            } else {
                return isUnixNetworkDrive(root, fileStore);
            }
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isWindowsNetworkDrive(Path root, FileStore fileStore) {
        try {
            String rootString = root.toString();
            if (rootString.length() >= 2 && rootString.charAt(1) == ':') {
                char driveLetter = rootString.charAt(0);
                
                // Network drives are typically mapped to letters like Y:, Z:, etc.
                // They usually have specific file system types
                String type = fileStore.type().toLowerCase();
                String name = fileStore.name().toLowerCase();
                
                // Check if it's a network file system
                boolean isNetworkType = type.contains("cifs") || type.contains("smb") || 
                                      type.contains("nfs") || name.contains("\\\\");
                
                // Network drives are typically in higher letters (Y, Z)
                boolean isNetworkLetter = driveLetter >= 'Y' || driveLetter >= 'y';
                
                return isNetworkType || isNetworkLetter;
            }
        } catch (Exception e) {
            logger.debug("Error checking Windows network drive", e);
        }
        return false;
    }

    private boolean isUnixNetworkDrive(Path root, FileStore fileStore) {
        try {
            String mountPoint = root.toString();
            String fileStoreType = fileStore.type().toLowerCase();

            // Check for network file systems
            boolean isNetworkFileSystem = fileStoreType.equals("cifs") ||
                    fileStoreType.equals("smb") ||
                    fileStoreType.equals("nfs") ||
                    fileStoreType.equals("smbfs");

            // Check mount point patterns typical for network drives
            boolean isNetworkMountPoint = mountPoint.startsWith("/net/") ||
                    mountPoint.startsWith("/mnt/network/") ||
                    mountPoint.contains("/smb/") ||
                    mountPoint.contains("/cifs/");

            return isNetworkFileSystem || isNetworkMountPoint;
        } catch (Exception e) {
            logger.debug("Error checking Unix network drive", e);
        }
        return false;
    }

    private void updateNetworkDriveList(Set<Path> currentNetworkDrives) {
        // Remove drives that are no longer available
        availableShares.removeIf(share -> !currentNetworkDrives.contains(Path.of(share.getDisplayName())));

        // Add new network drives
        for (Path drive : currentNetworkDrives) {
            if (!knownNetworkDrives.contains(drive)) {
                String driveName = drive.toString();
                long totalSpace = getTotalSpace(drive);
                long availableSpace = getAvailableSpace(drive);
                
                SmbShare share = new SmbShare(driveName, driveName, true);
                share.setTotalSpace(totalSpace);
                share.setAvailableSpace(availableSpace);
                
                availableShares.add(share);
                knownNetworkDrives.add(drive);
                logger.info("New network drive detected: {}", drive);
            }
        }

        knownNetworkDrives.retainAll(currentNetworkDrives);
    }

    public CompletableFuture<Void> scanNetworkForShares() {
        return CompletableFuture.runAsync(() -> {
            scanForNetworkDrives();
        }, executor);
    }

    public boolean connectToShare(SmbShare share, String username, String password) {
        // For already mounted network drives, no connection needed
        share.setConnected(true);
        return true;
    }

    public List<FileInfo> listDirectory(String path) throws IOException {
        List<FileInfo> files = new ArrayList<>();
        try {
            Path dirPath = Path.of(path);
            if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
                Files.list(dirPath).forEach(file -> {
                    try {
                        FileInfo fileInfo = new FileInfo();
                        fileInfo.setName(file.getFileName().toString());
                        fileInfo.setPath(file.toString());
                        fileInfo.setDirectory(Files.isDirectory(file));
                        if (!Files.isDirectory(file)) {
                            fileInfo.setSize(Files.size(file));
                            fileInfo.setLastModified(Files.getLastModifiedTime(file).toMillis());
                        }
                        files.add(fileInfo);
                    } catch (IOException e) {
                        logger.warn("Could not read file info for: {}", file, e);
                    }
                });
            }
        } catch (Exception e) {
            logger.error("Failed to list directory: {}", path, e);
            throw new IOException("Failed to list directory: " + path, e);
        }
        return files;
    }

    public String buildSmbUrl(SmbShare share, String path) {
        return Path.of(share.getHost(), path).toString();
    }

    public long getAvailableSpace(Path drive) {
        try {
            FileStore fileStore = Files.getFileStore(drive);
            return fileStore.getUsableSpace();
        } catch (IOException e) {
            logger.error("Could not get available space for network drive: {}", drive, e);
            return 0;
        }
    }

    public long getTotalSpace(Path drive) {
        try {
            FileStore fileStore = Files.getFileStore(drive);
            return fileStore.getTotalSpace();
        } catch (IOException e) {
            logger.error("Could not get total space for network drive: {}", drive, e);
            return 0;
        }
    }

    public void disconnect() {
        // No disconnection needed for mounted drives
    }

    public void shutdown() {
        executor.shutdown();
        scheduledExecutor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("SMB drive service shut down");
    }
}
