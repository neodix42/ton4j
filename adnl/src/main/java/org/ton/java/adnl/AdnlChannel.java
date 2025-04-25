package org.ton.java.adnl;

import javax.crypto.Cipher;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * ADNL Channel class for encrypted communication between peers
 */
public class AdnlChannel {
    private final Client client;
    private final Server server;
    private final byte[] clientKeyId;
    private final byte[] serverKeyId;
    private final byte[] sharedKey;
    
    /**
     * Create an ADNL channel
     * @param client Client
     * @param server Server
     * @param clientKeyId Client key ID
     * @param serverKeyId Server key ID
     */
    public AdnlChannel(Client client, Server server, byte[] clientKeyId, byte[] serverKeyId) {
        this.client = client;
        this.server = server;
        this.clientKeyId = clientKeyId;
        this.serverKeyId = serverKeyId;
        this.sharedKey = CryptoUtils.getSharedKey(client.getX25519Private(), server.getX25519Public());
    }
    
    /**
     * Get client
     * @return Client
     */
    public Client getClient() {
        return client;
    }
    
    /**
     * Get server
     * @return Server
     */
    public Server getServer() {
        return server;
    }
    
    /**
     * Get client key ID
     * @return Client key ID
     */
    public byte[] getClientKeyId() {
        return clientKeyId;
    }
    
    /**
     * Get server key ID
     * @return Server key ID
     */
    public byte[] getServerKeyId() {
        return serverKeyId;
    }
    
    /**
     * Get shared key
     * @return Shared key
     */
    public byte[] getSharedKey() {
        return sharedKey;
    }
    
    /**
     * Encrypt data
     * @param data Data to encrypt
     * @return Encrypted data
     */
    public byte[] encrypt(byte[] data) {
        try {
            byte[] checksum = sha256(data);
            
            Cipher cipher = CryptoUtils.createAESCtrCipher(sharedKey, checksum, Cipher.ENCRYPT_MODE);
            byte[] encrypted = CryptoUtils.aesCtrTransform(cipher, data);
            
            byte[] result = new byte[serverKeyId.length + checksum.length + encrypted.length];
            System.arraycopy(serverKeyId, 0, result, 0, serverKeyId.length);
            System.arraycopy(checksum, 0, result, serverKeyId.length, checksum.length);
            System.arraycopy(encrypted, 0, result, serverKeyId.length + checksum.length, encrypted.length);
            
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting data", e);
        }
    }
    
    /**
     * Decrypt data
     * @param data Data to decrypt
     * @param checksum Checksum
     * @return Decrypted data
     */
    public byte[] decrypt(byte[] data, byte[] checksum) {
        try {
            Cipher cipher = CryptoUtils.createAESCtrCipher(sharedKey, checksum, Cipher.DECRYPT_MODE);
            byte[] decrypted = CryptoUtils.aesCtrTransform(cipher, data);
            
            byte[] calculatedChecksum = sha256(decrypted);
            if (!Arrays.equals(calculatedChecksum, checksum)) {
                throw new RuntimeException("Invalid checksum");
            }
            
            return decrypted;
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting data", e);
        }
    }
    
    /**
     * Calculate SHA-256 hash
     * @param data Data
     * @return SHA-256 hash
     */
    private byte[] sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
