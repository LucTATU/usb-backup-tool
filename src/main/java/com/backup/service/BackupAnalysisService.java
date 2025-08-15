package com.backup.service;

import com.backup.model.Analysis;
import com.backup.model.FileInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class BackupAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(BackupAnalysisService.class);
    
    private final NetworkFileService networkFileService;
    
    public BackupAnalysisService(NetworkFileService networkFileService) {
        this.networkFileService = networkFileService;
    }
    
    public Analysis analyzeBackup(String sourceUrl, Path destinationPath) throws IOException {
        Analysis analysis = new Analysis();
        analyzeDirectory(sourceUrl, destinationPath, analysis);

        logger.info("Backup analysis: {} files to backup, {} bytes total",
                analysis.getFilesToBackup(), analysis.getTotalSizeToBackup());

        return analysis;
    }
    
    private void analyzeDirectory(String sourcePath, Path destinationPath, Analysis analysis) throws IOException {
        List<FileInfo> files = networkFileService.listFiles(sourcePath);

        for (FileInfo file : files) {
            if (file.isDirectory()) {
                Path destDir = destinationPath.resolve(file.getName());
                if (!Files.exists(destDir)) {
                    analysis.setFilesToBackup(analysis.getFilesToBackup() + 1);
                }
                analyzeDirectory(file.getPath(), destDir, analysis);
            } else {
                Path destFile = destinationPath.resolve(file.getName());
                if (shouldCopyFile(file, destFile)) {
                    analysis.setFilesToBackup(analysis.getFilesToBackup() + 1);
                    analysis.setTotalSizeToBackup(analysis.getTotalSizeToBackup() + file.getSize());
                }
            }
        }
    }
    
    private boolean shouldCopyFile(FileInfo sourceFile, Path destinationFile) {
        if (!Files.exists(destinationFile)) {
            return true;
        }

        try {
            long destSize = Files.size(destinationFile);
            long destModified = Files.getLastModifiedTime(destinationFile).toMillis();

            return sourceFile.getSize() != destSize || sourceFile.getLastModified() > destModified;

        } catch (IOException e) {
            logger.warn("Could not check destination file: {}", destinationFile, e);
            return true;
        }
    }
}
