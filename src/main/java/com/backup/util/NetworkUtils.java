package com.backup.util;

import com.backup.constants.AppConstants;

public final class NetworkUtils {
    
    private NetworkUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    public static String buildSmbUrl(String host, int port, String shareName) {
        return String.format(AppConstants.SMB_URL_FORMAT, host, port, shareName);
    }
    
    public static String buildSmbUrl(String host, int port, String shareName, String path) {
        String baseUrl = buildSmbUrl(host, port, shareName);
        return baseUrl + (path.startsWith("/") ? path.substring(1) : path);
    }
}
