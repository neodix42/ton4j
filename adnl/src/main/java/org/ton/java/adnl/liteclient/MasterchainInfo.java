package org.ton.java.adnl.liteclient;

/**
 * Represents masterchain information from liteserver
 */
public class MasterchainInfo {
    
    private final int workchain;
    private final long shard;
    private final int seqno;
    private final byte[] rootHash;
    private final byte[] fileHash;
    private final int unixTime;
    
    public MasterchainInfo(int workchain, long shard, int seqno, 
                          byte[] rootHash, byte[] fileHash, int unixTime) {
        this.workchain = workchain;
        this.shard = shard;
        this.seqno = seqno;
        this.rootHash = rootHash;
        this.fileHash = fileHash;
        this.unixTime = unixTime;
    }
    
    public int getWorkchain() {
        return workchain;
    }
    
    public long getShard() {
        return shard;
    }
    
    public int getSeqno() {
        return seqno;
    }
    
    public byte[] getRootHash() {
        return rootHash;
    }
    
    public byte[] getFileHash() {
        return fileHash;
    }
    
    public int getUnixTime() {
        return unixTime;
    }
    
    @Override
    public String toString() {
        return String.format("MasterchainInfo{workchain=%d, shard=%d, seqno=%d, unixTime=%d}", 
                           workchain, shard, seqno, unixTime);
    }
}
