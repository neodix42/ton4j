package org.ton.java.adnl;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;
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
  private byte[] authKey;
  private CompletableFuture<Void> authFuture;

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

    // Authentication is optional for liteservers
    if (authKey != null) {
      authenticate(authKey);
    }
    // Note: Most liteservers don't require authentication for basic queries
  }

  private void performHandshake(byte[] serverPublicKey) throws Exception {
    logger.fine("Performing ADNL handshake");

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

    logger.info("Server key ID: " + CryptoUtils.hex(serverKeyId));
    logger.info("Client public key: " + CryptoUtils.hex(clientPublicKey));

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

    logger.info("Handshake packet sent with serverKeyId: " + CryptoUtils.hex(serverKeyId));
  }

  private byte[] calculateKeyId(byte[] publicKey) throws Exception {
    // Calculate TL constructor ID for pub.ed25519 schema
    String tlSchema = "pub.ed25519 key:int256 = PublicKey";
    java.util.zip.CRC32 crc32 = new java.util.zip.CRC32();
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
    logger.fine("Starting packet listener");

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

        logger.info("Decrypted packet size: " + packetSizeLong);

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
        logger.info("Received handshake confirmation (empty packet)");
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
        logger.warning("Received checksum: " + CryptoUtils.hex(receivedChecksum));
        logger.warning("Calculated checksum: " + CryptoUtils.hex(calculatedChecksum));
        return;
      }

      // Handle handshake confirmation (empty payload)
      if (payload.length == 0) {
        connected = true;
        logger.info("Received handshake confirmation (empty packet)");
        return;
      }

      // Log payload for debugging
      logger.info("Received payload of size: " + payload.length + " bytes");
      logger.info("Payload hex: " + CryptoUtils.hex(payload));

      // First try to parse as adnl.message.answer
      try {
        Object[] deserialized = schemas.deserialize(payload);

        logger.info(
            "Deserialized result type: "
                + (deserialized[0] != null ? deserialized[0].getClass().getName() : "null"));

        if (deserialized[0] instanceof java.util.Map) {
          @SuppressWarnings("unchecked")
          java.util.Map<String, Object> message = (java.util.Map<String, Object>) deserialized[0];
          logger.info("Deserialized message type: " + message.get("@type"));

          if ("adnl.message.answer".equals(message.get("@type"))) {
            // Process ADNL answer message
            byte[] queryId = (byte[]) message.get("query_id");
            String queryIdHex = CryptoUtils.hex(queryId);
            Object answer = message.get("answer");

            logger.info("Received answer for query ID: " + queryIdHex);
            logger.info("Answer type: " + (answer != null ? answer.getClass().getName() : "null"));
            if (answer instanceof byte[]) {
              logger.info("Answer size: " + ((byte[]) answer).length + " bytes");
              logger.info("Answer hex: " + CryptoUtils.hex((byte[]) answer));

              // Try to deserialize the answer
              try {
                Object[] answerDeserialized = schemas.deserialize((byte[]) answer);
                logger.info(
                    "Answer deserialized type: "
                        + (answerDeserialized[0] != null
                            ? answerDeserialized[0].getClass().getName()
                            : "null"));
                if (answerDeserialized[0] instanceof java.util.Map) {
                  @SuppressWarnings("unchecked")
                  java.util.Map<String, Object> answerMap =
                      (java.util.Map<String, Object>) answerDeserialized[0];
                  logger.info("Answer message type: " + answerMap.get("@type"));

                  // Complete the future with the deserialized answer map
                  CompletableFuture<Object> future = activeQueries.remove(queryIdHex);
                  if (future != null) {
                    logger.info(
                        "Completing future for query ID: "
                            + queryIdHex
                            + " with deserialized answer");
                    future.complete(answerMap);
                    return;
                  }
                }
              } catch (Exception e) {
                logger.info("Could not deserialize answer: " + e.getMessage());
                // Even if we can't deserialize, we should still complete the future with the raw
                // bytes
                CompletableFuture<Object> future = activeQueries.remove(queryIdHex);
                if (future != null) {
                  logger.info(
                      "Completing future for query ID: " + queryIdHex + " with raw answer bytes");
                  future.complete(answer);
                  return;
                }
              }
            }

            CompletableFuture<Object> future = activeQueries.remove(queryIdHex);
            if (future != null) {
              logger.info("Completing future for query ID: " + queryIdHex);
              future.complete(answer);
              return;
            } else {
              logger.warning("No active query found for ID: " + queryIdHex);
            }
          } else {
            // Process other message types
            processMessage(message);
            return;
          }
        }
      } catch (Exception e) {
        logger.log(Level.INFO, "Error parsing as ADNL message: " + e.getMessage(), e);
      }

      // Special handling for liteServer responses
      // This is a direct response to our query
      if (payload.length >= 4) {
        try {
          // Check for known constructor IDs
          byte[] constructorId = Arrays.copyOfRange(payload, 0, 4);
          logger.info("Checking constructor ID: " + CryptoUtils.hex(constructorId));

          // Check for liteServer.masterchainInfo (constructor ID: 0x81288385)
          if (Arrays.equals(
              constructorId, new byte[] {(byte) 0x85, (byte) 0x83, (byte) 0x28, (byte) 0x81})) {
            logger.info("Detected liteServer.masterchainInfo response by constructor ID");

            // Find any active getMasterchainInfo query and complete it
            if (!activeQueries.isEmpty()) {
              String queryId = activeQueries.keySet().iterator().next();
              CompletableFuture<Object> future = activeQueries.remove(queryId);
              if (future != null) {
                // Try to deserialize as liteServer.masterchainInfo
                try {
                  Map<String, Object> result = schemas.deserializeToMap(payload);
                  logger.info("Successfully deserialized liteServer.masterchainInfo response");
                  future.complete(result);
                } catch (Exception e) {
                  logger.info(
                      "Could not deserialize as liteServer.masterchainInfo, completing with raw bytes: "
                          + e.getMessage());
                  future.complete(payload);
                }
                return;
              }
            }
          }

          // Try to find a schema for this constructor ID
          TLGenerator.TLSchema schema = schemas.getById(constructorId, ByteOrder.LITTLE_ENDIAN);
          if (schema != null) {
            logger.info(
                "Found schema for constructor ID: "
                    + CryptoUtils.hex(constructorId)
                    + " - "
                    + schema.getName());

            // Find any active query and complete it
            if (!activeQueries.isEmpty()) {
              String queryId = activeQueries.keySet().iterator().next();
              CompletableFuture<Object> future = activeQueries.remove(queryId);
              if (future != null) {
                // Try to deserialize using the found schema
                try {
                  Map<String, Object> result = schemas.deserializeToMap(payload);
                  logger.info("Successfully deserialized response as " + schema.getName());
                  future.complete(result);
                } catch (Exception e) {
                  logger.info(
                      "Could not deserialize as "
                          + schema.getName()
                          + ", completing with raw bytes: "
                          + e.getMessage());
                  future.complete(payload);
                }
                return;
              }
            }
          }
        } catch (Exception e) {
          logger.log(Level.INFO, "Error checking for TL response: " + e.getMessage(), e);
        }
      }

      // If we get here, try to handle as a raw response to a query
      if (!activeQueries.isEmpty()) {
        // Find the first active query and complete it with the payload
        String firstQueryId = activeQueries.keySet().iterator().next();
        CompletableFuture<Object> future = activeQueries.remove(firstQueryId);
        if (future != null) {
          logger.info("Completing query ID: " + firstQueryId + " with raw payload");

          // Try to deserialize the raw payload before completing the future
          try {
            Object[] deserialized = schemas.deserialize(payload);
            if (deserialized[0] != null) {
              logger.info("Successfully deserialized raw payload response");
              future.complete(deserialized[0]);
            } else {
              logger.info("Could not deserialize as TL object, completing with raw bytes");
              future.complete(payload);
            }
          } catch (Exception e) {
            logger.info("Could not deserialize raw payload, using as-is: " + e.getMessage());
            future.complete(payload);
          }
          return;
        }
      }

      logger.warning("Received unhandled payload: " + CryptoUtils.hex(payload));

    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error processing incoming packet", e);
      e.printStackTrace();
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
    try {
      logger.info("Received authentication nonce from server");

      byte[] serverNonce = (byte[]) message.get("nonce");
      logger.info("Server nonce: " + CryptoUtils.hex(serverNonce));

      // Concatenate our nonce with server nonce
      byte[] combinedNonce = new byte[ourNonce.length + serverNonce.length];
      System.arraycopy(ourNonce, 0, combinedNonce, 0, ourNonce.length);
      System.arraycopy(serverNonce, 0, combinedNonce, ourNonce.length, serverNonce.length);

      // Sign the combined nonce with our authentication key
      byte[] signature = CryptoUtils.sign(authKey, combinedNonce);

      // Get our public key for authentication
      byte[] publicKey = CryptoUtils.getPublicKey(authKey);

      // Create authentication complete message
      java.util.Map<String, Object> authComplete = new java.util.HashMap<>();
      authComplete.put("@type", "tcp.authentificationComplete");
      authComplete.put("key", publicKey);
      authComplete.put("signature", signature);

      byte[] serialized = schemas.serialize("tcp.authentificationComplete", authComplete, true);
      sendPacket(serialized);

      logger.info("Sent authentication complete");

      // Complete the authentication future
      if (authFuture != null) {
        authFuture.complete(null);
      }

    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error handling auth nonce", e);
      if (authFuture != null) {
        authFuture.completeExceptionally(e);
      }
    }
  }

  private void handleAnswer(java.util.Map<String, Object> message) {
    byte[] queryId = (byte[]) message.get("query_id");
    String queryIdHex = CryptoUtils.hex(queryId);
    Object answer = message.get("answer");

    logger.fine("Received answer for query ID: " + queryIdHex);

    CompletableFuture<Object> future = activeQueries.remove(queryIdHex);
    if (future != null) {
      logger.fine("Completing future for query ID: " + queryIdHex);
      future.complete(answer);
    } else {
      logger.warning("No active query found for ID: " + queryIdHex);
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
    logger.info("packet size: " + totalSize);

    // Generate nonce (32 bytes)
    byte[] nonce =
        CryptoUtils.hexToBytes("5fb13e11977cb5cff0fbf7f23f674d734cb7c4bf01322c5e6b928c5d8ea09cfd");
    //    byte[] nonce = new byte[32];
    //    new SecureRandom().nextBytes(nonce);
    packet.put(nonce);
    logger.info("packet nonce: " + CryptoUtils.hex(nonce));
    // Payload
    packet.put(payload);
    logger.info("packet payload: " + CryptoUtils.hex(payload));

    // Calculate checksum from nonce + payload (matching Go implementation)
    MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
    sha256.update(nonce);
    sha256.update(payload);
    byte[] checksum = sha256.digest();
    logger.info("packet checksum: " + CryptoUtils.hex(checksum));
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
      if (!connected || socket == null || socket.isClosed()) {
        throw new IllegalStateException("Not connected or socket closed");
      }

      // Generate query ID
      // byte[] queryId = new byte[32]; was random
      byte[] queryId =
          CryptoUtils.hexToBytes(
              "77c1545b96fa136b8e01cc08338bec47e8a43215492dda6d4d7e286382bb00c4");
      //      new SecureRandom().nextBytes(queryId);

      logger.info("Sending query with ID: " + CryptoUtils.hex(queryId));
      //      logger.info("Query type: " + query.getClass().getName());
      if (query instanceof byte[]) {
        logger.info("Query size: " + ((byte[]) query).length + " bytes");

        // Log the hex representation of the query for debugging
        logger.info("Query hex: " + CryptoUtils.hex((byte[]) query));
      }

      // Wrap in ADNL query
      java.util.Map<String, Object> adnlQuery = new java.util.HashMap<>();
      adnlQuery.put("@type", "adnl.message.query");
      adnlQuery.put("query_id", queryId);
      adnlQuery.put("query", query);

      byte[] serialized = schemas.serialize("adnl.message.query", adnlQuery, true);
      logger.info("Serialized ADNL query size: " + serialized.length + " bytes");
      logger.info("Serialized ADNL query hex: " + CryptoUtils.hex(serialized));

      // Calculate the total packet size for verification
      int totalSize = 32 + serialized.length + 32; // nonce + payload + checksum
      logger.info("Total packet size will be: " + (4 + totalSize) + " bytes");

      CompletableFuture<Object> future = new CompletableFuture<>();
      String queryIdHex = CryptoUtils.hex(queryId);
      activeQueries.put(queryIdHex, future);
      logger.info("Added query to active queries with ID: " + queryIdHex);

      // Send the packet before setting up the timeout to ensure it's sent
      try {
        sendPacket(serialized);
        logger.info("Query packet sent successfully");
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
              logger.warning("Query timed out: " + queryIdHex);
              future.completeExceptionally(new Exception("Query timeout"));

              // Check if we need to reconnect
              if (connected && (socket == null || socket.isClosed())) {
                logger.warning("Socket closed during query, marking as disconnected");
                connected = false;
              }
            }
          },
          60,
          TimeUnit.SECONDS);

      return future;
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error sending query", e);
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

    logger.info("Starting authentication with nonce: " + CryptoUtils.hex(ourNonce));

    // Send authentication request
    java.util.Map<String, Object> authRequest = new java.util.HashMap<>();
    authRequest.put("@type", "tcp.authentificate");
    authRequest.put("nonce", ourNonce);

    byte[] serialized = schemas.serialize("tcp.authentificate", authRequest, true);
    sendPacket(serialized);

    // Wait for authentication to complete
    authFuture.get(10, TimeUnit.SECONDS);
    authenticated = true;
    logger.info("Authentication completed successfully");
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

    // Liteserver schemas
    schemas.add(
        new TLGenerator.TLSchema(
            intToBytes(0xdf068c79), "liteServer.query", "Object", mapOf("data", "bytes")));
    schemas.add(
        new TLGenerator.TLSchema(
            intToBytes(0x2ee6b589),
            "liteServer.getMasterchainInfo",
            "liteServer.MasterchainInfo",
            new java.util.HashMap<>()));
    schemas.add(
        new TLGenerator.TLSchema(
            intToBytes(0x81288385),
            "liteServer.masterchainInfo",
            "liteServer.MasterchainInfo",
            mapOf(
                "last", "tonNode.blockIdExt",
                "state_root_hash", "int256",
                "init", "tonNode.zeroStateIdExt")));
    schemas.add(
        new TLGenerator.TLSchema(
            intToBytes(0x9b895a00),
            "tonNode.blockIdExt",
            "tonNode.BlockIdExt",
            mapOf(
                "workchain", "int",
                "shard", "long",
                "seqno", "int",
                "root_hash", "int256",
                "file_hash", "int256")));
    schemas.add(
        new TLGenerator.TLSchema(
            intToBytes(0x8b895a00),
            "tonNode.zeroStateIdExt",
            "tonNode.ZeroStateIdExt",
            mapOf("workchain", "int", "root_hash", "int256", "file_hash", "int256")));

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
