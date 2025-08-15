
package com.backup.model;

public class SmbShare {
    private String host;
    private String shareName;
    private boolean connected;
    private String displayName;

    public SmbShare(String host, String shareName, boolean connected) {
        this.host = host;
        this.shareName = shareName;
        this.connected = connected;
        this.displayName = String.format("\\\\%s\\%s", host, shareName);
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

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName + (connected ? " (Connected)" : "");
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
