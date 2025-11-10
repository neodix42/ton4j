package org.ton.ton4j.adnl;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for CryptoUtils, migrated from Go liteclient tests
 */
public class CryptoUtilsTest {

    @Test
    public void testSharedKey() {
        // Test case: basic
        // This mirrors the Go test case with the same input data and expected output
        
        // Our Ed25519 private key (from seed in Go test)
        byte[] ourPrivateKey = new byte[]{
            (byte) 175, (byte) 46, (byte) 138, (byte) 194, (byte) 124, (byte) 100, (byte) 226,
            (byte) 85, (byte) 88, (byte) 44, (byte) 196, (byte) 159, (byte) 130, (byte) 167,
            (byte) 223, (byte) 23, (byte) 125, (byte) 231, (byte) 145, (byte) 177, (byte) 104,
            (byte) 171, (byte) 189, (byte) 252, (byte) 16, (byte) 143, (byte) 108, (byte) 237,
            (byte) 99, (byte) 32, (byte) 104, (byte) 10
        };
        
        // Server's Ed25519 public key
        byte[] serverPublicKey = new byte[]{
            (byte) 159, (byte) 133, (byte) 67, (byte) 157, (byte) 32, (byte) 148, (byte) 185, (byte) 42,
            (byte) 99, (byte) 156, (byte) 44, (byte) 148, (byte) 147, (byte) 215, (byte) 183, (byte) 64,
            (byte) 227, (byte) 157, (byte) 234, (byte) 141, (byte) 8, (byte) 181, (byte) 37, (byte) 152,
            (byte) 109, (byte) 57, (byte) 214, (byte) 221, (byte) 105, (byte) 231, (byte) 243, (byte) 9
        };
        
        // Expected shared key result
        byte[] expectedSharedKey = new byte[]{
            (byte) 220, (byte) 183, (byte) 46, (byte) 193, (byte) 213, (byte) 106,
            (byte) 149, (byte) 6, (byte) 197, (byte) 7, (byte) 75, (byte) 228, (byte) 108, (byte) 247,
            (byte) 216, (byte) 126, (byte) 194, (byte) 59, (byte) 250, (byte) 51,
            (byte) 191, (byte) 19, (byte) 17, (byte) 221, (byte) 189, (byte) 86,
            (byte) 228, (byte) 159, (byte) 226, (byte) 223, (byte) 135, (byte) 119
        };
        
        // Test the sharedKey method
        try {
            byte[] actualSharedKey = CryptoUtils.sharedKey(ourPrivateKey, serverPublicKey);
            
            // Verify the result
            assertNotNull(actualSharedKey, "Shared key should not be null");
            assertEquals(expectedSharedKey.length, actualSharedKey.length, 
                "Shared key should have the expected length");
            
            // Compare byte arrays
            assertArrayEquals(expectedSharedKey, actualSharedKey, 
                "Shared key should match the expected value");
                
        } catch (Exception e) {
            fail("sharedKey() should not throw an exception: " + e.getMessage());
        }
    }
    
    @Test
    public void testSharedKeyWithNullInputs() {
        // Test error handling with null inputs
        assertThrows(RuntimeException.class, () -> {
            CryptoUtils.sharedKey(null, new byte[32]);
        }, "Should throw exception with null private key");
        
        assertThrows(RuntimeException.class, () -> {
            CryptoUtils.sharedKey(new byte[32], null);
        }, "Should throw exception with null public key");
        
        assertThrows(RuntimeException.class, () -> {
            CryptoUtils.sharedKey(null, null);
        }, "Should throw exception with both null keys");
    }
    
    @Test
    public void testSharedKeyWithInvalidKeyLengths() {
        // Test with invalid key lengths
        byte[] shortPrivateKey = new byte[16]; // Too short
        byte[] shortPublicKey = new byte[16];  // Too short
        byte[] validPrivateKey = new byte[32];
        byte[] validPublicKey = new byte[32];
        
        // These should throw exceptions for invalid key lengths
        assertThrows(RuntimeException.class, () -> {
            CryptoUtils.sharedKey(shortPrivateKey, validPublicKey);
        }, "Should throw exception with short private key");
        
        assertThrows(RuntimeException.class, () -> {
            CryptoUtils.sharedKey(validPrivateKey, shortPublicKey);
        }, "Should throw exception with short public key");
    }
    
    @Test
    public void testSharedKeyDeterministic() {
        // Test that the same inputs always produce the same output
        byte[] privateKey = new byte[32];
        byte[] publicKey = new byte[32];
        
        // Fill with some test data
        for (int i = 0; i < 32; i++) {
            privateKey[i] = (byte) i;
            publicKey[i] = (byte) (i + 32);
        }
        
        byte[] result1 = CryptoUtils.sharedKey(privateKey, publicKey);
        byte[] result2 = CryptoUtils.sharedKey(privateKey, publicKey);
        
        assertArrayEquals(result1, result2, 
            "sharedKey should be deterministic - same inputs should produce same output");
    }
}
