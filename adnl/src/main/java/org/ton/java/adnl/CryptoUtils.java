package org.ton.java.adnl;

import com.iwebpp.crypto.TweetNaclFast;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Cryptographic utilities for ADNL protocol
 */
public class CryptoUtils {
    
    /**
     * Get shared key using X25519
     * @param privateKey Private key
     * @param publicKey Public key
     * @return Shared key
     */
    public static byte[] getSharedKey(byte[] privateKey, byte[] publicKey) {
        TweetNaclFast.Box box = new TweetNaclFast.Box(publicKey, privateKey);
        byte[] zero = new byte[24];
        byte[] sharedKey = box.before();
        return sharedKey;
    }
    
    /**
     * Create AES-CTR cipher
     * @param key Key
     * @param iv IV
     * @param mode Cipher mode (ENCRYPT_MODE or DECRYPT_MODE)
     * @return Cipher
     */
    public static Cipher createAESCtrCipher(byte[] key, byte[] iv, int mode) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(Arrays.copyOf(key, 32), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(Arrays.copyOf(iv, 16));
            Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
            cipher.init(mode, keySpec, ivSpec);
            return cipher;
        } catch (Exception e) {
            throw new RuntimeException("Error creating AES-CTR cipher", e);
        }
    }
    
    /**
     * Transform data using AES-CTR
     * @param cipher Cipher
     * @param data Data
     * @return Transformed data
     */
    public static byte[] aesCtrTransform(Cipher cipher, byte[] data) {
        try {
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("Error transforming data with AES-CTR", e);
        }
    }
    
    /**
     * Get random bytes
     * @param length Length
     * @return Random bytes
     */
    public static byte[] getRandomBytes(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }
    
    /**
     * Convert bytes to hex string
     * @param bytes Bytes
     * @return Hex string
     */
    public static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
    
    /**
     * Convert hex string to bytes
     * @param hex Hex string
     * @return Bytes
     */
    public static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
    
    /**
     * Convert hex string to signed bytes
     * @param hex Hex string
     * @return Signed bytes
     */
    public static byte[] hexToSignedBytes(String hex) {
        byte[] bytes = hexToBytes(hex);
        byte[] result = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            result[i] = (byte) (bytes[i] & 0xff);
        }
        return result;
    }
}
