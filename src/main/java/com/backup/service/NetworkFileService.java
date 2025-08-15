
package com.backup.service;

import com.backup.constants.AppConstants;
import com.backup.exception.NetworkConnectionException;
import com.backup.model.FileInfo;
import jcifs.CIFSContext;
import jcifs.config.PropertyConfiguration;
import jcifs.context.BaseContext;
import jcifs.smb.NtlmPasswordAuthenticator;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class NetworkFileService {

    private static final Logger logger = LoggerFactory.getLogger(NetworkFileService.class);

    private CIFSContext context;
    private String currentHost;
    private int currentPort;
    private String currentUsername;
    private String currentShareName;

    public void connect(String host, int port, String username, String password, String shareName) 
            throws NetworkConnectionException {
        try {
            Properties props = createSmbProperties();
            PropertyConfiguration config = new PropertyConfiguration(props);
            CIFSContext baseContext = new BaseContext(config);
            
            NtlmPasswordAuthenticator auth = new NtlmPasswordAuthenticator(username, password);
            this.context = baseContext.withCredentials(auth);

            testConnection(host, port, shareName);
            
            this.currentHost = host;
            this.currentPort = port;
            this.currentUsername = username;
            this.currentShareName = shareName;

            logger.info("Successfully connected to SMB share: {}:{}/{}", host, port, shareName);

        } catch (Exception e) {
            logger.error("Failed to connect to SMB share: {}:{}", host, port, e);
            throw new NetworkConnectionException("SMB connection failed: " + e.getMessage(), e);
        }
    }
    
    private Properties createSmbProperties() {
        Properties props = new Properties();
        props.put("jcifs.smb.client.enableSMB2", "true");
        props.put("jcifs.smb.client.disableSMB1", "false");
        props.put("jcifs.smb.client.responseTimeout", String.valueOf(AppConstants.DEFAULT_RESPONSE_TIMEOUT));
        props.put("jcifs.smb.client.connTimeout", String.valueOf(AppConstants.DEFAULT_CONNECTION_TIMEOUT));
        return props;
    }
    
    private void testConnection(String host, int port, String shareName) throws IOException {
        String testUrl = String.format(AppConstants.SMB_URL_FORMAT, host, port, shareName);
        SmbFile testFile = new SmbFile(testUrl, context);
        testFile.exists();
    }

    public List<FileInfo> listFiles(String remotePath) throws IOException {
        validateConnection();

        try {
            SmbFile directory = new SmbFile(remotePath, context);
            SmbFile[] files = directory.listFiles();

            List<FileInfo> fileInfos = new ArrayList<>();

            if (files != null) {
                for (SmbFile file : files) {
                    FileInfo info = createFileInfo(file);
                    fileInfos.add(info);
                }
            }

            logger.debug("Listed {} files/directories from: {}", fileInfos.size(), remotePath);
            return fileInfos;

        } catch (Exception e) {
            logger.error("Failed to list files from: {}", remotePath, e);
            throw new IOException("Failed to list remote files: " + e.getMessage(), e);
        }
    }
    
    private FileInfo createFileInfo(SmbFile file) throws IOException {
        FileInfo info = new FileInfo();
        info.setName(file.getName());
        info.setPath(file.getPath());
        info.setDirectory(file.isDirectory());
        info.setSize(file.length());
        info.setLastModified(file.getLastModified());
        return info;
    }

    public InputStream openFile(String remotePath) throws IOException {
        validateConnection();

        try {
            SmbFile file = new SmbFile(remotePath, context);
            return new SmbFileInputStream(file);
        } catch (Exception e) {
            logger.error("Failed to open remote file: {}", remotePath, e);
            throw new IOException("Failed to open remote file: " + e.getMessage(), e);
        }
    }

    public FileInfo getFileInfo(String remotePath) throws IOException {
        validateConnection();

        try {
            SmbFile file = new SmbFile(remotePath, context);
            return createFileInfo(file);
        } catch (Exception e) {
            logger.error("Failed to get file info for: {}", remotePath, e);
            throw new IOException("Failed to get remote file info: " + e.getMessage(), e);
        }
    }
    
    private void validateConnection() throws IllegalStateException {
        if (context == null) {
            throw new IllegalStateException("Not connected to NAS");
        }
    }

    public boolean isConnected() {
        return context != null;
    }

    public void disconnect() {
        context = null;
        currentHost = null;
        currentPort = 0;
        currentUsername = null;
        currentShareName = null;
        logger.info("Disconnected from SMB share");
    }

    public String getConnectionInfo() {
        if (context != null) {
            return String.format("%s@%s:%d/%s", currentUsername, currentHost, currentPort, currentShareName);
        }
        return "Not connected";
    }
}
