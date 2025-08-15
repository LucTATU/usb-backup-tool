package com.backup.service;

import com.backup.model.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static com.backup.Utils.formatBytes;

public class BackupService {

    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);

    private final NetworkFileService networkService;
    private final ExecutorService executor;
    private Task<Void> currentBackupTask;

    public BackupService() {
        this.networkService = new NetworkFileService();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void startBackup(Configuration config, Path destinationPath,
                            Consumer<Progress> progressCallback,
                            Consumer<String> statusCallback,
                            Consumer<Boolean> completionCallback) {

        if (currentBackupTask != null && currentBackupTask.isRunning()) {
            statusCallback.accept("Backup already in progress");
            return;
        }

        currentBackupTask = createBackupTask(config, destinationPath,
                progressCallback, statusCallback, completionCallback);

        executor.submit(currentBackupTask);
    }

    private Task<Void> createBackupTask(Configuration config, Path destinationPath,
                                        Consumer<Progress> progressCallback,
                                        Consumer<String> statusCallback,
                                        Consumer<Boolean> completionCallback) {

        return new Task<>() {
            @Override
            protected Void call() {
                try {
                    // Update status
                    Platform.runLater(() -> statusCallback.accept("Connecting to NAS..."));

                    // Connect to NAS
                    String shareUrl = String.format("smb://%s:%d/%s/",
                            config.getNasHost(), config.getNasPort(), config.getNasShareName());

                    networkService.connect(config.getNasHost(), config.getNasPort(),
                            config.getNasUsername(), config.getNasPassword(),
                            config.getNasShareName());

                    // Calculate backup requirements
                    Platform.runLater(() -> statusCallback.accept("Analyzing files..."));

                    String sourceUrl = shareUrl + config.getNasBackupPath();
                    Analysis analysis = analyzeBackup(sourceUrl, destinationPath);

                    // Check available space
                    long availableSpace = Files.getFileStore(destinationPath).getUsableSpace();
                    if (availableSpace < analysis.totalSizeToBackup) {
                        throw new IOException("Insufficient disk space. Need " +
                                formatBytes(analysis.totalSizeToBackup) + ", available " +
                                formatBytes(availableSpace));
                    }

                    // Start actual backup
                    Platform.runLater(() -> statusCallback.accept("Starting backup..."));

                    performBackup(sourceUrl, destinationPath, analysis, progressCallback, statusCallback);

                    // Record successful backup
                    History history = new History(
                            LocalDateTime.now(), sourceUrl, destinationPath.toString(),
                            analysis.filesToBackup, analysis.totalSizeToBackup, true);

                    config.getHistories().add(history);

                    Platform.runLater(() -> {
                        statusCallback.accept("Backup completed successfully!");
                        completionCallback.accept(true);
                    });

                } catch (Exception e) {
                    logger.error("Backup failed", e);

                    // Record failed backup
                    History history = new History(
                            LocalDateTime.now(), config.getNasBackupPath(), destinationPath.toString(),
                            0, 0, false);
                    history.setErrorMessage(e.getMessage());

                    config.getHistories().add(history);

                    Platform.runLater(() -> {
                        statusCallback.accept("Backup failed: " + e.getMessage());
                        completionCallback.accept(false);
                    });
                }

                return null;
            }
        };
    }

    private Analysis analyzeBackup(String sourceUrl, Path destinationPath) throws IOException {
        Analysis analysis = new Analysis();
        analyzeDirectory(sourceUrl, destinationPath, analysis);

        logger.info("Backup analysis: {} files to backup, {} bytes total",
                analysis.filesToBackup, analysis.totalSizeToBackup);

        return analysis;
    }

    private void analyzeDirectory(String sourcePath, Path destinationPath, Analysis analysis) throws IOException {
        List<FileInfo> files = networkService.listFiles(sourcePath);

        for (FileInfo file : files) {
            if (file.isDirectory) {
                // Create directory if it doesn't exist
                Path destDir = destinationPath.resolve(file.name);
                if (!Files.exists(destDir)) {
                    analysis.filesToBackup++;
                }

                // Recursively analyze subdirectory
                analyzeDirectory(file.path, destDir, analysis);
            } else {
                // Check if file needs to be copied
                Path destFile = destinationPath.resolve(file.name);

                if (shouldCopyFile(file, destFile)) {
                    analysis.filesToBackup++;
                    analysis.totalSizeToBackup += file.size;
                }
            }
        }
    }

    private boolean shouldCopyFile(FileInfo sourceFile, Path destinationFile) {
        if (!Files.exists(destinationFile)) {
            return true; // File doesn't exist, needs to be copied
        }

        try {
            // Check file size and modification time
            long destSize = Files.size(destinationFile);
            long destModified = Files.getLastModifiedTime(destinationFile).toMillis();

            return sourceFile.size != destSize || sourceFile.lastModified > destModified;

        } catch (IOException e) {
            logger.warn("Could not check destination file: {}", destinationFile, e);
            return true; // Copy if we can't determine
        }
    }

    private void performBackup(String sourcePath, Path destinationPath, Analysis analysis,
                               Consumer<Progress> progressCallback,
                               Consumer<String> statusCallback) throws IOException {

        Progress progress = new Progress();
        progress.totalFiles = analysis.filesToBackup;
        progress.totalBytes = analysis.totalSizeToBackup;

        copyDirectory(sourcePath, destinationPath, progress, progressCallback, statusCallback);
    }

    private void copyDirectory(String sourcePath, Path destinationPath, Progress progress,
                               Consumer<Progress> progressCallback,
                               Consumer<String> statusCallback) throws IOException {

        // Create destination directory if it doesn't exist
        if (!Files.exists(destinationPath)) {
            Files.createDirectories(destinationPath);
        }

        List<FileInfo> files = networkService.listFiles(sourcePath);

        for (FileInfo file : files) {
            if (file.isDirectory) {
                Path destDir = destinationPath.resolve(file.name);
                copyDirectory(file.path, destDir, progress, progressCallback, statusCallback);
            } else {
                Path destFile = destinationPath.resolve(file.name);

                if (shouldCopyFile(file, destFile)) {
                    Platform.runLater(() -> statusCallback.accept("Copying: " + file.name));

                    copyFile(file.path, destFile);

                    progress.filesProcessed++;
                    progress.bytesProcessed += file.size;

                    Platform.runLater(() -> progressCallback.accept(progress));
                }
            }
        }
    }

    private void copyFile(String sourcePath, Path destinationPath) throws IOException {
        try (InputStream input = networkService.openFile(sourcePath)) {
            Files.copy(input, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void cancelBackup() {
        if (currentBackupTask != null && currentBackupTask.isRunning()) {
            currentBackupTask.cancel();
        }
    }

    public boolean isBackupInProgress() {
        return currentBackupTask != null && currentBackupTask.isRunning();
    }

    public void shutdown() {
        if (currentBackupTask != null) {
            currentBackupTask.cancel();
        }
        executor.shutdown();
        networkService.disconnect();
    }

}