package org.ton.java.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optimized utility class for hashing operations
 */
public final class HashUtils {
    // Cache for expensive operations
    private static final Map<String, MessageDigest> MESSAGE_DIGEST_CACHE = new ConcurrentHashMap<>();
    
    private HashUtils() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Get a cached MessageDigest instance for the specified algorithm
     * @param algorithm The digest algorithm
     * @return MessageDigest instance
     * @throws NoSuchAlgorithmException If the algorithm is not available
     */
    private static MessageDigest getMessageDigest(String algorithm) throws NoSuchAlgorithmException {
        MessageDigest digest = MESSAGE_DIGEST_CACHE.get(algorithm);
        if (digest == null) {
            digest = MessageDigest.getInstance(algorithm);
            MESSAGE_DIGEST_CACHE.put(algorithm, digest);
        }
        digest.reset();
        return digest;
    }
    
    /**
     * Calculate SHA-256 hash of a byte array
     * @param bytes The input bytes
     * @return The hash as a byte array
     */
    public static byte[] sha256AsArray(byte[] bytes) {
        if (bytes == null) {
            return new byte[0];
        }
        
        try {
            final MessageDigest digest = getMessageDigest("SHA-256");
            return digest.digest(bytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Calculate SHA-256 hash of a byte array and return as hex string
     * @param bytes The input bytes
     * @return The hash as a hex string
     */
    public static String sha256(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        
        try {
            final MessageDigest digest = getMessageDigest("SHA-256");
            final byte[] hash = digest.digest(bytes);
            return ByteUtils.bytesToHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Calculate SHA-256 hash of a string and return as hex string
     * @param base The input string
     * @return The hash as a hex string
     */
    public static String sha256(final String base) {
        if (base == null) {
            return "";
        }
        
        try {
            final MessageDigest digest = getMessageDigest("SHA-256");
            final byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            return ByteUtils.bytesToHex(hash);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Calculate SHA-256 hash of an int array and return as hex string
     * @param bytes The input int array
     * @return The hash as a hex string
     */
    public static String sha256(int[] bytes) {
        if (bytes == null) {
            return "";
        }
        
        byte[] converted = ByteUtils.unsignedBytesToSigned(bytes);
        return sha256(converted);
    }
    
    /**
     * Calculate SHA-1 hash of a byte array
     * @param bytes The input bytes
     * @return The hash as a byte array
     */
    public static byte[] sha1AsArray(byte[] bytes) {
        if (bytes == null) {
            return new byte[0];
        }
        
        try {
            final MessageDigest digest = getMessageDigest("SHA-1");
            return digest.digest(bytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Calculate SHA-1 hash of a byte array and return as hex string
     * @param bytes The input bytes
     * @return The hash as a hex string
     */
    public static String sha1(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        
        try {
            final MessageDigest digest = getMessageDigest("SHA-1");
            final byte[] hash = digest.digest(bytes);
            return ByteUtils.bytesToHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Calculate MD5 hash of a byte array
     * @param bytes The input bytes
     * @return The hash as a byte array
     */
    public static byte[] md5AsArray(byte[] bytes) {
        if (bytes == null) {
            return new byte[0];
        }
        
        try {
            final MessageDigest digest = getMessageDigest("MD5");
            return digest.digest(bytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Calculate MD5 hash of a byte array and return as hex string
     * @param bytes The input bytes
     * @return The hash as a hex string
     */
    public static String md5(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        
        try {
            final MessageDigest digest = getMessageDigest("MD5");
            final byte[] hash = digest.digest(bytes);
            return ByteUtils.bytesToHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * CRC-16/XMODEM implementation
     * @param bytes The input bytes
     * @return The CRC-16 checksum
     */
    public static int getCRC16ChecksumAsInt(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return 0;
        }
        
        int crc = 0x0000;
        int polynomial = 0x1021;

        for (byte b : bytes) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b >> (7 - i) & 1) == 1);
                boolean c15 = ((crc >> 15 & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit) crc ^= polynomial;
            }
        }

        crc &= 0xffff;
        return crc;
    }
    
    /**
     * Calculate CRC-16 checksum and return as bytes
     * @param bytes The input bytes
     * @return The CRC-16 checksum as bytes
     */
    public static byte[] getCRC16ChecksumAsBytes(byte[] bytes) {
        return ByteUtils.intToByteArray(getCRC16ChecksumAsInt(bytes));
    }
    
    /**
     * Calculate CRC-16 checksum and return as hex string
     * @param bytes The input bytes
     * @return The CRC-16 checksum as hex string
     */
    public static String getCRC16ChecksumAsHex(byte[] bytes) {
        return ByteUtils.bytesToHex(getCRC16ChecksumAsBytes(bytes));
    }
    
    /**
     * Calculate method ID from method name
     * @param methodName The method name
     * @return The method ID
     */
    public static int calculateMethodId(String methodName) {
        int l = getCRC16ChecksumAsInt(methodName.getBytes(StandardCharsets.UTF_8));
        l = (l & 0xffff) | 0x10000;
        return l;
    }
}
