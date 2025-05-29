package org.ton.java.adnl.message;

import java.util.Arrays;

/**
 * ADNL message part for large messages
 * TL: adnl.message.part hash:int256 total_size:int offset:int data:bytes = adnl.Message
 */
public class MessagePart {
    private byte[] hash;
    private int totalSize;
    private int offset;
    private byte[] data;
    
    public MessagePart() {}
    
    public MessagePart(byte[] hash, int totalSize, int offset, byte[] data) {
        this.hash = hash;
        this.totalSize = totalSize;
        this.offset = offset;
        this.data = data;
    }
    
    public byte[] getHash() {
        return hash;
    }
    
    public void setHash(byte[] hash) {
        this.hash = hash;
    }
    
    public int getTotalSize() {
        return totalSize;
    }
    
    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }
    
    public int getOffset() {
        return offset;
    }
    
    public void setOffset(int offset) {
        this.offset = offset;
    }
    
    public byte[] getData() {
        return data;
    }
    
    public void setData(byte[] data) {
        this.data = data;
    }
    
    @Override
    public String toString() {
        return "MessagePart{hash=" + Arrays.toString(hash) + 
               ", totalSize=" + totalSize + ", offset=" + offset + 
               ", data.length=" + (data != null ? data.length : 0) + "}";
    }
}
