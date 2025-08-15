
package com.backup.constants;

public final class AppConstants {
    
    // Network constants
    public static final int DEFAULT_SMB_PORT = 445;
    public static final int DEFAULT_CONNECTION_TIMEOUT = 10000;
    public static final int DEFAULT_RESPONSE_TIMEOUT = 30000;
    
    // UI constants
    public static final String DEFAULT_CONFIG_FILE = "backup-config.json";
    public static final String CONFIG_DIR_NAME = ".nas-backup";
    
    // Drive monitoring
    public static final int DRIVE_SCAN_INTERVAL_SECONDS = 3;
    public static final long MIN_DRIVE_SIZE_BYTES = 1024 * 1024 * 1024; // 1GB
    
    // SMB Protocol
    public static final String SMB_URL_FORMAT = "smb://%s:%d/%s/";
    
    // Date format
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    
    private AppConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}
