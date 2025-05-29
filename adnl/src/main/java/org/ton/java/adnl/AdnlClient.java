package org.ton.java.adnl;

import org.ton.java.adnl.message.*;
import org.ton.java.adnl.packet.*;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * ADNL (Abstract Datagram Network Layer) client implementation
 * Mirrors the Go implementation functionality
 */
public class AdnlClient implements AutoCloseable {
    
    // Constants
    public static final int BASE_PAYLOAD_MTU = 1024;
    public static final int HUGE_PACKET_MAX_SIZE = 1024 * 8 + 128;
    public static final int MAX_MTU = 1500 - 40 - 8; // IPv6 over Ethernet
    
    // Connection
    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private final String address;
    private final int port;
    
    // Keys
    private final byte[] ourPrivateKey;
    private final byte[] ourPublicKey;
    private byte[] peerPublicKey;
    
    // State
    private final AtomicLong seqno = new AtomicLong(0);
    private final AtomicLong confirmSeqno = new AtomicLong(0);
    private final AtomicInteger reinitTime = new AtomicInteger((int) Instant.now().getEpochSecond());
    private final AtomicInteger dstReinit = new AtomicInteger(0);
    private volatile boolean connected = false;
    private volatile boolean closed = false;
    
    // Message handling
    private final Map<String, PartitionedMessage> messageParts = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Object>> activeQueries = new ConcurrentHashMap<>();
    private final Map<Long, CompletableFuture<MessagePong>> activePings = new ConcurrentHashMap<>();
    
    // Handlers
    private Consumer<MessageCustom> customMessageHandler;
    private Consumer<MessageQuery> queryHandler;
    private Runnable disconnectHandler;
    
    // Threading
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private CompletableFuture<Void> readerTask;
    
    public AdnlClient(String address, int port, byte[] ourPrivateKey, byte[] peerPublicKey) {
        this.address = address;
        this.port = port;
        this.ourPrivateKey = ourPrivateKey;
        this.ourPublicKey = CryptoUtils.getPublicKey(ourPrivateKey);
        this.peerPublicKey = peerPublicKey;
    }
    
    /**
     * Connect to the ADNL server
     */
    public void connect() throws IOException {
        if (connected) {
            return;
        }
        
        socket = new Socket(address, port);
        socket.setTcpNoDelay(true);
        socket.setSoTimeout(30000); // 30 second timeout
        
        inputStream = new DataInputStream(socket.getInputStream());
        outputStream = new DataOutputStream(socket.getOutputStream());
        
        connected = true;
        
        // Start reader thread
        readerTask = CompletableFuture.runAsync(this::readerLoop, executor);
    }
    
    /**
     * Send a ping message and measure round-trip time
     */
    public long ping(long timeoutMs) throws Exception {
        if (!connected) {
            throw new IllegalStateException("Not connected");
        }
        
        long value = System.nanoTime();
        MessagePing ping = new MessagePing(value);
        
        CompletableFuture<MessagePong> future = new CompletableFuture<>();
        activePings.put(value, future);
        
        try {
            byte[] packet = buildRequest(ping);
            send(packet);
            
            MessagePong pong = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            return System.nanoTime() - value;
        } finally {
            activePings.remove(value);
        }
    }
    
    /**
     * Send a query and wait for response
     */
    public <T> T query(Object request, Class<T> responseType, long timeoutMs) throws Exception {
        if (!connected) {
            throw new IllegalStateException("Not connected");
        }
        
        MessageQuery query = createQueryMessage(request);
        String queryId = bytesToHex(query.getId());
        
        CompletableFuture<Object> future = new CompletableFuture<>();
        activeQueries.put(queryId, future);
        
        try {
            List<byte[]> packets = buildRequestMaySplit(query, false);
            for (byte[] packet : packets) {
                send(packet);
            }
            
            Object response = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            return responseType.cast(response);
        } finally {
            activeQueries.remove(queryId);
        }
    }
    
    /**
     * Send a custom message
     */
    public void sendCustomMessage(Object message) throws Exception {
        if (!connected) {
            throw new IllegalStateException("Not connected");
        }
        
        MessageCustom custom = new MessageCustom(message);
        List<byte[]> packets = buildRequestMaySplit(custom, false);
        
        for (byte[] packet : packets) {
            send(packet);
        }
    }
    
    /**
     * Set custom message handler
     */
    public void setCustomMessageHandler(Consumer<MessageCustom> handler) {
        this.customMessageHandler = handler;
    }
    
    /**
     * Set query handler
     */
    public void setQueryHandler(Consumer<MessageQuery> handler) {
        this.queryHandler = handler;
    }
    
    /**
     * Set disconnect handler
     */
    public void setDisconnectHandler(Runnable handler) {
        this.disconnectHandler = handler;
    }
    
    /**
     * Close the connection
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        
        closed = true;
        connected = false;
        
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            // Ignore
        }
        
        if (readerTask != null) {
            readerTask.cancel(true);
        }
        
        executor.shutdown();
        
        if (disconnectHandler != null) {
            disconnectHandler.run();
        }
    }
    
    /**
     * Reader loop for incoming packets
     */
    private void readerLoop() {
        byte[] buffer = new byte[MAX_MTU];
        
        try {
            while (connected && !closed) {
                // Read packet length (4 bytes)
                int length = inputStream.readInt();
                if (length <= 0 || length > MAX_MTU) {
                    throw new IOException("Invalid packet length: " + length);
                }
                
                // Read packet data
                inputStream.readFully(buffer, 0, length);
                byte[] packet = Arrays.copyOf(buffer, length);
                
                // Process packet
                processIncomingPacket(packet);
            }
        } catch (Exception e) {
            if (connected && !closed) {
                System.err.println("Reader loop error: " + e.getMessage());
                close();
            }
        }
    }
    
    /**
     * Process incoming packet
     */
    private void processIncomingPacket(byte[] packet) {
        try {
            byte[] data = CryptoUtils.decodePacket(ourPrivateKey, packet);
            PacketContent packetContent = parsePacket(data);
            processPacket(packetContent);
        } catch (Exception e) {
            System.err.println("Error processing packet: " + e.getMessage());
        }
    }
    
    /**
     * Process packet content
     */
    private void processPacket(PacketContent packet) throws Exception {
        // Update sequence numbers
        if (packet.getSeqno() != null) {
            long seqno = packet.getSeqno();
            confirmSeqno.updateAndGet(current -> Math.max(current, seqno));
        }
        
        // Process messages
        if (packet.getMessages() != null) {
            for (Object message : packet.getMessages()) {
                processMessage(message);
            }
        }
    }
    
    /**
     * Process individual message
     */
    private void processMessage(Object message) throws Exception {
        if (message instanceof MessagePong) {
            MessagePong pong = (MessagePong) message;
            CompletableFuture<MessagePong> future = activePings.get(pong.getValue());
            if (future != null) {
                future.complete(pong);
            }
        } else if (message instanceof MessagePing) {
            MessagePing ping = (MessagePing) message;
            MessagePong pong = new MessagePong(ping.getValue());
            byte[] packet = buildRequest(pong);
            send(packet);
        } else if (message instanceof MessageAnswer) {
            MessageAnswer answer = (MessageAnswer) message;
            String queryId = bytesToHex(answer.getId());
            CompletableFuture<Object> future = activeQueries.get(queryId);
            if (future != null) {
                future.complete(answer.getData());
            }
        } else if (message instanceof MessageQuery) {
            if (queryHandler != null) {
                queryHandler.accept((MessageQuery) message);
            }
        } else if (message instanceof MessageCustom) {
            if (customMessageHandler != null) {
                customMessageHandler.accept((MessageCustom) message);
            }
        } else if (message instanceof MessagePart) {
            processMessagePart((MessagePart) message);
        } else if (message instanceof MessageNop) {
            // No operation
        }
    }
    
    /**
     * Process message part for large messages
     */
    private void processMessagePart(MessagePart part) throws Exception {
        String msgId = bytesToHex(part.getHash());
        
        PartitionedMessage partitioned = messageParts.computeIfAbsent(msgId, 
            k -> new PartitionedMessage(part.getTotalSize()));
        
        boolean complete = partitioned.addPart(part.getOffset(), part.getData());
        
        if (complete) {
            messageParts.remove(msgId);
            byte[] data = partitioned.build(part.getHash());
            
            // Parse and process the complete message
            // This would need TL parsing implementation
            // For now, we'll treat it as a custom message
            MessageCustom custom = new MessageCustom(data);
            processMessage(custom);
        }
    }
    
    /**
     * Build request packet
     */
    private byte[] buildRequest(Object message) throws Exception {
        long seqno = this.seqno.incrementAndGet();
        return createPacket(seqno, false, message);
    }
    
    /**
     * Build request with potential splitting for large messages
     */
    private List<byte[]> buildRequestMaySplit(Object message, boolean useBaseMtu) throws Exception {
        // Serialize message (simplified - would need proper TL serialization)
        byte[] messageData = serializeMessage(message);
        
        int mtu = useBaseMtu ? BASE_PAYLOAD_MTU : (MAX_MTU - 32 - 64);
        
        if (messageData.length <= mtu) {
            return Collections.singletonList(buildRequest(message));
        }
        
        if (messageData.length > HUGE_PACKET_MAX_SIZE) {
            throw new IllegalArgumentException("Message too large");
        }
        
        // Split into parts
        List<MessagePart> parts = MessageUtils.splitMessage(messageData, mtu);
        List<byte[]> packets = new ArrayList<>();
        
        for (MessagePart part : parts) {
            packets.add(buildRequest(part));
        }
        
        return packets;
    }
    
    /**
     * Create ADNL packet
     */
    private byte[] createPacket(long seqno, boolean isResponse, Object... messages) throws Exception {
        if (peerPublicKey == null) {
            throw new IllegalStateException("Unknown peer");
        }
        
        // Create packet content
        PacketContent packet = new PacketContent();
        packet.setRand1(generateRandom(16));
        packet.setMessages(Arrays.asList(messages));
        packet.setSeqno(seqno);
        packet.setConfirmSeqno(confirmSeqno.get());
        packet.setReinitDate(reinitTime.get());
        packet.setDstReinitDate(dstReinit.get());
        packet.setRand2(generateRandom(16));
        
        if (!isResponse) {
            packet.setFrom(new PublicKeyED25519(ourPublicKey));
        } else {
            packet.setFromIdShort(CryptoUtils.hash(ourPublicKey));
        }
        
        // Serialize packet
        byte[] packetData = serializePacket(packet);
        
        // Sign packet
        byte[] signature = CryptoUtils.sign(ourPrivateKey, packetData);
        packet.setSignature(signature);
        
        // Re-serialize with signature
        packetData = serializePacket(packet);
        
        // Encrypt packet
        return CryptoUtils.encodePacket(ourPrivateKey, peerPublicKey, packetData);
    }
    
    /**
     * Send packet over the network
     */
    private void send(byte[] packet) throws IOException {
        if (!connected) {
            throw new IOException("Not connected");
        }
        
        if (packet.length > MAX_MTU) {
            throw new IOException("Packet too large");
        }
        
        synchronized (outputStream) {
            outputStream.writeInt(packet.length);
            outputStream.write(packet);
            outputStream.flush();
        }
    }
    
    /**
     * Parse incoming packet (simplified)
     */
    private PacketContent parsePacket(byte[] data) throws Exception {
        // This would need proper TL parsing implementation
        // For now, return a basic packet
        PacketContent packet = new PacketContent();
        packet.setMessages(new ArrayList<>());
        return packet;
    }
    
    /**
     * Serialize message (simplified)
     */
    private byte[] serializeMessage(Object message) throws Exception {
        // This would need proper TL serialization
        // For now, return empty bytes
        return new byte[0];
    }
    
    /**
     * Serialize packet (simplified)
     */
    private byte[] serializePacket(PacketContent packet) throws Exception {
        // This would need proper TL serialization
        // For now, return empty bytes
        return new byte[0];
    }
    
    /**
     * Create query message with random ID
     */
    private MessageQuery createQueryMessage(Object request) throws Exception {
        byte[] queryId = new byte[32];
        new SecureRandom().nextBytes(queryId);
        return new MessageQuery(queryId, request);
    }
    
    /**
     * Generate random bytes
     */
    private byte[] generateRandom(int length) {
        byte[] random = new byte[length];
        new SecureRandom().nextBytes(random);
        return random;
    }
    
    /**
     * Convert bytes to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    // Getters
    public boolean isConnected() {
        return connected;
    }
    
    public String getAddress() {
        return address;
    }
    
    public int getPort() {
        return port;
    }
    
    public byte[] getPeerPublicKey() {
        return peerPublicKey;
    }
}
