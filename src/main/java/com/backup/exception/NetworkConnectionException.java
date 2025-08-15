
package com.backup.exception;

public class NetworkConnectionException extends BackupException {
    
    public NetworkConnectionException(String message) {
        super(message);
    }
    
    public NetworkConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
