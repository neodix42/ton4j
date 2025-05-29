package org.ton.java.adnl.liteclient.tl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Base class for TL (Type Language) serialization of liteserver queries
 * Based on TON TL schema for liteserver API
 */
public abstract class LiteServerQuery {
    
    /**
     * Get the TL constructor ID for this query type
     */
    public abstract int getConstructorId();
    
    /**
     * Serialize this query to bytes using TL format
     */
    public byte[] serialize() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        // Write constructor ID (4 bytes, little endian)
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(getConstructorId());
        baos.write(buffer.array());
        
        // Write query-specific data
        serializeData(baos);
        
        return baos.toByteArray();
    }
    
    /**
     * Serialize query-specific data
     */
    protected abstract void serializeData(ByteArrayOutputStream baos) throws IOException;
    
    /**
     * Write a 32-bit integer in little endian format
     */
    protected void writeInt32(ByteArrayOutputStream baos, int value) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(value);
        baos.write(buffer.array());
    }
    
    /**
     * Write a 64-bit long in little endian format
     */
    protected void writeInt64(ByteArrayOutputStream baos, long value) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(value);
        baos.write(buffer.array());
    }
    
    /**
     * Write a 256-bit hash (32 bytes)
     */
    protected void writeInt256(ByteArrayOutputStream baos, byte[] hash) throws IOException {
        if (hash.length != 32) {
            throw new IllegalArgumentException("Hash must be 32 bytes");
        }
        baos.write(hash);
    }
    
    /**
     * Write bytes with length prefix
     */
    protected void writeBytes(ByteArrayOutputStream baos, byte[] data) throws IOException {
        if (data.length < 254) {
            baos.write(data.length);
            baos.write(data);
            // Add padding to align to 4 bytes
            int padding = (4 - ((data.length + 1) % 4)) % 4;
            for (int i = 0; i < padding; i++) {
                baos.write(0);
            }
        } else {
            baos.write(254);
            writeInt32(baos, data.length);
            baos.write(data);
            // Add padding to align to 4 bytes
            int padding = (4 - (data.length % 4)) % 4;
            for (int i = 0; i < padding; i++) {
                baos.write(0);
            }
        }
    }
}
