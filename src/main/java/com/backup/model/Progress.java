package com.backup.model;

import lombok.Data;

@Data
public class Progress {
    public long totalFiles;
    public long totalBytes;
    public long filesProcessed;
    public long bytesProcessed;

    public double getFileProgress() {
        return totalFiles > 0 ? (double) filesProcessed / totalFiles : 0.0;
    }

    public double getByteProgress() {
        return totalBytes > 0 ? (double) bytesProcessed / totalBytes : 0.0;
    }
}
