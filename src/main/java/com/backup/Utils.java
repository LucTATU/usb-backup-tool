
package com.backup;

import com.backup.constants.AppConstants;

import java.time.format.DateTimeFormatter;

public final class Utils {
    
    private Utils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    public static DateTimeFormatter formatDateTime() {
        return DateTimeFormatter.ofPattern(AppConstants.DATETIME_FORMAT);
    }
}
