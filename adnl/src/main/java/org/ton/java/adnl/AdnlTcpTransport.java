package org.ton.java.adnl;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;

/**
 * TCP-based ADNL transport implementation for liteserver communication Based on the ADNL-TCP
 * specification and Go reference implementation
 */
public class AdnlTcpTransport {
  private static final Logger logger = Logger.getLogger(AdnlTcpTransport.class.getName());

  private Socket socket;
  private DataInputStream input;
  private DataOutputStream output;
  private Cipher readCipher;
  private Cipher writeCipher;
  private volatile boolean connected = false;
  private volatile boolean running = false;

  private final Client client;
  private final TLGenerator.TLSchemas schemas;
  private final ConcurrentHashMap<String, CompletableFuture<Object>> activeQueries =
      new ConcurrentHashMap<>();
  private final ConcurrentHashMap<Long, CompletableFuture<TcpPong>> activePings =
      new ConcurrentHashMap<>();
  private final ScheduledExecutorService timeoutExecutor = Executors.newScheduledThreadPool(2);

  private Thread listenerThread;
  private boolean authenticated = false;
  private byte[] ourNonce;

  public AdnlTcpTransport() {
    this.client = Client.generate();
    this.schemas = createTcpSchemas();
  }

  public AdnlTcpTransport(Client client) {
    this.client = client;
    this.schemas = createTcpSchemas();
  }

  public void connect(String host, int port, byte[] serverPublicKey) throws Exception {
    connect(host, port, serverPublicKey, null);
  }

  public void connect(String host, int port, byte[] serverPublicKey, byte[] authKey)
      throws Exception {
    logger.info("Connecting to " + host + ":" + port);

    socket = new Socket();
    socket.connect(new InetSocketAddress(host, port), 10000);
    socket.setSoTimeout(30000);

    input = new DataInputStream(socket.getInputStream());
    output = new DataOutputStream(socket.getOutputStream());

    performHandshake(serverPublicKey);
    startListener();
    waitForHandshakeConfirmation();

    connected = true;
    logger.info("Connected successfully");

    if (authKey != null) {
      authenticate(authKey);
    }
  }

  private void performHandshake(byte[] serverPublicKey) throws Exception {
    logger.fine("Performing ADNL handshake");

    byte[] randomData = new byte[160];
    new SecureRandom().nextBytes(randomData);

    readCipher =
        CryptoUtils.createAESCtrCipher(
            Arrays.copyOfRange(randomData, 0, 32),
            Arrays.copyOfRange(randomData, 64, 80),
            Cipher.DECRYPT_MODE);

    writeCipher =
        CryptoUtils.createAESCtrCipher(
            Arrays.copyOfRange(randomData, 32, 64),
            Arrays.copyOfRange(randomData, 80, 96),
            Cipher.ENCRYPT_MODE);

    byte[] serverKeyId = calculateKeyId(serverPublicKey);
    byte[] clientPublicKey = client.getEd25519Public();

    MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
    byte[] checksum = sha256.digest(randomData);

    byte[] sharedKey =
        CryptoUtils.getSharedKey(
            client.getX25519Private(), CryptoUtils.convertEd25519ToX25519Public(serverPublicKey));

    // Derive encryption key using SHA256(sharedKey + checksum)
    byte[] keyMaterial = new byte[sharedKey.length + checksum.length];
    System.arraycopy(sharedKey, 0, keyMaterial, 0, sharedKey.length);
    System.arraycopy(checksum, 0, keyMaterial, sharedKey.length, checksum.length);
    byte[] keyDigest = MessageDigest.getInstance("SHA-256").digest(keyMaterial);

    byte[] encKey = new byte[32];
    System.arraycopy(keyDigest, 0, encKey, 0, 16);
    System.arraycopy(checksum, 16, encKey, 16, 16);

    byte[] iv = new byte[16];
    System.arraycopy(checksum, 0, iv, 0, 4);
    System.arraycopy(sharedKey, 20, iv, 4, 12);

    Cipher handshakeCipher = CryptoUtils.createAESCtrCipher(encKey, iv, Cipher.ENCRYPT_MODE);
    byte[] encrypted = CryptoUtils.aesCtrTransform(handshakeCipher, randomData);

    logger.info("Server key ID: " + CryptoUtils.hex(serverKeyId));
    logger.info("Client public key: " + CryptoUtils.hex(clientPublicKey));

    ByteBuffer handshakePacket = ByteBuffer.allocate(256);
    handshakePacket.put(serverKeyId);
    handshakePacket.put(clientPublicKey);
    handshakePacket.put(checksum);
    handshakePacket.put(encrypted);

    output.write(handshakePacket.array());
    output.flush();

    logger.info("Handshake packet sent with serverKeyId: " + CryptoUtils.hex(serverKeyId));
  }

  private byte[] calculateKeyId(byte[] publicKey) throws Exception {
    ByteBuffer buffer = ByteBuffer.allocate(4 + publicKey.length);
    buffer.put(new byte[] {(byte) 0xC6, (byte) 0xB4, (byte) 0x13, (byte) 0x48});
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
    }

    if (!connected) {
      throw new Exception("Handshake confirmation timeout");
    }
  }

  /** Listen for incoming packets */
  private void listenForPackets() {
    logger.fine("Starting packet listener");

    try {
      while (running && !socket.isClosed()) {
        // Read packet size (4 bytes, little endian)
        byte[] sizeBytes = new byte[4];
        input.readFully(sizeBytes);

        // Decrypt size into a new array to avoid overwriting
        byte[] decryptedSizeBytes = readCipher.update(sizeBytes);

        // Read as unsigned integer (Java doesn't have unsigned int, so use long)
        long packetSizeLong =
            ByteBuffer.wrap(decryptedSizeBytes).order(ByteOrder.LITTLE_ENDIAN).getInt()
                & 0xFFFFFFFFL;

        logger.info("Decrypted packet size: " + packetSizeLong);

        if (packetSizeLong > 16 * 1024 * 1024) { // 16MB limit
          throw new IOException("Packet too large: " + packetSizeLong);
        }

        int packetSize = (int) packetSizeLong;

        // Read packet data
        byte[] packetData = new byte[packetSize];
        input.readFully(packetData);

        // Decrypt packet data into a new array
        byte[] decryptedPacketData = readCipher.update(packetData);

        // Process packet
        processIncomingPacket(decryptedPacketData);
      }
    } catch (Exception e) {
      if (running) {
        logger.log(Level.SEVERE, "Error in packet listener", e);
      }
    } finally {
      running = false;
      try {
        if (socket != null && !socket.isClosed()) {
          socket.close();
        }
      } catch (IOException e) {
        logger.log(Level.WARNING, "Error closing socket", e);
      }
    }
  }

  private void processIncomingPacket(byte[] packetData) {
    try {
      if (packetData.length == 0) {
        // Empty packet = handshake confirmation
        connected = true;
        logger.fine("Received handshake confirmation");
        return;
      }

      // Validate packet checksum
      if (packetData.length < 64) { // 32 bytes nonce + 32 bytes checksum minimum
        logger.warning("Packet too small: " + packetData.length);
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
        logger.warning("Invalid packet checksum");
        return;
      }

      // Handle handshake confirmation separately
      if (payload.length == 0) {
        connected = true;
        logger.fine("Received handshake confirmation");
        return;
      }

      // Deserialize TL message
      Object[] deserialized = schemas.deserialize(payload);
      if (deserialized[0] instanceof java.util.Map) {
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> message = (java.util.Map<String, Object>) deserialized[0];
        processMessage(message);
      }

    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error processing incoming packet", e);
    }
  }

  private void processMessage(java.util.Map<String, Object> message) {
    String type = (String) message.get("@type");
    logger.fine("Processing message: " + type);

    switch (type) {
      case "tcp.pong":
        handlePong(message);
        break;
      case "tcp.authentificationNonce":
        handleAuthNonce(message);
        break;
      case "adnl.message.answer":
        handleAnswer(message);
        break;
      default:
        logger.fine("Unknown message type: " + type);
    }
  }

  private void handlePong(java.util.Map<String, Object> message) {
    Long randomId = (Long) message.get("random_id");
    CompletableFuture<TcpPong> future = activePings.remove(randomId);
    if (future != null) {
      future.complete(new TcpPong(randomId));
    }
  }

  private void handleAuthNonce(java.util.Map<String, Object> message) {
    logger.fine("Received auth nonce");
  }

  private void handleAnswer(java.util.Map<String, Object> message) {
    byte[] queryId = (byte[]) message.get("query_id");
    String queryIdHex = CryptoUtils.hex(queryId);

    CompletableFuture<Object> future = activeQueries.remove(queryIdHex);
    if (future != null) {
      future.complete(message.get("answer"));
    }
  }

  public void sendPacket(byte[] payload) throws Exception {
    if (!connected) {
      throw new IllegalStateException("Not connected");
    }

    // Build packet: [size:4][nonce:32][payload:N][checksum:32]
    byte[] nonce = new byte[32];
    new SecureRandom().nextBytes(nonce);

    // Calculate checksum
    MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
    sha256.update(nonce);
    sha256.update(payload);
    byte[] checksum = sha256.digest();

    // Build full packet
    int totalSize = 32 + payload.length + 32; // nonce + payload + checksum
    ByteBuffer packet = ByteBuffer.allocate(4 + totalSize);
    packet.order(ByteOrder.LITTLE_ENDIAN);

    // Size (4 bytes LE)
    packet.putInt(totalSize);

    // Nonce (32 bytes)
    packet.put(nonce);

    // Payload
    packet.put(payload);

    // Checksum (32 bytes)
    packet.put(checksum);

    // Encrypt and send
    byte[] packetData = packet.array();
    writeCipher.update(packetData, 0, packetData.length, packetData, 0);

    synchronized (output) {
      output.write(packetData);
      output.flush();
    }
  }

  public CompletableFuture<TcpPong> ping() {
    try {
      long randomId = new SecureRandom().nextLong();

      java.util.Map<String, Object> pingData = new java.util.HashMap<>();
      pingData.put("@type", "tcp.ping");
      pingData.put("random_id", randomId);

      byte[] serialized = schemas.serialize("tcp.ping", pingData, true);

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
      // Generate query ID
      byte[] queryId = new byte[32];
      new SecureRandom().nextBytes(queryId);

      // Wrap in ADNL query
      java.util.Map<String, Object> adnlQuery = new java.util.HashMap<>();
      adnlQuery.put("@type", "adnl.message.query");
      adnlQuery.put("query_id", queryId);
      adnlQuery.put("query", query);

      byte[] serialized = schemas.serialize("adnl.message.query", adnlQuery, true);

      CompletableFuture<Object> future = new CompletableFuture<>();
      activeQueries.put(CryptoUtils.hex(queryId), future);

      sendPacket(serialized);

      // Set timeout
      timeoutExecutor.schedule(
          () -> {
            if (activeQueries.remove(CryptoUtils.hex(queryId)) != null) {
              future.completeExceptionally(new Exception("Query timeout"));
            }
          },
          30,
          TimeUnit.SECONDS);

      return future;
    } catch (Exception e) {
      CompletableFuture<Object> future = new CompletableFuture<>();
      future.completeExceptionally(e);
      return future;
    }
  }

  private void authenticate(byte[] authKey) throws Exception {
    // Generate our nonce
    ourNonce = new byte[32];
    new SecureRandom().nextBytes(ourNonce);

    // Send authentication request
    java.util.Map<String, Object> authRequest = new java.util.HashMap<>();
    authRequest.put("@type", "tcp.authentificate");
    authRequest.put("nonce", ourNonce);

    byte[] serialized = schemas.serialize("tcp.authentificate", authRequest, true);
    sendPacket(serialized);

    // Wait for server nonce and complete authentication
    // This would be handled in handleAuthNonce method
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
      logger.log(Level.WARNING, "Error closing socket", e);
    }
  }

  public boolean isConnected() {
    return connected && socket != null && !socket.isClosed();
  }

  private static TLGenerator.TLSchemas createTcpSchemas() {
    java.util.List<TLGenerator.TLSchema> schemas = new java.util.ArrayList<>();

    // TCP protocol schemas
    schemas.add(
        new TLGenerator.TLSchema(
            intToBytes(0x9a2b084d), "tcp.ping", "tcp.Pong", mapOf("random_id", "long")));
    schemas.add(
        new TLGenerator.TLSchema(
            intToBytes(0x8b9c0a0e), "tcp.pong", "tcp.Ping", mapOf("random_id", "long")));
    schemas.add(
        new TLGenerator.TLSchema(
            intToBytes(0x2d691b5f), "tcp.authentificate", "tcp.Message", mapOf("nonce", "bytes")));
    schemas.add(
        new TLGenerator.TLSchema(
            intToBytes(0x5c6b1c0d),
            "tcp.authentificationNonce",
            "tcp.Message",
            mapOf("nonce", "bytes")));
    schemas.add(
        new TLGenerator.TLSchema(
            intToBytes(0x4a7b2e1f),
            "tcp.authentificationComplete",
            "tcp.Message",
            mapOf("key", "bytes", "signature", "bytes")));

    // ADNL message schemas
    schemas.add(
        new TLGenerator.TLSchema(
            intToBytes(0x7af98bb4),
            "adnl.message.query",
            "adnl.Message",
            mapOf("query_id", "bytes", "query", "bytes")));
    schemas.add(
        new TLGenerator.TLSchema(
            intToBytes(0x4c2d4977),
            "adnl.message.answer",
            "adnl.Message",
            mapOf("query_id", "bytes", "answer", "bytes")));

    return new TLGenerator.TLSchemas(schemas);
  }

  private static byte[] intToBytes(int value) {
    return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
  }

  private static <K, V> java.util.Map<K, V> mapOf(Object... keyValues) {
    java.util.Map<K, V> map = new java.util.HashMap<>();
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
