package org.ton.java.adnl;

import com.iwebpp.crypto.TweetNaclFast;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.params.X25519PublicKeyParameters;

import java.security.MessageDigest;

/**
 * Server class for ADNL protocol
 */
public class Server {
    private final String host;
    private final int port;
    private final byte[] ed25519Public;
    private final byte[] x25519Public;
    private final byte[] keyId;

    /**
     * Create a server with the specified host, port, and public key
     * @param host Host
     * @param port Port
     * @param ed25519Public Ed25519 public key
     */
    public Server(String host, int port, byte[] ed25519Public) {
        this.host = host;
        this.port = port;
        this.ed25519Public = ed25519Public;
        
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
     * Create a server from a base64-encoded public key
     * @param host Host
     * @param port Port
     * @param base64PublicKey Base64-encoded public key
     * @return Server instance
     */
    public static Server fromBase64(String host, int port, String base64PublicKey) {
        byte[] publicKey = java.util.Base64.getDecoder().decode(base64PublicKey);
        return new Server(host, port, publicKey);
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
     * Verify signature with Ed25519 public key
     * @param signature Signature
     * @param data Data
     * @return True if signature is valid
     */
    public boolean verify(byte[] signature, byte[] data) {
        TweetNaclFast.Signature verifier = new TweetNaclFast.Signature(ed25519Public, null);
        return verifier.detached_verify(data, signature);
    }
    
    /**
     * Calculate key ID from public key
     * @param publicKey Public key
     * @return Key ID
     */
    private static byte[] getKeyId(byte[] publicKey) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return sha256.digest(publicKey);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 error", e);
        }
    }
}
