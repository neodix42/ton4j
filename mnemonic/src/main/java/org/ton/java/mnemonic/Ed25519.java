package org.ton.java.mnemonic;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;


public class Ed25519 {

    public static final int KEY_SIZE = 32;

    public static byte[] privateKey() throws NoSuchAlgorithmException {
        SecureRandom rnd = SecureRandom.getInstanceStrong();
        byte[] publicKey = new byte[KEY_SIZE];
        rnd.nextBytes(publicKey);
        return publicKey;
    }

    public static byte[] publicKey(byte[] privateKey) throws NoSuchAlgorithmException {
        SecureRandom rnd = SecureRandom.getInstanceStrong();
        byte[] publicKey = new byte[KEY_SIZE];
        org.bouncycastle.math.ec.rfc8032.Ed25519.generatePublicKey(privateKey, 0, publicKey, 0);
        return publicKey;
    }

    public static byte[] sign(byte[] privateKey, byte[] message) {
        byte[] signature = new byte[64];
        org.bouncycastle.math.ec.rfc8032.Ed25519.sign(privateKey, 0, message, 0, message.length, signature, 0);
        return signature;
    }

    public static boolean verify(byte[] publicKey, byte[] message, byte[] signature) {
        return org.bouncycastle.math.ec.rfc8032.Ed25519.verify(signature, 0, publicKey, 0, message, 0, message.length);
    }
}
