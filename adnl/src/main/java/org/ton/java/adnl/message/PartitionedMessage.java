package org.ton.java.adnl.message;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Handles partitioned messages for large ADNL messages
 * Mirrors the Go implementation of partitionedMessage
 */
public class PartitionedMessage {
    private final Instant startedAt;
    private final Map<Integer, Boolean> knownOffsets;
    private final byte[] buffer;
    private int gotLength;
    private final ReentrantLock lock;
    
    public PartitionedMessage(int size) {
        this.startedAt = Instant.now();
        this.knownOffsets = new HashMap<>();
        this.buffer = new byte[size];
        this.gotLength = 0;
        this.lock = new ReentrantLock();
    }
    
    /**
     * Add a part to the message
     * @param offset Offset in the message
     * @param data Data to add
     * @return true if message is complete, false otherwise
     * @throws Exception if error occurs
     */
    public boolean addPart(int offset, byte[] data) throws Exception {
        lock.lock();
        try {
            if (gotLength == buffer.length) {
                // Already full, skip part processing and don't report as ready
                return false;
            }
            
            if (data.length == 0 || offset < 0) {
                return false;
            }
            
            if (buffer.length - offset < data.length) {
                throw new Exception("Part is bigger than defined message");
            }
            
            if (knownOffsets.containsKey(offset)) {
                return false;
            }
            
            if (knownOffsets.size() > 32) {
                throw new Exception("Too many parts");
            }
            
            System.arraycopy(data, 0, buffer, offset, data.length);
            
            knownOffsets.put(offset, true);
            gotLength += data.length;
            
            return gotLength == buffer.length;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Build the final message from parts
     * @param msgHash Expected hash of the message
     * @return Complete message data
     * @throws Exception if message is not complete or hash doesn't match
     */
    public byte[] build(byte[] msgHash) throws Exception {
        lock.lock();
        try {
            if (gotLength != buffer.length) {
                throw new Exception("Not full yet");
            }
            
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] hash = sha256.digest(buffer);
            
            if (!Arrays.equals(hash, msgHash)) {
                throw new Exception("Invalid message, hash not matches");
            }
            
            return Arrays.copyOf(buffer, buffer.length);
        } finally {
            lock.unlock();
        }
    }
    
    public Instant getStartedAt() {
        return startedAt;
    }
    
    public boolean isComplete() {
        lock.lock();
        try {
            return gotLength == buffer.length;
        } finally {
            lock.unlock();
        }
    }
}
