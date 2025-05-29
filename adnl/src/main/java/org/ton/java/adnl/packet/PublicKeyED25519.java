package org.ton.java.adnl.packet;

import java.util.Arrays;

/**
 * Ed25519 public key structure for ADNL
 * Mirrors the Go implementation of PublicKeyED25519
 */
public class PublicKeyED25519 {
    private byte[] key;
    
    public PublicKeyED25519() {}
    
    public PublicKeyED25519(byte[] key) {
        this.key = key;
    }
    
    public byte[] getKey() {
        return key;
    }
    
    public void setKey(byte[] key) {
        this.key = key;
    }
    
    @Override
    public String toString() {
        return "PublicKeyED25519{key=" + Arrays.toString(key) + "}";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PublicKeyED25519 that = (PublicKeyED25519) obj;
        return Arrays.equals(key, that.key);
    }
    
    @Override
    public int hashCode() {
        return Arrays.hashCode(key);
    }
}
