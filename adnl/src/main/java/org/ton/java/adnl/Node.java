package org.ton.java.adnl;

import com.iwebpp.crypto.TweetNaclFast;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Node class for ADNL protocol
 */
public class Node {
    private static final Logger logger = Logger.getLogger(Node.class.getName());
    
    private final String host;
    private final int port;
    private final byte[] ed25519Public;
    private final byte[] x25519Public;
    private final byte[] keyId;
    private final List<AdnlChannel> channels = new ArrayList<>();
    private final AdnlTransport transport;
    
    private int seqno = 0;
    private int confirmSeqno = 0;
    private boolean connected = false;
    private ScheduledExecutorService pingExecutor;
    
    /**
     * Create a node with the specified host, port, and public key
     * @param host Host
     * @param port Port
     * @param base64PublicKey Base64-encoded public key
     * @param transport ADNL transport
     */
    public Node(String host, int port, String base64PublicKey, AdnlTransport transport) {
        this.host = host;
        this.port = port;
        this.transport = transport;
        
        byte[] publicKey = java.util.Base64.getDecoder().decode(base64PublicKey);
        this.ed25519Public = publicKey;
        
        // Convert Ed25519 public key to X25519 public key
        try {
            this.x25519Public = TweetNaclFast.convertEd25519PublicToX25519(ed25519Public);
        } catch (Exception e) {
            throw new RuntimeException("Error converting Ed25519 public key to X25519", e);
        }
        
        // Calculate key ID
        this.keyId = getKeyId(ed25519Public);
    }
    
    /**
     * Get host
     * @return Host
     */
    public String getHost() {
        return host;
    }
    
    /**
     * Get port
     * @return Port
     */
    public int getPort() {
        return port;
    }
    
    /**
     * Get Ed25519 public key
     * @return Ed25519 public key
     */
    public byte[] getEd25519Public() {
        return ed25519Public;
    }
    
    /**
     * Get X25519 public key
     * @return X25519 public key
     */
    public byte[] getX25519Public() {
        return x25519Public;
    }
    
    /**
     * Get key ID
     * @return Key ID
     */
    public byte[] getKeyId() {
        return keyId;
    }
    
    /**
     * Get channels
     * @return Channels
     */
    public List<AdnlChannel> getChannels() {
        return channels;
    }
    
    /**
     * Get seqno
     * @return Seqno
     */
    public int getSeqno() {
        return seqno;
    }
    
    /**
     * Increment seqno
     */
    public void incrementSeqno() {
        seqno++;
    }
    
    /**
     * Get confirm seqno
     * @return Confirm seqno
     */
    public int getConfirmSeqno() {
        return confirmSeqno;
    }
    
    /**
     * Set confirm seqno
     * @param confirmSeqno Confirm seqno
     */
    public void setConfirmSeqno(int confirmSeqno) {
        this.confirmSeqno = confirmSeqno;
    }
    
    /**
     * Check if node is connected
     * @return True if connected
     */
    public boolean isConnected() {
        return connected;
    }
    
    /**
     * Set connected status
     * @param connected Connected status
     */
    public void setConnected(boolean connected) {
        this.connected = connected;
    }
    
    /**
     * Start ping
     */
    public void startPing() {
        if (pingExecutor != null) {
            pingExecutor.shutdown();
        }
        
        pingExecutor = Executors.newSingleThreadScheduledExecutor();
        pingExecutor.scheduleAtFixedRate(() -> {
            try {
                if (connected) {
                    ping();
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error pinging node", e);
            }
        }, 10, 10, TimeUnit.SECONDS);
    }
    
    /**
     * Stop ping
     */
    public void stopPing() {
        if (pingExecutor != null) {
            pingExecutor.shutdown();
            pingExecutor = null;
        }
    }
    
    /**
     * Ping node
     */
    private void ping() {
        try {
            transport.sendQueryMessage("dht.ping", createPingData(), this);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error sending ping", e);
        }
    }
    
    /**
     * Create ping data
     * @return Ping data
     */
    private java.util.Map<String, Object> createPingData() {
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("random_id", CryptoUtils.getRandomBytes(8));
        return data;
    }
    
    /**
     * Calculate key ID from public key
     * @param publicKey Public key
     * @return Key ID
     */
    private static byte[] getKeyId(byte[] publicKey) {
        try {
            java.security.MessageDigest sha256 = java.security.MessageDigest.getInstance("SHA-256");
            return sha256.digest(publicKey);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 error", e);
        }
    }
}
