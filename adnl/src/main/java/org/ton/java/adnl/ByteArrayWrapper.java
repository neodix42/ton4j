package org.ton.java.adnl;

import java.util.Arrays;

/**
 * Wrapper for byte arrays to use as keys in maps
 */
public class ByteArrayWrapper {
    private final byte[] data;
    
    public ByteArrayWrapper(byte[] data) {
        this.data = data;
    }
    
    public byte[] getData() {
        return data;
    }
    
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ByteArrayWrapper)) {
            return false;
        }
        return Arrays.equals(data, ((ByteArrayWrapper) other).data);
    }
    
    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }
}
