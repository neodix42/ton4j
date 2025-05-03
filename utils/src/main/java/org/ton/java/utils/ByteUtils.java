package org.ton.java.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optimized utility class for byte array operations
 */
public final class ByteUtils {
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
    private static final Map<String, byte[]> HEX_TO_BYTES_CACHE = new ConcurrentHashMap<>(256);
    
    // Initialize common hex values for faster lookup
    static {
        // Pre-compute common hex conversions
        for (int i = 0; i < 256; i++) {
            String hex = String.format("%02x", i);
            HEX_TO_BYTES_CACHE.put(hex, new byte[] { (byte) i });
        }
    }
    
    private ByteUtils() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Optimized bytesToHex implementation using a lookup table
     * @param raw The byte array to convert
     * @return Hex string representation
     */
    public static String bytesToHex(byte[] raw) {
        if (raw == null || raw.length == 0) {
            return "";
        }
        
        final int len = raw.length;
        final char[] hexChars = new char[len * 2];
        
        for (int i = 0, j = 0; i < len; i++) {
            final int v = raw[i] & 0xFF;
            hexChars[j++] = HEX_CHARS[v >>> 4];
            hexChars[j++] = HEX_CHARS[v & 0x0F];
        }
        
        return new String(hexChars);
    }
    
    /**
     * Convert a hex string to a byte array
     * @param hex The hex string to convert
     * @return The byte array
     */
    public static byte[] hexToSignedBytes(String hex) {
        if (hex == null || hex.isEmpty()) {
            return new byte[0];
        }
        
        // Check if we have a cached value for common hex strings
        if (hex.length() == 2) {
            byte[] cached = HEX_TO_BYTES_CACHE.get(hex.toLowerCase());
            if (cached != null) {
                return cached.clone();
            }
        }
        
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                               + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
    
    /**
     * Optimized slice implementation that avoids unnecessary array copying
     * @param src Source byte array
     * @param from Start index
     * @param size Number of bytes to slice
     * @return Sliced byte array
     */
    public static byte[] slice(byte[] src, int from, int size) {
        if (src == null) {
            return new byte[0];
        }
        
        if (from < 0 || size < 0 || from + size > src.length) {
            throw new IllegalArgumentException("Invalid slice parameters");
        }
        
        byte[] resultArray = new byte[size];
        System.arraycopy(src, from, resultArray, 0, size);
        return resultArray;
    }
    
    /**
     * Optimized byte array concatenation using System.arraycopy
     * @param a First byte array
     * @param b Second byte array
     * @return Concatenated byte array
     */
    public static byte[] concatBytes(byte[] a, byte[] b) {
        if (a == null) return b != null ? b.clone() : new byte[0];
        if (b == null) return a.clone();
        
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }
    
    /**
     * Convert signed bytes to unsigned integers
     * @param bytes The signed bytes
     * @return Array of unsigned integers
     */
    public static int[] signedBytesToUnsigned(byte[] bytes) {
        if (bytes == null) {
            return new int[0];
        }
        
        int[] converted = new int[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            converted[i] = bytes[i] & 0xFF;
        }
        return converted;
    }
    
    /**
     * Convert unsigned integers to signed bytes
     * @param bytes The unsigned integers
     * @return Array of signed bytes
     */
    public static byte[] unsignedBytesToSigned(int[] bytes) {
        if (bytes == null) {
            return new byte[0];
        }
        
        byte[] converted = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            converted[i] = (byte) (bytes[i] & 0xff);
        }
        return converted;
    }
    
    /**
     * Long to signed bytes
     *
     * @param l value
     * @return array of unsigned bytes
     */
    public static byte[] long4BytesToBytes(long l) {
        byte[] result = new byte[4];
        result[3] = (byte) (l & 0xFF);
        result[2] = (byte) ((l >> 8) & 0xFF);
        result[1] = (byte) ((l >> 16) & 0xFF);
        result[0] = (byte) ((l >> 24) & 0xFF);
        return result;
    }
    
    public static int[] longToBytes(long l) {
        int[] result = new int[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (int) l & 0xFF;
            l >>= 8;
        }
        return result;
    }
    
    public static long bytesToLong(final byte[] b) {
        if (b == null || b.length < 8) {
            throw new IllegalArgumentException("Byte array must be at least 8 bytes long");
        }
        
        return (((long) b[0] & 0xFF) << 56) |
               (((long) b[1] & 0xFF) << 48) |
               (((long) b[2] & 0xFF) << 40) |
               (((long) b[3] & 0xFF) << 32) |
               (((long) b[4] & 0xFF) << 24) |
               (((long) b[5] & 0xFF) << 16) |
               (((long) b[6] & 0xFF) << 8) |
               ((long) b[7] & 0xFF);
    }
    
    public static int bytesToInt(final byte[] b) {
        if (b == null || b.length < 4) {
            throw new IllegalArgumentException("Byte array must be at least 4 bytes long");
        }
        
        return ((b[0] & 0xFF) << 24) |
               ((b[1] & 0xFF) << 16) |
               ((b[2] & 0xFF) << 8) |
               (b[3] & 0xFF);
    }
    
    public static int bytesToIntX(final byte[] b) {
        if (b == null || b.length == 0) {
            return 0;
        }
        
        int result = 0;
        for (byte value : b) {
            result = (result << 8) | (value & 0xFF);
        }
        return result;
    }
    
    public static short bytesToShort(final byte[] b) {
        if (b == null || b.length < 2) {
            throw new IllegalArgumentException("Byte array must be at least 2 bytes long");
        }
        
        return (short) (((b[0] & 0xFF) << 8) | (b[1] & 0xFF));
    }
    
    public static long intsToLong(final int[] b) {
        if (b == null || b.length < 8) {
            throw new IllegalArgumentException("Int array must be at least 8 elements long");
        }
        
        long result = 0;
        for (int i = 0; i < 8; i++) {
            result = (result << 8) | (b[i] & 0xFF);
        }
        return result;
    }
    
    public static int intsToInt(final int[] b) {
        if (b == null || b.length < 4) {
            throw new IllegalArgumentException("Int array must be at least 4 elements long");
        }
        
        return ((b[0] & 0xFF) << 24) |
               ((b[1] & 0xFF) << 16) |
               ((b[2] & 0xFF) << 8) |
               (b[3] & 0xFF);
    }
    
    public static short intsToShort(final int[] b) {
        if (b == null || b.length < 2) {
            throw new IllegalArgumentException("Int array must be at least 2 elements long");
        }
        
        return (short) (((b[0] & 0xFF) << 8) | (b[1] & 0xFF));
    }
    
    public static int[] intToIntArray(int l) {
        return new int[] {l};
    }
    
    public static byte[] intToByteArray(int value) {
        return new byte[] {(byte) (value >>> 8), (byte) value};
    }
    
    /**
     * Convert an int array to a hex string
     * @param raw The int array to convert
     * @return Hex string representation
     */
    public static String bytesToHex(int[] raw) {
        if (raw == null || raw.length == 0) {
            return "";
        }
        
        final int len = raw.length;
        final char[] hexChars = new char[len * 2];
        
        for (int i = 0, j = 0; i < len; i++) {
            final int v = raw[i] & 0xFF;
            hexChars[j++] = HEX_CHARS[v >>> 4];
            hexChars[j++] = HEX_CHARS[v & 0x0F];
        }
        
        return new String(hexChars);
    }
}
