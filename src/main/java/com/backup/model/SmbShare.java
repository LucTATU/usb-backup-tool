package com.backup.model;

public class SmbShare {
    private String host;
    private String shareName;
    private boolean connected;
    private long totalSpace;
    private long availableSpace;

    public SmbShare(String host, String shareName, boolean connected) {
        this.host = host;
        this.shareName = shareName;
        this.connected = connected;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getShareName() {
        return shareName;
    }

    public void setShareName(String shareName) {
        this.shareName = shareName;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public long getTotalSpace() {
        return totalSpace;
    }

    public void setTotalSpace(long totalSpace) {
        this.totalSpace = totalSpace;
    }

    public long getAvailableSpace() {
        return availableSpace;
    }

    public void setAvailableSpace(long availableSpace) {
        this.availableSpace = availableSpace;
    }

    public String getDisplayName() {
        return host;
    }

    @Override
    public String toString() {
        if (totalSpace > 0) {
            return String.format("%s (%s / %s)",
                host,
                formatBytes(availableSpace),
                formatBytes(totalSpace));
        }
        return host + (connected ? " (Connected)" : "");
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SmbShare smbShare = (SmbShare) obj;
        return host.equals(smbShare.host) && shareName.equals(smbShare.shareName);
    }

    @Override
    public int hashCode() {
        return (host + shareName).hashCode();
    }
}