
package com.backup.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DriveUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(DriveUtils.class);
    
    private DriveUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    public static boolean isRemovableDrive(Path root, FileStore fileStore) {
        try {
            long totalSpace = fileStore.getTotalSpace();
            long usableSpace = fileStore.getUsableSpace();

            if (totalSpace <= 0 || usableSpace <= 0) {
                return false;
            }

            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("win")) {
                return isWindowsRemovableDrive(root);
            } else {
                return isUnixRemovableDrive(root, fileStore);
            }

        } catch (IOException e) {
            logger.debug("Error checking drive type for: {}", root, e);
            return false;
        }
    }
    
    private static boolean isWindowsRemovableDrive(Path root) {
        try {
            String rootString = root.toString();
            if (rootString.length() >= 2 && rootString.charAt(1) == ':') {
                char driveLetter = rootString.charAt(0);
                return driveLetter != 'C' && driveLetter != 'D';
            }
        } catch (Exception e) {
            logger.debug("Error checking Windows drive type", e);
        }
        return false;
    }
    
    private static boolean isUnixRemovableDrive(Path root, FileStore fileStore) {
        try {
            String mountPoint = root.toString();
            String fileStoreType = fileStore.type();

            return mountPoint.startsWith("/media/") ||
                    mountPoint.startsWith("/mnt/") ||
                    mountPoint.startsWith("/Volumes/") ||
                    fileStoreType.equals("vfat") ||
                    fileStoreType.equals("exfat") ||
                    fileStoreType.equals("ntfs");

        } catch (Exception e) {
            logger.debug("Error checking Unix drive type", e);
        }
        return false;
    }
    
    public static long getAvailableSpace(Path drive) {
        try {
            FileStore fileStore = Files.getFileStore(drive);
            return fileStore.getUsableSpace();
        } catch (IOException e) {
            logger.error("Could not get available space for drive: {}", drive, e);
            return 0;
        }
    }

    public static long getTotalSpace(Path drive) {
        try {
            FileStore fileStore = Files.getFileStore(drive);
            return fileStore.getTotalSpace();
        } catch (IOException e) {
            logger.error("Could not get total space for drive: {}", drive, e);
            return 0;
        }
    }
}
