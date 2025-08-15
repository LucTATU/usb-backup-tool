package com.backup.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class History {
    private LocalDateTime timestamp;
    private String sourcePath;
    private String destinationPath;
    private long filesCopied;
    private long totalSize;
    private boolean successful;
    private String errorMessage = null;

    public History(LocalDateTime timestamp, String sourcePath, String destinationPath,
                   long filesCopied, long totalSize, boolean successful) {
        this.timestamp = timestamp;
        this.sourcePath = sourcePath;
        this.destinationPath = destinationPath;
        this.filesCopied = filesCopied;
        this.totalSize = totalSize;
        this.successful = successful;
    }
}
package com.backup.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class History {
    private LocalDateTime timestamp;
    private String sourcePath;
    private String destinationPath;
    private long filesCopied;
    private long totalSize;
    private boolean successful;
    private String errorMessage;

    public History(LocalDateTime timestamp, String sourcePath, String destinationPath, 
                   long filesCopied, long totalSize, boolean successful) {
        this.timestamp = timestamp;
        this.sourcePath = sourcePath;
        this.destinationPath = destinationPath;
        this.filesCopied = filesCopied;
        this.totalSize = totalSize;
        this.successful = successful;
    }
}
