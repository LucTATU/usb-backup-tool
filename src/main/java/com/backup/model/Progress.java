package com.backup.model;

public class Progress {
    public long totalFiles = 0;
    public long filesProcessed = 0;
    public long totalBytes = 0;
    public long bytesProcessed = 0;

    public double getFileProgress() {
        return totalFiles == 0 ? 0 : (double) filesProcessed / totalFiles;
    }

    public double getByteProgress() {
        return totalBytes == 0 ? 0 : (double) bytesProcessed / totalBytes;
    }
}
package com.backup.model;

import lombok.Data;

@Data
public class Progress {
    private long totalFiles;
    private long totalBytes;
    private long filesProcessed;
    private long bytesProcessed;

    public double getFileProgress() {
        return totalFiles > 0 ? (double) filesProcessed / totalFiles : 0.0;
    }

    public double getByteProgress() {
        return totalBytes > 0 ? (double) bytesProcessed / totalBytes : 0.0;
    }
}
