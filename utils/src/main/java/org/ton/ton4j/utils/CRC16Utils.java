package org.ton.ton4j.utils;

/**
 * Utility class for CRC16 checksum operations
 */
public final class CRC16Utils {
    
    private CRC16Utils() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Calculate CRC-16/XMODEM checksum as an integer
     * @param bytes The bytes to calculate the checksum for
     * @return The checksum as an integer
     */
    public static int getCRC16ChecksumAsInt(byte[] bytes) {
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
     * Calculate method ID from method name
     * @param methodName The method name
     * @return The method ID
     */
    public static int calculateMethodId(String methodName) {
        int l = getCRC16ChecksumAsInt(methodName.getBytes());
        l = (l & 0xffff) | 0x10000;
        return l;
    }
    
    /**
     * Calculate CRC-16/XMODEM checksum as a hex string
     * @param bytes The bytes to calculate the checksum for
     * @return The checksum as a hex string
     */
    public static String getCRC16ChecksumAsHex(byte[] bytes) {
        return ByteUtils.bytesToHex(getCRC16ChecksumAsBytes(bytes));
    }
    
    /**
     * Calculate CRC-16/XMODEM checksum as bytes
     * @param bytes The bytes to calculate the checksum for
     * @return The checksum as bytes
     */
    public static byte[] getCRC16ChecksumAsBytes(byte[] bytes) {
        return ByteUtils.intToByteArray(getCRC16ChecksumAsInt(bytes));
    }
}
