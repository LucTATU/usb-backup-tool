package com.backup.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Analysis {
    private long filesToBackup = 0;
    private long totalSizeToBackup = 0;
}
