package org.ton.ton4j.utils;

import java.math.BigInteger;

/**
 * Optimized utility class for CRC32 operations
 */
public final class CRC32Utils {
    
    private CRC32Utils() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Calculate CRC32C checksum using polynomial 0x1EDC6F41
     * @param bytes The input bytes
     * @return The CRC32C checksum as a long
     */
    public static Long getCRC32ChecksumAsLong(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return 0L;
        }
        
        CRC32C crc32c = new CRC32C();
        crc32c.update(bytes, 0, bytes.length);
        return crc32c.getValue() & 0x00000000ffffffffL;
    }
    
    /**
     * Calculate CRC32C checksum and return as hex string
     * @param bytes The input bytes
     * @return The CRC32C checksum as hex string
     */
    public static String getCRC32ChecksumAsHex(byte[] bytes) {
        return BigInteger.valueOf(getCRC32ChecksumAsLong(bytes)).toString(16);
    }
    
    /**
     * Calculate CRC32C checksum and return as bytes
     * @param bytes The input bytes
     * @return The CRC32C checksum as bytes
     */
    public static byte[] getCRC32ChecksumAsBytes(byte[] bytes) {
        return ByteUtils.long4BytesToBytes(getCRC32ChecksumAsLong(bytes));
    }
    
    /**
     * Calculate CRC32C checksum and return as reversed bytes
     * @param bytes The input bytes
     * @return The CRC32C checksum as reversed bytes
     */
    public static byte[] getCRC32ChecksumAsBytesReversed(byte[] bytes) {
        byte[] b = ByteUtils.long4BytesToBytes(getCRC32ChecksumAsLong(bytes));

        byte[] reversed = new byte[4];
        reversed[0] = b[3];
        reversed[1] = b[2];
        reversed[2] = b[1];
        reversed[3] = b[0];

        return reversed;
    }
}
