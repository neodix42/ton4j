package org.ton.ton4j.tl.types.db.block;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Extended block identifier for TON blockchain.
 * Based on the TL schema definition for tonNode.blockIdExt.
 */
public class BlockIdExt {
    private static final int SERIALIZED_SIZE = 4 + 4 + 8 + 32; // workchain + shard + seqno + rootHash
    
    private int workchain;
    private long shard;
    private long seqno;
    private byte[] rootHash;
    private byte[] fileHash;
    
    /**
     * Creates a new BlockIdExt.
     */
    public BlockIdExt() {
        this.rootHash = new byte[32];
        this.fileHash = new byte[32];
    }
    
    /**
     * Creates a new BlockIdExt with the specified parameters.
     * 
     * @param workchain The workchain ID
     * @param shard The shard ID
     * @param seqno The sequence number
     * @param rootHash The root hash (32 bytes)
     * @param fileHash The file hash (32 bytes)
     */
    public BlockIdExt(int workchain, long shard, long seqno, byte[] rootHash, byte[] fileHash) {
        this.workchain = workchain;
        this.shard = shard;
        this.seqno = seqno;
        this.rootHash = Arrays.copyOf(rootHash, 32);
        this.fileHash = Arrays.copyOf(fileHash, 32);
    }
    
    /**
     * Deserializes a BlockIdExt from a ByteBuffer.
     * 
     * @param buffer The ByteBuffer containing the serialized BlockIdExt
     * @return The deserialized BlockIdExt
     */
    public static BlockIdExt deserialize(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        
        BlockIdExt blockIdExt = new BlockIdExt();
        
        blockIdExt.workchain = buffer.getInt();
        blockIdExt.shard = buffer.getLong();
        blockIdExt.seqno = buffer.getLong();
        
        blockIdExt.rootHash = new byte[32];
        buffer.get(blockIdExt.rootHash);
        
        blockIdExt.fileHash = new byte[32];
        buffer.get(blockIdExt.fileHash);
        
        return blockIdExt;
    }
    
    /**
     * Serializes this BlockIdExt to a ByteBuffer.
     * 
     * @param buffer The ByteBuffer to write to
     */
    public void serialize(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        
        buffer.putInt(workchain);
        buffer.putLong(shard);
        buffer.putLong(seqno);
        buffer.put(rootHash);
        buffer.put(fileHash);
    }
    
    /**
     * Gets the serialized size of this BlockIdExt.
     * 
     * @return The serialized size in bytes
     */
    public int getSerializedSize() {
        return SERIALIZED_SIZE;
    }
    
    /**
     * Gets the workchain ID.
     * 
     * @return The workchain ID
     */
    public int getWorkchain() {
        return workchain;
    }
    
    /**
     * Sets the workchain ID.
     * 
     * @param workchain The workchain ID
     */
    public void setWorkchain(int workchain) {
        this.workchain = workchain;
    }
    
    /**
     * Gets the shard ID.
     * 
     * @return The shard ID
     */
    public long getShard() {
        return shard;
    }
    
    /**
     * Sets the shard ID.
     * 
     * @param shard The shard ID
     */
    public void setShard(long shard) {
        this.shard = shard;
    }
    
    /**
     * Gets the sequence number.
     * 
     * @return The sequence number
     */
    public long getSeqno() {
        return seqno;
    }
    
    /**
     * Sets the sequence number.
     * 
     * @param seqno The sequence number
     */
    public void setSeqno(long seqno) {
        this.seqno = seqno;
    }
    
    /**
     * Gets the root hash.
     * 
     * @return The root hash
     */
    public byte[] getRootHash() {
        return rootHash;
    }
    
    /**
     * Sets the root hash.
     * 
     * @param rootHash The root hash
     */
    public void setRootHash(byte[] rootHash) {
        this.rootHash = Arrays.copyOf(rootHash, 32);
    }
    
    /**
     * Gets the file hash.
     * 
     * @return The file hash
     */
    public byte[] getFileHash() {
        return fileHash;
    }
    
    /**
     * Sets the file hash.
     * 
     * @param fileHash The file hash
     */
    public void setFileHash(byte[] fileHash) {
        this.fileHash = Arrays.copyOf(fileHash, 32);
    }
    
    @Override
    public String toString() {
        return "BlockIdExt{" +
                "workchain=" + workchain +
                ", shard=" + shard +
                ", seqno=" + seqno +
                ", rootHash=" + bytesToHex(rootHash) +
                ", fileHash=" + bytesToHex(fileHash) +
                '}';
    }
    
    /**
     * Converts a byte array to a hexadecimal string.
     * 
     * @param bytes The byte array to convert
     * @return The hexadecimal string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            hex.append(Character.forDigit((b >> 4) & 0xF, 16))
               .append(Character.forDigit((b & 0xF), 16));
        }
        return hex.toString().toLowerCase();
    }
}
