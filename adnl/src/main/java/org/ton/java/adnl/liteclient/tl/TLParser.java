package org.ton.java.adnl.liteclient.tl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Parser for TL (Type Language) responses from TON liteserver
 */
public class TLParser {
    
    private final ByteBuffer buffer;
    
    public TLParser(byte[] data) {
        this.buffer = ByteBuffer.wrap(data);
        this.buffer.order(ByteOrder.LITTLE_ENDIAN);
    }
    
    /**
     * Read 32-bit integer
     */
    public int readInt32() {
        return buffer.getInt();
    }
    
    /**
     * Read 64-bit long
     */
    public long readInt64() {
        return buffer.getLong();
    }
    
    /**
     * Read 256-bit hash (32 bytes)
     */
    public byte[] readInt256() {
        byte[] hash = new byte[32];
        buffer.get(hash);
        return hash;
    }
    
    /**
     * Read bytes with length prefix
     */
    public byte[] readBytes() {
        int length = buffer.get() & 0xFF;
        
        if (length == 254) {
            // Long form: next 3 bytes contain length
            length = readInt32() & 0xFFFFFF;
        }
        
        byte[] data = new byte[length];
        buffer.get(data);
        
        // Skip padding to align to 4 bytes
        int padding;
        if (length < 254) {
            padding = (4 - ((length + 1) % 4)) % 4;
        } else {
            padding = (4 - (length % 4)) % 4;
        }
        
        for (int i = 0; i < padding; i++) {
            buffer.get(); // skip padding byte
        }
        
        return data;
    }
    
    /**
     * Check if there's more data to read
     */
    public boolean hasRemaining() {
        return buffer.hasRemaining();
    }
    
    /**
     * Get current position
     */
    public int position() {
        return buffer.position();
    }
    
    /**
     * Get remaining bytes count
     */
    public int remaining() {
        return buffer.remaining();
    }
}
