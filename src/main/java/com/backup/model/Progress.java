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
