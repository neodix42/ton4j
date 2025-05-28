package org.ton.ton4j.utils;

/**
 * Test class for Utils performance optimizations
 */
public class UtilsTest {
    public static void main(String[] args) {
        testByteUtils();
        testHashUtils();
        testCRC32Utils();
        testNetworkUtils();
        testBackwardCompatibility();
    }
    
    private static void testByteUtils() {
        System.out.println("\n=== Testing ByteUtils ===");
        
        // Test bytesToHex
        System.out.println("Testing bytesToHex...");
        byte[] testBytes = new byte[] {0x01, 0x02, 0x03, 0x04, 0x05};
        String hex = ByteUtils.bytesToHex(testBytes);
        System.out.println("Bytes to Hex: " + hex);
        assert "0102030405".equals(hex);
        
        // Test hexToSignedBytes
        System.out.println("Testing hexToSignedBytes...");
        String hexStr = "0102030405";
        byte[] bytes = ByteUtils.hexToSignedBytes(hexStr);
        assert bytes.length == 5;
        assert bytes[0] == 0x01;
        assert bytes[4] == 0x05;
        System.out.println("Hex to Bytes: Success");
        
        // Test slice
        System.out.println("Testing slice...");
        byte[] original = new byte[] {0x01, 0x02, 0x03, 0x04, 0x05};
        byte[] sliced = ByteUtils.slice(original, 1, 3);
        assert sliced.length == 3;
        assert sliced[0] == 0x02;
        assert sliced[2] == 0x04;
        System.out.println("Slice: Success");
        
        // Test concatBytes
        System.out.println("Testing concatBytes...");
        byte[] a = new byte[] {0x01, 0x02, 0x03};
        byte[] b = new byte[] {0x04, 0x05};
        byte[] result = ByteUtils.concatBytes(a, b);
        assert result.length == 5;
        assert result[0] == 0x01;
        assert result[4] == 0x05;
        System.out.println("Concat: Success");
        
        // Test signedBytesToUnsigned
        System.out.println("Testing signedBytesToUnsigned...");
        byte[] signedBytes = new byte[] {(byte)0xFF, (byte)0x80, 0x00};
        int[] unsignedInts = ByteUtils.signedBytesToUnsigned(signedBytes);
        assert unsignedInts.length == 3;
        assert unsignedInts[0] == 255;
        assert unsignedInts[1] == 128;
        assert unsignedInts[2] == 0;
        System.out.println("signedBytesToUnsigned: Success");
        
        // Test unsignedBytesToSigned
        System.out.println("Testing unsignedBytesToSigned...");
        int[] unsignedBytes = new int[] {255, 128, 0};
        byte[] signedResult = ByteUtils.unsignedBytesToSigned(unsignedBytes);
        assert signedResult.length == 3;
        assert signedResult[0] == (byte)0xFF;
        assert signedResult[1] == (byte)0x80;
        assert signedResult[2] == 0;
        System.out.println("unsignedBytesToSigned: Success");
        
        // Test bytesToInt and intToByteArray
        System.out.println("Testing bytesToInt and intToByteArray...");
        byte[] intBytes = new byte[] {0x00, 0x00, 0x01, 0x02};
        int intValue = ByteUtils.bytesToInt(intBytes);
        assert intValue == 258;
        System.out.println("bytesToInt: Success");
    }
    
    private static void testHashUtils() {
        System.out.println("\n=== Testing HashUtils ===");
        
        // Test SHA-256
        System.out.println("Testing sha256...");
        String input = "test";
        String hash = HashUtils.sha256(input);
        System.out.println("SHA-256 of 'test': " + hash);
        assert "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08".equals(hash);
        
        // Test SHA-256 with byte array
        System.out.println("Testing sha256 with byte array...");
        byte[] inputBytes = "test".getBytes();
        String hashFromBytes = HashUtils.sha256(inputBytes);
        assert hash.equals(hashFromBytes);
        System.out.println("SHA-256 with byte array: Success");
        
        // Test CRC-16
        System.out.println("Testing CRC-16...");
        byte[] crc16Data = "123456789".getBytes();
        int crc16Value = HashUtils.getCRC16ChecksumAsInt(crc16Data);
        System.out.println("CRC-16 of '123456789': " + Integer.toHexString(crc16Value));
        assert crc16Value == 0x31C3;
        System.out.println("CRC-16: Success");
    }
    
    private static void testCRC32Utils() {
        System.out.println("\n=== Testing CRC32Utils ===");
        
        // Test CRC32C
        System.out.println("Testing CRC32C...");
        byte[] data = "123456789".getBytes();
        long crc32Value = CRC32Utils.getCRC32ChecksumAsLong(data);
        System.out.println("CRC32C of '123456789': " + Long.toHexString(crc32Value));
        assert crc32Value == 0xe3069283L;
        System.out.println("CRC32C: Success");
        
        // Test CRC32C as hex
        System.out.println("Testing CRC32C as hex...");
        String crc32Hex = CRC32Utils.getCRC32ChecksumAsHex(data);
        System.out.println("CRC32C of '123456789' as hex: " + crc32Hex);
        assert "e3069283".equals(crc32Hex);
        System.out.println("CRC32C as hex: Success");
    }
    
    private static void testNetworkUtils() {
        System.out.println("\n=== Testing NetworkUtils ===");
        
        // Test int2ip and ip2int
        System.out.println("Testing int2ip and ip2int...");
        int ipInt = 0x7f000001; // 127.0.0.1
        String ipStr = NetworkUtils.int2ip(ipInt);
        System.out.println("int2ip(0x7f000001): " + ipStr);
        assert "127.0.0.1".equals(ipStr);
        
        int ipIntBack = NetworkUtils.ip2int(ipStr);
        System.out.println("ip2int('127.0.0.1'): 0x" + Integer.toHexString(ipIntBack));
        assert ipIntBack == ipInt;
        System.out.println("int2ip and ip2int: Success");
        
        // Test getOS
        System.out.println("Testing getOS...");
        NetworkUtils.OS os = NetworkUtils.getOS();
        System.out.println("Detected OS: " + os);
        System.out.println("getOS: Success");
    }
    
    private static void testBackwardCompatibility() {
        System.out.println("\n=== Testing Backward Compatibility ===");
        
        // Test Utils.bytesToHex
        System.out.println("Testing Utils.bytesToHex...");
        byte[] testBytes = new byte[] {0x01, 0x02, 0x03, 0x04, 0x05};
        String hex = Utils.bytesToHex(testBytes);
        System.out.println("Bytes to Hex: " + hex);
        assert "0102030405".equals(hex);
        
        // Test Utils.sha256
        System.out.println("Testing Utils.sha256...");
        String input = "test";
        String hash = Utils.sha256(input);
        System.out.println("SHA-256 of 'test': " + hash);
        assert "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08".equals(hash);
        
        // Test Utils.getCRC32ChecksumAsLong
        System.out.println("Testing Utils.getCRC32ChecksumAsLong...");
        byte[] data = "123456789".getBytes();
        long crc32Value = Utils.getCRC32ChecksumAsLong(data);
        System.out.println("CRC32C of '123456789': " + Long.toHexString(crc32Value));
        assert crc32Value == 0xe3069283L;
        
        System.out.println("Backward Compatibility: Success");
    }
}
