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
 * TCP-based ADNL transport implementation for liteserver communication
 * Based on the ADNL-TCP specification and Go reference implementation
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
    private final ConcurrentHashMap<String, CompletableFuture<Object>> activeQueries = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, CompletableFuture<TcpPong>> activePings = new ConcurrentHashMap<>();
    private final ScheduledExecutorService timeoutExecutor = Executors.newScheduledThreadPool(2);
    
    private Thread listenerThread;
    private boolean authenticated = false;
    private byte[] ourNonce;
    
    /**
     * Create TCP transport with generated client key
     */
    public AdnlTcpTransport() {
        this.client = Client.generate();
        this.schemas = createTcpSchemas();
    }
    
    /**
     * Create TCP transport with specific client key
     * @param client Client with keys
     */
    public AdnlTcpTransport(Client client) {
        this.client = client;
        this.schemas = createTcpSchemas();
    }
    
    /**
     * Connect to liteserver
     * @param host Server host
     * @param port Server port
     * @param serverPublicKey Server's Ed25519 public key (32 bytes)
     * @throws Exception if connection fails
     */
    public void connect(String host, int port, byte[] serverPublicKey) throws Exception {
        connect(host, port, serverPublicKey, null);
    }
    
    /**
     * Connect to liteserver with authentication
     * @param host Server host
     * @param port Server port
     * @param serverPublicKey Server's Ed25519 public key (32 bytes)
     * @param authKey Authentication key (can be null for liteserver)
     * @throws Exception if connection fails
     */
    public void connect(String host, int port, byte[] serverPublicKey, byte[] authKey) throws Exception {
        logger.info("Connecting to " + host + ":" + port);
        
        // Create TCP connection
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 10000);
        socket.setSoTimeout(30000);
        
        input = new DataInputStream(socket.getInputStream());
        output = new DataOutputStream(socket.getOutputStream());
        
        // Perform handshake
        performHandshake(serverPublicKey);
        
        // Start listener thread
        startListener();
        
        // Wait for handshake confirmation (empty packet)
        waitForHandshakeConfirmation();
        
        connected = true;
        logger.info("Connected successfully");
        
        // Authenticate if auth key provided
        if (authKey != null) {
            authenticate(authKey);
        }
    }
    
    /**
     * Perform ADNL-TCP handshake
     * @param serverPublicKey Server's public key
     * @throws Exception if handshake fails
     */
    private void performHandshake(byte[] serverPublicKey) throws Exception {
        logger.fine("Performing ADNL handshake");
        
        // Generate 160 random bytes for cipher construction
        byte[] randomData = new byte[160];
        new SecureRandom().nextBytes(randomData);
        
        // Build read and write ciphers from random data
        readCipher = CryptoUtils.createAESCtrCipher(
            Arrays.copyOfRange(randomData, 0, 32),
            Arrays.copyOfRange(randomData, 64, 80),
            Cipher.DECRYPT_MODE
        );
        
        writeCipher = CryptoUtils.createAESCtrCipher(
            Arrays.copyOfRange(randomData, 32, 64),
            Arrays.copyOfRange(randomData, 80, 96),
            Cipher.ENCRYPT_MODE
        );
        
        // Calculate server key ID: SHA256 of TL schema + public key
        byte[] serverKeyId = calculateKeyId(serverPublicKey);
        
        // Get client public key
        byte[] clientPublicKey = client.getEd25519Public();
        
        // Calculate checksum of 160 bytes
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] checksum = sha256.digest(randomData);
        
        // Derive encryption key and IV from ECDH shared key and checksum
        byte[] sharedKey = CryptoUtils.getSharedKey(client.getX25519Private(), 
                                                   CryptoUtils.convertEd25519ToX25519Public(serverPublicKey));
        
        // Build encryption key: sharedKey[0:16] + checksum[16:32]
        byte[] encKey = new byte[32];
        System.arraycopy(sharedKey, 0, encKey, 0, 16);
        System.arraycopy(checksum, 16, encKey, 16, 16);
        
        // Build IV: checksum[0:4] + sharedKey[20:32]
        byte[] iv = new byte[16];
        System.arraycopy(checksum, 0, iv, 0, 4);
        System.arraycopy(sharedKey, 20, iv, 4, 12);
        
        // Encrypt 160 bytes
        Cipher handshakeCipher = CryptoUtils.createAESCtrCipher(encKey, iv, Cipher.ENCRYPT_MODE);
        byte[] encrypted = CryptoUtils.aesCtrTransform(handshakeCipher, randomData);
        
        // Build handshake packet: [server_key_id:32][client_pub:32][checksum:32][encrypted:160]
        ByteBuffer handshakePacket = ByteBuffer.allocate(256);
        handshakePacket.put(serverKeyId);
        handshakePacket.put(clientPublicKey);
        handshakePacket.put(checksum);
        handshakePacket.put(encrypted);
        
        // Send handshake packet
        output.write(handshakePacket.array());
        output.flush();
        
        logger.fine("Handshake packet sent");
    }
    
    /**
     * Calculate key ID from Ed25519 public key
     * @param publicKey Ed25519 public key
     * @return Key ID (SHA256 hash)
     */
    private byte[] calculateKeyId(byte[] publicKey) throws Exception {
        // Key ID = SHA256([0xC6, 0xB4, 0x13, 0x48] + public_key) for Ed25519
        ByteBuffer buffer = ByteBuffer.allocate(4 + publicKey.length);
        buffer.put(new byte[]{(byte) 0xC6, (byte) 0xB4, (byte) 0x13, (byte) 0x48});
        buffer.put(publicKey);
        
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        return sha256.digest(buffer.array());
    }
    
    /**
     * Start listener thread for incoming packets
     */
    private void startListener() {
        running = true;
        listenerThread = new Thread(this::listenForPackets);
        listenerThread.setDaemon(true);
        listenerThread.start();
    }
    
    /**
     * Wait for handshake confirmation (empty packet)
     */
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
    
    /**
     * Listen for incoming packets
     */
    private void listenForPackets() {
        logger.fine("Starting packet listener");
        
        try {
            while (running && !socket.isClosed()) {
                // Read packet size (4 bytes, little endian)
                byte[] sizeBytes = new byte[4];
                input.readFully(sizeBytes);
                
                // Decrypt size
                readCipher.update(sizeBytes, 0, 4, sizeBytes, 0);
                int packetSize = ByteBuffer.wrap(sizeBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
                
                if (packetSize > 10 * 1024 * 1024) { // 10MB limit
                    throw new IOException("Packet too large: " + packetSize);
                }
                
                // Read packet data
                byte[] packetData = new byte[packetSize];
                input.readFully(packetData);
                
                // Decrypt packet data
                readCipher.update(packetData, 0, packetSize, packetData, 0);
                
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
     * Process incoming packet
     * @param packetData Decrypted packet data
     */
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
            byte[] receivedChecksum = Arrays.copyOfRange(packetData, packetData.length - 32, packetData.length);
            
            // Verify checksum
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            sha256.update(nonce);
            sha256.update(payload);
            byte[] calculatedChecksum = sha256.digest();
            
            if (!Arrays.equals(receivedChecksum, calculatedChecksum)) {
                logger.warning("Invalid packet checksum");
                return;
            }
            
            if (payload.length == 0) {
                // Empty payload after handshake
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
    
    /**
     * Process incoming message
     * @param message Deserialized message
     */
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
    
    /**
     * Handle pong message
     */
    private void handlePong(java.util.Map<String, Object> message) {
        Long randomId = (Long) message.get("random_id");
        CompletableFuture<TcpPong> future = activePings.remove(randomId);
        if (future != null) {
            future.complete(new TcpPong(randomId));
        }
    }
    
    /**
     * Handle authentication nonce
     */
    private void handleAuthNonce(java.util.Map<String, Object> message) {
        // Will be implemented when authentication is needed
        logger.fine("Received auth nonce");
    }
    
    /**
     * Handle answer message
     */
    private void handleAnswer(java.util.Map<String, Object> message) {
        byte[] queryId = (byte[]) message.get("query_id");
        String queryIdHex = CryptoUtils.hex(queryId);
        
        CompletableFuture<Object> future = activeQueries.remove(queryIdHex);
        if (future != null) {
            future.complete(message.get("answer"));
        }
    }
    
    /**
     * Send packet
     * @param payload Payload data
     * @throws Exception if send fails
     */
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
    
    /**
     * Send ping
     * @return Future that completes when pong is received
     */
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
            timeoutExecutor.schedule(() -> {
                if (activePings.remove(randomId) != null) {
                    future.completeExceptionally(new Exception("Ping timeout"));
                }
            }, 5, TimeUnit.SECONDS);
            
            return future;
        } catch (Exception e) {
            CompletableFuture<TcpPong> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
    
    /**
     * Send query and wait for response
     * @param query Query data
     * @return Future that completes with response
     */
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
            timeoutExecutor.schedule(() -> {
                if (activeQueries.remove(CryptoUtils.hex(queryId)) != null) {
                    future.completeExceptionally(new Exception("Query timeout"));
                }
            }, 30, TimeUnit.SECONDS);
            
            return future;
        } catch (Exception e) {
            CompletableFuture<Object> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
    
    /**
     * Authenticate with server (for services that require it)
     * @param authKey Authentication private key
     * @throws Exception if authentication fails
     */
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
    
    /**
     * Close connection
     */
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
    
    /**
     * Check if connected
     * @return true if connected
     */
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }
    
    /**
     * Create TL schemas for TCP protocol
     * @return TLSchemas with TCP and ADNL schemas
     */
    private static TLGenerator.TLSchemas createTcpSchemas() {
        java.util.List<TLGenerator.TLSchema> schemas = new java.util.ArrayList<>();
        
        // TCP protocol schemas
        schemas.add(new TLGenerator.TLSchema(intToBytes(0x9a2b084d), "tcp.ping", "tcp.Pong", 
                mapOf("random_id", "long")));
        schemas.add(new TLGenerator.TLSchema(intToBytes(0x8b9c0a0e), "tcp.pong", "tcp.Ping", 
                mapOf("random_id", "long")));
        schemas.add(new TLGenerator.TLSchema(intToBytes(0x2d691b5f), "tcp.authentificate", "tcp.Message", 
                mapOf("nonce", "bytes")));
        schemas.add(new TLGenerator.TLSchema(intToBytes(0x5c6b1c0d), "tcp.authentificationNonce", "tcp.Message", 
                mapOf("nonce", "bytes")));
        schemas.add(new TLGenerator.TLSchema(intToBytes(0x4a7b2e1f), "tcp.authentificationComplete", "tcp.Message", 
                mapOf("key", "bytes", "signature", "bytes")));
        
        // ADNL message schemas
        schemas.add(new TLGenerator.TLSchema(intToBytes(0x7af98bb4), "adnl.message.query", "adnl.Message", 
                mapOf("query_id", "bytes", "query", "bytes")));
        schemas.add(new TLGenerator.TLSchema(intToBytes(0x4c2d4977), "adnl.message.answer", "adnl.Message", 
                mapOf("query_id", "bytes", "answer", "bytes")));
        
        return new TLGenerator.TLSchemas(schemas);
    }
    
    /**
     * Convert int to bytes (little endian)
     */
    private static byte[] intToBytes(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }
    
    /**
     * Create map from key-value pairs
     */
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
    
    /**
     * TCP Pong message class
     */
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
