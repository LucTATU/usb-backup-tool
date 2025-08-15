package com.backup.service;

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

    public void connect(String host, int port, String username, String password, String shareName) throws IOException {
        try {
            // Setup JCIFS properties
            Properties props = new Properties();
            props.put("jcifs.smb.client.enableSMB2", "true");
            props.put("jcifs.smb.client.disableSMB1", "false"); // Keep SMB1 as fallback
            props.put("jcifs.smb.client.responseTimeout", "30000");
            props.put("jcifs.smb.client.connTimeout", "10000");

            PropertyConfiguration config = new PropertyConfiguration(props);

// 2. Create a CIFSContext from the config
            CIFSContext baseContext = new BaseContext(config);

// 3. Add authentication
            NtlmPasswordAuthenticator auth = new NtlmPasswordAuthenticator(username, password);
            CIFSContext authedContext = baseContext.withCredentials(auth);

            // Test connection
            String testUrl = String.format("smb://%s:%d/%s/", host, port, shareName);
            SmbFile testFile = new SmbFile(testUrl, context);
            testFile.exists(); // This will throw an exception if connection fails

            this.currentHost = host;
            this.currentPort = port;
            this.currentUsername = username;

            logger.info("Successfully connected to SMB share: {}:{}/{}", host, port, shareName);

        } catch (Exception e) {
            logger.error("Failed to connect to SMB share: {}:{}", host, port, e);
            throw new IOException("SMB connection failed", e);
        }
    }

    public List<FileInfo> listFiles(String remotePath) throws IOException {
        if (context == null) {
            throw new IllegalStateException("Not connected to NAS");
        }

        try {
            SmbFile directory = new SmbFile(remotePath, context);
            SmbFile[] files = directory.listFiles();

            List<FileInfo> fileInfos = new ArrayList<>();

            if (files != null) {
                for (SmbFile file : files) {
                    FileInfo info = new FileInfo();
                    info.name = file.getName();
                    info.path = file.getPath();
                    info.isDirectory = file.isDirectory();
                    info.size = file.length();
                    info.lastModified = file.getLastModified();

                    fileInfos.add(info);
                }
            }

            logger.debug("Listed {} files/directories from: {}", fileInfos.size(), remotePath);
            return fileInfos;

        } catch (Exception e) {
            logger.error("Failed to list files from: {}", remotePath, e);
            throw new IOException("Failed to list remote files", e);
        }
    }

    public InputStream openFile(String remotePath) throws IOException {
        if (context == null) {
            throw new IllegalStateException("Not connected to NAS");
        }

        try {
            SmbFile file = new SmbFile(remotePath, context);
            return new SmbFileInputStream(file);
        } catch (Exception e) {
            logger.error("Failed to open remote file: {}", remotePath, e);
            throw new IOException("Failed to open remote file", e);
        }
    }

    public FileInfo getFileInfo(String remotePath) throws IOException {
        if (context == null) {
            throw new IllegalStateException("Not connected to NAS");
        }

        try {
            SmbFile file = new SmbFile(remotePath, context);

            FileInfo info = new FileInfo();
            info.name = file.getName();
            info.path = file.getPath();
            info.isDirectory = file.isDirectory();
            info.size = file.length();
            info.lastModified = file.getLastModified();

            return info;

        } catch (Exception e) {
            logger.error("Failed to get file info for: {}", remotePath, e);
            throw new IOException("Failed to get remote file info", e);
        }
    }

    public boolean testConnection() {
        return context != null;
    }

    public void disconnect() {
        context = null;
        currentHost = null;
        currentPort = 0;
        currentUsername = null;
        logger.info("Disconnected from SMB share");
    }

    public String getCurrentConnection() {
        if (context != null) {
            return String.format("%s@%s:%d", currentUsername, currentHost, currentPort);
        }
        return "Not connected";
    }

}