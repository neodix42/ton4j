package org.ton.java.adnl;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;
import javax.crypto.Cipher;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.tl.types.MasterchainInfo;

/**
 * TCP-based ADNL transport implementation for lite-server communication Based on the ADNL-TCP
 * specification and Go reference implementation
 */
@Slf4j
public class AdnlTcpTransport {

  private Socket socket;
  private DataInputStream input;
  private DataOutputStream output;
  private Cipher readCipher;
  private Cipher writeCipher;
  private volatile boolean connected = false;
  private volatile boolean running = false;

  private final Client client;
  private final ConcurrentHashMap<String, CompletableFuture<Object>> activeQueries =
      new ConcurrentHashMap<>();
  private final ConcurrentHashMap<Long, CompletableFuture<TcpPong>> activePings =
      new ConcurrentHashMap<>();
  private final ScheduledExecutorService timeoutExecutor = Executors.newScheduledThreadPool(2);

  private Thread listenerThread;
  private boolean authenticated = false;
  private byte[] ourNonce;
  private byte[] authKey;
  private CompletableFuture<Void> authFuture;

  public AdnlTcpTransport() {
    this.client = Client.generate();
  }

  public AdnlTcpTransport(Client client) {
    this.client = client;
  }

  public void connect(String host, int port, byte[] serverPublicKey) throws Exception {
    connect(host, port, serverPublicKey, null);
  }

  public void connect(String host, int port, byte[] serverPublicKey, byte[] authKey)
      throws Exception {
    log.info("Connecting to {}:{}", host, port);

    socket = new Socket();
    socket.connect(new InetSocketAddress(host, port), 10000);
    socket.setSoTimeout(30000);

    input = new DataInputStream(socket.getInputStream());
    output = new DataOutputStream(socket.getOutputStream());

    performHandshake(serverPublicKey);
    startListener();
    waitForHandshakeConfirmation();

    connected = true;
    log.info("Connected successfully");

    // Authentication is optional for lite-servers
    if (authKey != null) {
      authenticate(authKey);
    }
    // Note: Most liteservers don't require authentication for basic queries
  }

  private void performHandshake(byte[] serverPublicKey) throws Exception {
    log.info("Performing ADNL handshake");

    // Generate 160 random bytes for encryption keys (matching Go implementation)
    byte[] randomData = new byte[160];
    new SecureRandom().nextBytes(randomData);

    // Build ciphers for incoming and outgoing packets (matching Go implementation)
    readCipher =
        CryptoUtils.createAESCtrCipher(
            Arrays.copyOfRange(randomData, 0, 32), // rnd[:32]
            Arrays.copyOfRange(randomData, 64, 80), // rnd[64:80]
            Cipher.DECRYPT_MODE);

    writeCipher =
        CryptoUtils.createAESCtrCipher(
            Arrays.copyOfRange(randomData, 32, 64), // rnd[32:64]
            Arrays.copyOfRange(randomData, 80, 96), // rnd[80:96]
            Cipher.ENCRYPT_MODE);

    // Calculate server key ID using TL hash (matching Go implementation)
    byte[] serverKeyId = calculateKeyId(serverPublicKey);
    byte[] clientPublicKey = client.getEd25519Public();

    // Calculate checksum of random data
    MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
    byte[] checksum = sha256.digest(randomData);

    // Calculate shared key using proper Ed25519 to X25519 conversion
    byte[] sharedKey = CryptoUtils.sharedKey(client.getEd25519Private(), serverPublicKey);

    // Build handshake encryption key and IV (matching Go implementation exactly)
    // k := key[0:16] + checksum[16:32]
    // iv := checksum[0:4] + key[20:32]
    byte[] k = new byte[32];
    System.arraycopy(sharedKey, 0, k, 0, 16); // key[0:16]
    System.arraycopy(checksum, 16, k, 16, 16); // checksum[16:32]

    byte[] iv = new byte[16];
    System.arraycopy(checksum, 0, iv, 0, 4); // checksum[0:4]
    System.arraycopy(sharedKey, 20, iv, 4, 12); // key[20:32]

    // Create handshake cipher and encrypt the random data
    Cipher handshakeCipher = CryptoUtils.createAESCtrCipher(k, iv, Cipher.ENCRYPT_MODE);
    byte[] encryptedData = CryptoUtils.aesCtrTransform(handshakeCipher, randomData);

    log.info("Server key ID: " + CryptoUtils.hex(serverKeyId));
    log.info("Client public key: " + CryptoUtils.hex(clientPublicKey));

    // Build handshake packet: serverKeyId(32) + clientPublicKey(32) + checksum(32) +
    // encryptedData(160)
    // Total: 256 bytes (matching Go implementation)
    ByteBuffer handshakePacket = ByteBuffer.allocate(256);
    handshakePacket.put(serverKeyId); // 32 bytes
    handshakePacket.put(clientPublicKey); // 32 bytes
    handshakePacket.put(checksum); // 32 bytes
    handshakePacket.put(encryptedData); // 160 bytes

    // Send handshake packet directly (no encryption, no framing)
    // This is the raw 256-byte handshake packet as per ADNL specification
    output.write(handshakePacket.array());
    output.flush();

    log.info("Handshake packet sent with serverKeyId: " + CryptoUtils.hex(serverKeyId));
  }

  private byte[] calculateKeyId(byte[] publicKey) throws Exception {
    // Calculate TL constructor ID for pub.ed25519 schema
    String tlSchema = "pub.ed25519 key:int256 = PublicKey";
    CRC32 crc32 = new CRC32();
    crc32.update(tlSchema.getBytes("UTF-8"));
    long constructorId = crc32.getValue();

    // Build TL-serialized structure: constructor_id + key
    ByteBuffer buffer = ByteBuffer.allocate(4 + publicKey.length);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt((int) constructorId);
    buffer.put(publicKey);

    MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
    return sha256.digest(buffer.array());
  }

  private void startListener() {
    running = true;
    listenerThread = new Thread(this::listenForPackets);
    listenerThread.setDaemon(true);
    listenerThread.start();
  }

  /** Wait for handshake confirmation (empty packet) */
  private void waitForHandshakeConfirmation() throws Exception {
    // The server responds with an empty packet to confirm handshake
    long timeout = System.currentTimeMillis() + 10000; // 10 second timeout

    while (System.currentTimeMillis() < timeout && !connected) {
      Thread.sleep(100);
      // Check if listener thread is still running
      if (listenerThread != null && !listenerThread.isAlive()) {
        throw new Exception("Packet listener thread died during handshake");
      }
    }

    if (!connected) {
      throw new Exception("Handshake confirmation timeout");
    }
  }

  /** Listen for incoming packets */
  private void listenForPackets() {
    log.info("Starting packet listener");

    try {
      while (running && !socket.isClosed()) {
        // Read packet size (4 bytes, little endian) with proper partial read handling
        byte[] sizeBytes = readExactBytes(4);
        if (sizeBytes == null) {
          break; // Connection closed
        }

        // Decrypt size in-place to maintain cipher state
        CryptoUtils.aesCtrTransformInPlace(readCipher, sizeBytes);

        // Read as unsigned integer and handle properly
        long packetSizeLong =
            ByteBuffer.wrap(sizeBytes).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xFFFFFFFFL;

        log.info("Decrypted packet size: " + packetSizeLong);

        // Validate packet size
        if (packetSizeLong > 16 * 1024 * 1024) { // 16MB limit
          throw new IOException("Packet too large: " + packetSizeLong);
        }
        if (packetSizeLong < 0) { // Negative size is invalid
          throw new IOException("Invalid packet size: " + packetSizeLong);
        }

        int packetSize = (int) packetSizeLong;

        // Read packet data with proper partial read handling
        byte[] packetData = readExactBytes(packetSize);
        if (packetData == null) {
          break; // Connection closed
        }

        // Decrypt packet data in-place to maintain cipher state
        CryptoUtils.aesCtrTransformInPlace(readCipher, packetData);

        // Process packet
        processIncomingPacket(packetData);
      }
    } catch (Exception e) {
      if (running) {
        log.info("Error in packet listener", e);
      }
    } finally {
      running = false;
      try {
        if (socket != null && !socket.isClosed()) {
          socket.close();
        }
      } catch (IOException e) {
        log.warn("Error closing socket", e);
      }
    }
  }

  /**
   * Read exact number of bytes from socket, handling partial reads Similar to Go's readData
   * function
   */
  private byte[] readExactBytes(int count) throws IOException {
    byte[] result = new byte[count];
    int totalRead = 0;

    while (totalRead < count) {
      int bytesRead = input.read(result, totalRead, count - totalRead);
      if (bytesRead == -1) {
        // Connection closed
        return null;
      }
      totalRead += bytesRead;
    }

    return result;
  }

  private void processIncomingPacket(byte[] packetData) {
    try {
      if (packetData.length == 0) {
        // Empty packet = handshake confirmation
        connected = true;
        log.info("Received handshake confirmation (empty packet)");
        return;
      }

      // Validate packet checksum
      if (packetData.length < 64) { // 32 bytes nonce + 32 bytes checksum minimum
        log.info("Packet too small: {}", packetData.length);
        return;
      }

      byte[] nonce = Arrays.copyOfRange(packetData, 0, 32);
      byte[] payload = Arrays.copyOfRange(packetData, 32, packetData.length - 32);
      byte[] receivedChecksum =
          Arrays.copyOfRange(packetData, packetData.length - 32, packetData.length);

      // Verify checksum
      MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
      sha256.update(nonce);
      sha256.update(payload);
      byte[] calculatedChecksum = sha256.digest();

      if (!Arrays.equals(receivedChecksum, calculatedChecksum)) {
        log.info("Invalid packet checksum");
        log.info("Received checksum: {}", CryptoUtils.hex(receivedChecksum));
        log.info("Calculated checksum: {}", CryptoUtils.hex(calculatedChecksum));
        return;
      }

      // Handle handshake confirmation (empty payload)
      if (payload.length == 0) {
        connected = true;
        log.info("Received handshake confirmation (empty packet)");
        return;
      }

      // Log payload for debugging
      log.info("Received payload of size: {}", payload.length + " bytes");
      log.info("Payload hex: {}", CryptoUtils.hex(payload));

      // Special handling for liteServer responses
      // This is a direct response to our query
      if (payload.length >= 4) {
        try {
          // Check for known constructor IDs
          byte[] constructorId = Arrays.copyOfRange(payload, 0, 4);
          byte[] queryId = Arrays.copyOfRange(payload, 4, 36);
          int querySize = Arrays.copyOfRange(payload, 36, 37)[0] & 0xFF;
          byte[] queryBody = Arrays.copyOfRange(payload, 37, 37 + querySize);
          byte[] queryBodyConstructorId = Arrays.copyOfRange(queryBody, 0, 4);
          byte[] queryBodyPayload = Arrays.copyOfRange(queryBody, 4, queryBody.length);
          log.info("Checking constructorId: {}", CryptoUtils.hex(constructorId));
          log.info("Checking queryId: {}", CryptoUtils.hex(queryId));
          log.info("Checking queryBody: {}", CryptoUtils.hex(queryBody));
          log.info("Checking queryBodyConstructorId: {}", CryptoUtils.hex(queryBodyConstructorId));
          log.info("Checking queryBodyPayload: {}", CryptoUtils.hex(queryBodyPayload));

          // Check for liteServer.masterchainInfo (constructor ID: 0x81288385)
          //          if (Arrays.equals(
          //                  queryBodyConstructorId, new byte[] {(byte) 0x85, (byte) 0x83, (byte)
          // 0x28, (byte) 0x81})) {
          //            log.info("Detected liteServer.masterchainInfo response by constructor
          // ID");

          // Find any active getMasterchainInfo query and complete it
          if (!activeQueries.isEmpty()) {
            String queryIdStr = activeQueries.keySet().iterator().next();
            CompletableFuture<Object> future = activeQueries.remove(queryIdStr);
            if (future != null) {
              // Try to deserialize as liteServer.masterchainInfo
              try {
                byte[] result = new byte[0];
                if (CryptoUtils.hex(queryBodyConstructorId).equals(MasterchainInfo.constructorId)) {
                  result = MasterchainInfo.deserialize(queryBodyPayload).serialize();
                }
                //                  Map<String, Object> result =
                // schemas.deserializeToMap(payload);
                log.info("Successfully deserialized liteServer.masterchainInfo response");
                future.complete(result);
              } catch (Exception e) {
                log.error(
                    "Could not deserialize as liteServer.masterchainInfo, completing with raw bytes: ",
                    e);
                future.complete(payload);
              }
              return;
            }
          }
          //          }

        } catch (Exception e) {
          log.error("Error checking for TL response:", e);
        }
      }

      log.info("Received unhandled payload: " + CryptoUtils.hex(payload));

    } catch (Exception e) {
      log.error("Error processing incoming packet", e);
    }
  }

  public void sendPacket(byte[] payload) throws Exception {
    if (!connected) {
      throw new IllegalStateException("Not connected");
    }

    // Build packet following the Go implementation:
    // [size:4][nonce:32][payload:N][checksum:32]

    // First allocate buffer with size + nonce, then we'll append payload and checksum
    int totalSize = 32 + payload.length + 32; // nonce + payload + checksum
    ByteBuffer packet = ByteBuffer.allocate(4 + totalSize);
    packet.order(ByteOrder.LITTLE_ENDIAN);

    // Size (4 bytes LE) - this is the size of the data after the size field
    packet.putInt(totalSize);
    log.info("packet size: " + totalSize);

    // Generate nonce (32 bytes)
    byte[] nonce =
        CryptoUtils.hexToBytes("5fb13e11977cb5cff0fbf7f23f674d734cb7c4bf01322c5e6b928c5d8ea09cfd");
    //    byte[] nonce = new byte[32];
    //    new SecureRandom().nextBytes(nonce);
    packet.put(nonce);
    log.info("packet nonce: " + CryptoUtils.hex(nonce));
    // Payload
    packet.put(payload);
    log.info("packet payload: " + CryptoUtils.hex(payload));

    // Calculate checksum from nonce + payload (matching Go implementation)
    MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
    sha256.update(nonce);
    sha256.update(payload);
    byte[] checksum = sha256.digest();
    log.info("packet checksum: " + CryptoUtils.hex(checksum));
    // Add checksum
    packet.put(checksum);

    // Encrypt and send
    byte[] packetData = packet.array();

    // Encrypt in-place to maintain cipher state (matching Go implementation)
    CryptoUtils.aesCtrTransformInPlace(writeCipher, packetData);

    synchronized (output) {
      output.write(packetData);
      output.flush();
    }
  }

  public CompletableFuture<TcpPong> ping() {
    try {
      long randomId = new SecureRandom().nextLong();

      Map<String, Object> pingData = new HashMap<>();
      pingData.put("@type", "tcp.ping");
      pingData.put("random_id", randomId);

      //      byte[] serialized = schemas.serialize("tcp.ping", pingData, true); // todo replace
      byte[] serialized = null; // todo replace

      CompletableFuture<TcpPong> future = new CompletableFuture<>();
      activePings.put(randomId, future);

      sendPacket(serialized);

      // Set timeout
      timeoutExecutor.schedule(
          () -> {
            if (activePings.remove(randomId) != null) {
              future.completeExceptionally(new Exception("Ping timeout"));
            }
          },
          5,
          TimeUnit.SECONDS);

      return future;
    } catch (Exception e) {
      CompletableFuture<TcpPong> future = new CompletableFuture<>();
      future.completeExceptionally(e);
      return future;
    }
  }

  public CompletableFuture<Object> query(Object query) {
    try {
      if (!connected || socket == null || socket.isClosed()) {
        throw new IllegalStateException("Not connected or socket closed");
      }

      // Generate query ID
      // byte[] queryId = new byte[32]; was random
      byte[] queryId =
          CryptoUtils.hexToBytes(
              "77c1545b96fa136b8e01cc08338bec47e8a43215492dda6d4d7e286382bb00c4");
      //      new SecureRandom().nextBytes(queryId);

      log.info("Sending query with ID: " + CryptoUtils.hex(queryId));
      //      log.info("Query type: " + query.getClass().getName());
      if (query instanceof byte[]) {
        log.info("Query size: " + ((byte[]) query).length + " bytes");

        // Log the hex representation of the query for debugging
        log.info("Query hex: " + CryptoUtils.hex((byte[]) query));
      }

      // Wrap in ADNL query
      //      java.util.Map<String, Object> adnlQuery = new java.util.HashMap<>();
      //      adnlQuery.put("@type", "adnl.message.query");
      //      adnlQuery.put("query_id", queryId);
      //      adnlQuery.put("query", query);

      // byte[] serialized = schemas.serialize("adnl.message.query", adnlQuery, true);
      byte[] serialized =
          CryptoUtils.hexToBytes(
              "7af98bb4"
                  + "77c1545b96fa136b8e01cc08338bec47e8a43215492dda6d4d7e286382bb00c4"
                  + "0"
                  + Integer.toHexString(((byte[]) query).length) // works but need 0
                  //                  + "0c"
                  + CryptoUtils.hex((byte[]) query)
                  + "000000");
      //      log.info("Serialized ADNL query size: " + serialized.length + " bytes");
      log.info("Serialized ADNL query hex: " + CryptoUtils.hex(serialized));

      // Calculate the total packet size for verification
      int totalSize = 32 + serialized.length + 32; // nonce + payload + checksum
      log.info("Total packet size will be: " + (4 + totalSize) + " bytes");

      CompletableFuture<Object> future = new CompletableFuture<>();
      String queryIdHex = CryptoUtils.hex(queryId);
      activeQueries.put(queryIdHex, future);
      log.info("Added query to active queries with ID: " + queryIdHex);

      // Send the packet before setting up the timeout to ensure it's sent
      try {
        sendPacket(serialized); // ADNLQuery
        log.info("Query packet sent successfully");
      } catch (Exception e) {
        // If sending fails, remove the query from active queries and complete the future
        // exceptionally
        activeQueries.remove(queryIdHex);
        throw e;
      }

      // Set timeout - increased to 60 seconds for liteserver queries
      timeoutExecutor.schedule(
          () -> {
            if (activeQueries.remove(queryIdHex) != null) {
              log.info("Query timed out: " + queryIdHex);
              future.completeExceptionally(new Exception("Query timeout"));

              // Check if we need to reconnect
              if (connected && (socket == null || socket.isClosed())) {
                log.info("Socket closed during query, marking as disconnected");
                connected = false;
              }
            }
          },
          60,
          TimeUnit.SECONDS);

      return future;
    } catch (Exception e) {
      log.info("Error sending query", e);
      CompletableFuture<Object> future = new CompletableFuture<>();
      future.completeExceptionally(e);
      return future;
    }
  }

  private void authenticate(byte[] authKey) throws Exception {
    this.authKey = authKey;
    this.authFuture = new CompletableFuture<>();

    // Generate our nonce
    ourNonce = new byte[32];
    new SecureRandom().nextBytes(ourNonce);

    log.info("Starting authentication with nonce: " + CryptoUtils.hex(ourNonce));

    // Send authentication request
    Map<String, Object> authRequest = new HashMap<>();
    authRequest.put("@type", "tcp.authentificate");
    authRequest.put("nonce", ourNonce);

    // byte[] serialized = schemas.serialize("tcp.authentificate", authRequest, true); // replace
    // todo
    byte[] serialized = null;
    sendPacket(serialized);

    // Wait for authentication to complete
    authFuture.get(10, TimeUnit.SECONDS);
    authenticated = true;
    log.info("Authentication completed successfully");
  }

  public void close() {
    running = false;
    connected = false;

    if (listenerThread != null) {
      listenerThread.interrupt();
    }

    // Shutdown timeout executor
    timeoutExecutor.shutdown();

    try {
      if (socket != null && !socket.isClosed()) {
        socket.close();
      }
    } catch (IOException e) {
      log.info("Error closing socket", e);
    }
  }

  public boolean isConnected() {
    return connected && socket != null && !socket.isClosed();
  }

  private static byte[] intToBytes(int value) {
    return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
  }

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

  public static class TcpPong {
    private final long randomId;

    public TcpPong(long randomId) {
      this.randomId = randomId;
    }

    public long getRandomId() {
      return randomId;
    }
  }
}
