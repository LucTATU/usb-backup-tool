package com.backup.model;

public class FileInfo {
    public String name;
    public String path;
    public boolean isDirectory;
    public long size;
    public long lastModified;

    @Override
    public String toString() {
        return String.format("FileInfo{name='%s', size=%d, isDirectory=%s}", name, size, isDirectory);
    }
}
package com.backup.model;

import lombok.Data;

@Data
public class FileInfo {
    private String name;
    private String path;
    private boolean isDirectory;
    private long size;
    private long lastModified;
}
