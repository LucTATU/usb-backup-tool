
package com.backup.service;

import com.backup.model.FileInfo;
import com.backup.model.SmbShare;
import com.backup.exception.NetworkConnectionException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SmbDriveService {

    private static final Logger logger = LoggerFactory.getLogger(SmbDriveService.class);

    private final NetworkFileService networkService;
    private final ExecutorService executor;
    private final ObservableList<SmbShare> availableShares = FXCollections.observableArrayList();

    public SmbDriveService() {
        this.networkService = new NetworkFileService();
        this.executor = Executors.newCachedThreadPool();
    }

    public ObservableList<SmbShare> getAvailableShares() {
        return FXCollections.unmodifiableObservableList(availableShares);
    }

    public CompletableFuture<Void> scanNetworkForShares() {
        return CompletableFuture.runAsync(() -> {
            try {
                List<SmbShare> shares = new ArrayList<>();
                List<String> networkHosts = getNetworkHosts();

                for (String host : networkHosts) {
                    try {
                        List<String> hostShares = getSharesForHost(host);
                        for (String share : hostShares) {
                            shares.add(new SmbShare(host, share, false));
                        }
                    } catch (Exception e) {
                        logger.debug("Failed to get shares for host {}: {}", host, e.getMessage());
                    }
                }

                javafx.application.Platform.runLater(() -> {
                    availableShares.clear();
                    availableShares.addAll(shares);
                });

            } catch (Exception e) {
                logger.error("Error scanning network for SMB shares", e);
            }
        }, executor);
    }

    private List<String> getNetworkHosts() {
        List<String> hosts = new ArrayList<>();
        try {
            for (NetworkInterface netInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (netInterface.isLoopback() || !netInterface.isUp()) continue;

                for (InetAddress address : Collections.list(netInterface.getInetAddresses())) {
                    if (address.isSiteLocalAddress()) {
                        String subnet = getSubnet(address.getHostAddress());
                        hosts.addAll(scanSubnet(subnet));
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error getting network hosts", e);
        }
        return hosts;
    }

    private String getSubnet(String ip) {
        String[] parts = ip.split("\\.");
        return parts[0] + "." + parts[1] + "." + parts[2] + ".";
    }

    private List<String> scanSubnet(String subnet) {
        List<String> hosts = new ArrayList<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 1; i < 255; i++) {
            final String host = subnet + i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    InetAddress address = InetAddress.getByName(host);
                    if (address.isReachable(1000)) {
                        synchronized (hosts) {
                            hosts.add(host);
                        }
                    }
                } catch (Exception e) {
                    // Host not reachable
                }
            }, executor);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return hosts;
    }

    private List<String> getSharesForHost(String host) {
        // This would require implementing SMB enumeration
        // For now, return common share names
        List<String> commonShares = List.of("shared", "public", "data", "backup", "files");
        List<String> availableShares = new ArrayList<>();

        for (String share : commonShares) {
            try {
                String testUrl = String.format("smb://%s/%s/", host, share);
                if (testShareExists(host, share)) {
                    availableShares.add(share);
                }
            } catch (Exception e) {
                // Share doesn't exist or not accessible
            }
        }

        return availableShares;
    }

    private boolean testShareExists(String host, String share) {
        try {
            networkService.connect(host, 445, "guest", "", share);
            networkService.disconnect();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean connectToShare(SmbShare share, String username, String password) {
        try {
            networkService.connect(share.getHost(), 445, username, password, share.getShareName());
            share.setConnected(true);
            return true;
        } catch (NetworkConnectionException e) {
            logger.error("Failed to connect to share: {}", share, e);
            return false;
        }
    }

    public List<FileInfo> listDirectory(String path) throws IOException {
        return networkService.listFiles(path);
    }

    public String buildSmbUrl(SmbShare share, String path) {
        return String.format("smb://%s/%s/%s", share.getHost(), share.getShareName(), path);
    }

    public void disconnect() {
        networkService.disconnect();
    }

    public void shutdown() {
        executor.shutdown();
        networkService.disconnect();
    }
}
