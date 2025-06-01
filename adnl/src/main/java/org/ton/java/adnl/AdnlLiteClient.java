package org.ton.java.adnl;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ton.ton4j.tl.types.MasterchainInfo;

/**
 * Native ADNL Lite Client implementation for TON blockchain Uses TCP transport to communicate with
 * liteservers
 */
public class AdnlLiteClient {
  private static final Logger logger = Logger.getLogger(AdnlLiteClient.class.getName());

  private final AdnlTcpTransport transport;
  private final ScheduledExecutorService pingScheduler;
  private volatile boolean connected = false;

  /** Create lite client with generated keys */
  public AdnlLiteClient() {
    this.transport = new AdnlTcpTransport();
    this.pingScheduler = Executors.newSingleThreadScheduledExecutor();
  }

  /**
   * Create lite client with specific client keys
   *
   * @param client Client with keys
   */
  public AdnlLiteClient(Client client) {
    this.transport = new AdnlTcpTransport(client);
    this.pingScheduler = Executors.newSingleThreadScheduledExecutor();
  }

  /**
   * Connect to liteserver
   *
   * @param host Server host
   * @param port Server port
   * @param serverPublicKeyBase64 Server's Ed25519 public key (base64 encoded)
   * @throws Exception if connection fails
   */
  public void connect(String host, int port, String serverPublicKeyBase64) throws Exception {
    byte[] serverPublicKey = Base64.getDecoder().decode(serverPublicKeyBase64);
    transport.connect(host, port, serverPublicKey);
    connected = true;

    // Start ping scheduler (every 5 seconds as per specification)
    startPingScheduler();

    logger.info("Connected to liteserver " + host + ":" + port);
  }

  /** Start ping scheduler to maintain connection */
  private void startPingScheduler() {
    pingScheduler.scheduleAtFixedRate(
        () -> {
          try {
            if (connected && transport.isConnected()) {
              transport.ping().get(5, TimeUnit.SECONDS);
              logger.fine("Ping successful");
            }
          } catch (Exception e) {
            logger.log(Level.WARNING, "Ping failed", e);
            // Connection might be lost, could implement reconnection logic here
          }
        },
        5,
        5,
        TimeUnit.SECONDS);
  }

  /**
   * Get masterchain info
   *
   * @return MasterchainInfo
   * @throws Exception if query fails
   */
  public MasterchainInfo getMasterchainInfo() throws Exception {
    if (!connected || !transport.isConnected()) {
      throw new IllegalStateException("Not connected to liteserver");
    }

    // Create a direct binary representation of the getMasterchainInfo query
    // This matches exactly what the Go implementation sends
    // The TL-B schema for liteServer.getMasterchainInfo is:
    // liteServer.getMasterchainInfo = liteServer.MasterchainInfo;
    // Constructor ID: 0x2ee6b589 (little endian: 89 b5 e6 2e)
    byte[] queryBytes =
        new byte[] {
          (byte) 0xdf, // liteServer.query constructor
          (byte) 0x06,
          (byte) 0x8c,
          (byte) 0x79,
          (byte) 0x04, // bytes length (4 bytes)
          (byte) 0x2e, // getMasterchainInfo constructor
          (byte) 0xe6,
          (byte) 0xb5,
          (byte) 0x89,
          (byte) 0x00, // padding to align to 4 bytes
          (byte) 0x00,
          (byte) 0x00
        };

    // Log the query details for debugging
    logger.info("Sending getMasterchainInfo query, size: " + queryBytes.length + " bytes");
    logger.info("Query hex: " + bytesToHex(queryBytes));

    // Try alternative approach - create the query using the TL serializer
    Object response = null;
    try {

      byte[] serializedQuery = queryBytes;
      try {
        // Send query and wait for response with increased timeout
        response = transport.query(serializedQuery).get(60, TimeUnit.SECONDS);
        //        logger.info(
        //            "Received response of type: "
        //                + (response != null ? response.getClass().getName() : "null"));
      } catch (Exception e) {
        logger.log(Level.WARNING, "Error with serialized query: " + e.getMessage(), e);

        // Check if we're still connected
        if (!transport.isConnected()) {
          logger.warning("Connection lost during query, reconnecting is required");
          connected = false;
          throw new IOException("Connection lost during query", e);
        }

        throw e;
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, "Error with serialized query approach: " + e.getMessage(), e);

      // Check if we're still connected before trying the direct binary approach
      if (!transport.isConnected()) {
        logger.warning("Connection lost, reconnecting is required");
        connected = false;
        throw new IOException("Connection lost, reconnecting is required", e);
      }
    }

    // Parse response
    try {
      return MasterchainInfo.deserialize((byte[]) response);

    } catch (Exception e) {
      logger.log(Level.WARNING, "Error parsing response: " + e.getMessage(), e);
    }

    throw new Exception(
        "Invalid response format: " + (response != null ? response.getClass().getName() : "null"));
  }

  /** Convert bytes to hex string for debugging */
  private String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%02x", b & 0xff));
    }
    return sb.toString();
  }

  /** Close connection */
  public void close() {
    connected = false;
    pingScheduler.shutdown();
    transport.close();

    try {
      if (!pingScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
        pingScheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      pingScheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Check if connected
   *
   * @return true if connected
   */
  public boolean isConnected() {
    return connected && transport.isConnected();
  }

  /** Convert int to bytes (little endian) */
  private static byte[] intToBytes(int value) {
    return java.nio.ByteBuffer.allocate(4)
        .order(java.nio.ByteOrder.LITTLE_ENDIAN)
        .putInt(value)
        .array();
  }

  /** Create map from key-value pairs */
  private static <K, V> Map<K, V> mapOf(Object... keyValues) {
    Map<K, V> map = new HashMap<>();
    for (int i = 0; i < keyValues.length; i += 2) {
      @SuppressWarnings("unchecked")
      K key = (K) keyValues[i];
      @SuppressWarnings("unchecked")
      V value = (V) keyValues[i + 1];
      map.put(key, value);
    }
    return map;
  }
}
