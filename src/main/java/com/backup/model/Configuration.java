package com.backup.model;

import lombok.Data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Data
public class Configuration {
    private String nasHost;
    private int nasPort = 445;
    private String nasUsername;
    private String nasPassword;
    private String nasShareName;
    private String nasBackupPath;
    private Path lastUsedExternalDrive;
    private List<History> histories = new ArrayList<>();
}
