package org.ton.java.adnl;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.tl.queries.LiteServerQuery;
import org.ton.ton4j.tl.types.CurrentTime;
import org.ton.ton4j.tl.types.LiteServerAnswer;
import org.ton.ton4j.tl.types.MasterchainInfo;
import org.ton.ton4j.tl.types.Version;

/**
 * Native ADNL Lite Client implementation for TON blockchain Uses TCP transport to communicate with
 * lite-servers
 */
@Slf4j
public class AdnlLiteClient {

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

    log.info("Connected to lite-server " + host + ":" + port);
  }

  /** Start ping scheduler to maintain connection */
  private void startPingScheduler() {
    pingScheduler.scheduleAtFixedRate(
        () -> {
          try {
            if (connected && transport.isConnected()) {
              transport.ping().get(5, TimeUnit.SECONDS);
              log.info("Ping successful");
            }
          } catch (Exception e) {
            log.warn("Adnl tcp.Ping failed: ", e);
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
      throw new IllegalStateException("Not connected to lite-server");
    }

    byte[] queryBytes =
        LiteServerQuery.serialize("liteServer.getMasterchainInfo = liteServer.MasterchainInfo");

    // Log the query details for debugging
    log.info("Sending getMasterchainInfo query, size: {} bytes", queryBytes.length);
    log.info("Query hex: {}", CryptoUtils.hex(queryBytes));

    LiteServerAnswer response;
    try {
      response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
      return (MasterchainInfo) response;
    } catch (Exception e) {
      log.warn("Error with serialized query approach: {}", e.getMessage(), e);

      // Check if we're still connected before trying the direct binary approach
      if (!transport.isConnected()) {
        log.info("Connection lost, reconnecting is required");
        connected = false;
        throw new IOException("Connection lost, reconnecting is required", e);
      }
    }

    throw new Exception("Was not able to retrieve masterchainInfo from lite server");
  }

  public CurrentTime getTime() throws Exception {
    if (!connected || !transport.isConnected()) {
      throw new IllegalStateException("Not connected to lite-server");
    }

    byte[] queryBytes = LiteServerQuery.serialize("liteServer.getTime = liteServer.CurrentTime");
    log.info("Sending getTime query, size: {} bytes", queryBytes.length);
    log.info("Query hex: {}", CryptoUtils.hex(queryBytes));

    LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
    return (CurrentTime) response;
  }

  public Version getVersion() throws Exception {
    if (!connected || !transport.isConnected()) {
      throw new IllegalStateException("Not connected to lite-server");
    }

    byte[] queryBytes = LiteServerQuery.serialize("liteServer.getVersion = liteServer.Version");
    log.info("Sending getVersion query, size: {} bytes", queryBytes.length);
    log.info("Query hex: {}", CryptoUtils.hex(queryBytes));

    LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
    return (Version) response;
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
}
