package org.ton.java.adnl;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Connection pool for managing multiple liteserver connections Provides load balancing and failover
 * capabilities
 */
public class LiteClientConnectionPool {
  private static final Logger logger = Logger.getLogger(LiteClientConnectionPool.class.getName());

  private final List<AdnlLiteClient> connections = new ArrayList<>();
  private final AtomicInteger currentIndex = new AtomicInteger(0);
  private final ScheduledExecutorService healthChecker;
  private volatile boolean closed = false;

  /** Create connection pool */
  public LiteClientConnectionPool() {
    this.healthChecker = Executors.newSingleThreadScheduledExecutor();
    startHealthChecker();
  }

  /**
   * Add connection from config
   *
   * @param config Global config with liteserver list
   * @throws Exception if no connections could be established
   */
  public void addConnectionsFromConfig(GlobalConfig config) throws Exception {
    if (config.getLiteservers().isEmpty()) {
      throw new Exception("No liteservers in config");
    }

    List<Exception> errors = new ArrayList<>();
    int successCount = 0;

    for (GlobalConfig.LiteServer liteserver : config.getLiteservers()) {
      try {
        String host = intToIp(liteserver.getIp());
        int port = liteserver.getPort();
        String publicKey = liteserver.getId().getKey();

        AdnlLiteClient client = new AdnlLiteClient();
        client.connect(host, port, publicKey);

        synchronized (connections) {
          connections.add(client);
        }

        successCount++;
        logger.info("Connected to liteserver " + host + ":" + port);

        // Try to connect to at least 2 servers for redundancy
        if (successCount >= 2) {
          break;
        }

      } catch (Exception e) {
        errors.add(e);
        logger.log(Level.WARNING, "Failed to connect to liteserver", e);
      }
    }

    if (successCount == 0) {
      throw new Exception("Failed to connect to any liteserver. Errors: " + errors);
    }

    logger.info("Connected to " + successCount + " liteservers");
  }

  /**
   * Add single connection
   *
   * @param host Server host
   * @param port Server port
   * @param serverPublicKeyBase64 Server public key (base64)
   * @throws Exception if connection fails
   */
  public void addConnection(String host, int port, String serverPublicKeyBase64) throws Exception {
    AdnlLiteClient client = new AdnlLiteClient();
    client.connect(host, port, serverPublicKeyBase64);

    synchronized (connections) {
      connections.add(client);
    }

    logger.info("Added connection to " + host + ":" + port);
  }

  /**
   * Execute query with load balancing and failover
   *
   * @param query Query function
   * @return Query result
   * @throws Exception if all connections fail
   */
  public <T> T executeQuery(Function<AdnlLiteClient, T> query) throws Exception {
    List<AdnlLiteClient> availableConnections;
    synchronized (connections) {
      availableConnections = new ArrayList<>(connections);
    }

    if (availableConnections.isEmpty()) {
      throw new Exception("No available connections");
    }

    // Try each connection in round-robin fashion
    List<Exception> errors = new ArrayList<>();
    int attempts = Math.min(availableConnections.size(), 3); // Try up to 3 connections

    for (int i = 0; i < attempts; i++) {
      int index = currentIndex.getAndIncrement() % availableConnections.size();
      AdnlLiteClient client = availableConnections.get(index);

      try {
        if (client.isConnected()) {
          return query.apply(client);
        }
      } catch (Exception e) {
        errors.add(e);
        logger.log(Level.WARNING, "Query failed on connection " + index, e);
      }
    }

    throw new Exception("All connection attempts failed. Errors: " + errors);
  }

  //    /**
  //     * Get masterchain info
  //     * @return MasterchainInfo
  //     * @throws Exception if query fails
  //     */
  //    public AdnlLiteClient.MasterchainInfo getMasterchainInfo() throws Exception {
  //        return executeQuery(client -> {
  //            try {
  //                return client.getMasterchainInfo();
  //            } catch (Exception e) {
  //                throw new RuntimeException(e);
  //            }
  //        });
  //    }
  //
  //    /**
  //     * Run smart contract method
  //     * @param blockId Block ID
  //     * @param accountId Account ID
  //     * @param methodId Method ID
  //     * @param params Method parameters
  //     * @return RunMethodResult
  //     * @throws Exception if query fails
  //     */
  //    public AdnlLiteClient.RunMethodResult runSmcMethod(
  //            AdnlLiteClient.BlockIdExt blockId,
  //            AdnlLiteClient.AccountId accountId,
  //            long methodId,
  //            byte[] params) throws Exception {
  //        return executeQuery(client -> {
  //            try {
  //                return client.runSmcMethod(blockId, accountId, methodId, params);
  //            } catch (Exception e) {
  //                throw new RuntimeException(e);
  //            }
  //        });
  //    }
  //
  //    /**
  //     * Get account state
  //     * @param blockId Block ID
  //     * @param accountId Account ID
  //     * @return AccountState
  //     * @throws Exception if query fails
  //     */
  //    public AdnlLiteClient.AccountState getAccountState(
  //            AdnlLiteClient.BlockIdExt blockId,
  //            AdnlLiteClient.AccountId accountId) throws Exception {
  //        return executeQuery(client -> {
  //            try {
  //                return client.getAccountState(blockId, accountId);
  //            } catch (Exception e) {
  //                throw new RuntimeException(e);
  //            }
  //        });
  //    }

  /**
   * Get number of active connections
   *
   * @return Number of active connections
   */
  public int getActiveConnectionCount() {
    synchronized (connections) {
      return (int) connections.stream().mapToLong(c -> c.isConnected() ? 1 : 0).sum();
    }
  }

  /**
   * Get total number of connections
   *
   * @return Total number of connections
   */
  public int getTotalConnectionCount() {
    synchronized (connections) {
      return connections.size();
    }
  }

  /** Start health checker to monitor connections */
  private void startHealthChecker() {
    healthChecker.scheduleAtFixedRate(
        () -> {
          if (closed) return;

          synchronized (connections) {
            connections.removeIf(
                client -> {
                  if (!client.isConnected()) {
                    logger.info("Removing disconnected client");
                    client.close();
                    return true;
                  }
                  return false;
                });
          }

          int activeCount = getActiveConnectionCount();
          int totalCount = getTotalConnectionCount();

          if (activeCount < totalCount) {
            logger.warning("Some connections are inactive: " + activeCount + "/" + totalCount);
          }
        },
        30,
        30,
        TimeUnit.SECONDS);
  }

  /** Close all connections */
  public void close() {
    closed = true;

    healthChecker.shutdown();
    try {
      if (!healthChecker.awaitTermination(5, TimeUnit.SECONDS)) {
        healthChecker.shutdownNow();
      }
    } catch (InterruptedException e) {
      healthChecker.shutdownNow();
      Thread.currentThread().interrupt();
    }

    synchronized (connections) {
      for (AdnlLiteClient client : connections) {
        try {
          client.close();
        } catch (Exception e) {
          logger.log(Level.WARNING, "Error closing client", e);
        }
      }
      connections.clear();
    }

    logger.info("Connection pool closed");
  }

  /**
   * Convert integer IP to string
   *
   * @param ip Integer IP
   * @return IP string
   */
  private static String intToIp(int ip) {
    return String.format(
        "%d.%d.%d.%d", (ip >>> 24) & 0xFF, (ip >>> 16) & 0xFF, (ip >>> 8) & 0xFF, ip & 0xFF);
  }

  /** Global config class for parsing liteserver configurations */
  public static class GlobalConfig {
    private List<LiteServer> liteservers = new ArrayList<>();

    public List<LiteServer> getLiteservers() {
      return liteservers;
    }

    public void setLiteservers(List<LiteServer> liteservers) {
      this.liteservers = liteservers;
    }

    public static class LiteServer {
      private int ip;
      private int port;
      private Id id;

      public int getIp() {
        return ip;
      }

      public void setIp(int ip) {
        this.ip = ip;
      }

      public int getPort() {
        return port;
      }

      public void setPort(int port) {
        this.port = port;
      }

      public Id getId() {
        return id;
      }

      public void setId(Id id) {
        this.id = id;
      }

      public static class Id {
        private String key;

        public String getKey() {
          return key;
        }

        public void setKey(String key) {
          this.key = key;
        }
      }
    }
  }
}
