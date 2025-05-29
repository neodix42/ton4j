package org.ton.java.adnl.liteclient.tl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * liteServer.getAccountState id:tonNode.blockIdExt account:liteServer.accountId = liteServer.AccountState;
 */
public class GetAccountState extends LiteServerQuery {
    
    // TL constructor ID for liteServer.getAccountState
    private static final int CONSTRUCTOR_ID = 0x321a30c3;
    
    private final byte[] blockRootHash;
    private final byte[] blockFileHash;
    private final int workchain;
    private final long shard;
    private final int seqno;
    private final int accountWorkchain;
    private final byte[] accountAddress; // 32 bytes
    
    public GetAccountState(byte[] blockRootHash, byte[] blockFileHash, 
                          int workchain, long shard, int seqno,
                          int accountWorkchain, byte[] accountAddress) {
        this.blockRootHash = blockRootHash;
        this.blockFileHash = blockFileHash;
        this.workchain = workchain;
        this.shard = shard;
        this.seqno = seqno;
        this.accountWorkchain = accountWorkchain;
        this.accountAddress = accountAddress;
    }
    
    @Override
    public int getConstructorId() {
        return CONSTRUCTOR_ID;
    }
    
    @Override
    protected void serializeData(ByteArrayOutputStream baos) throws IOException {
        // Serialize tonNode.blockIdExt
        writeInt32(baos, workchain);
        writeInt64(baos, shard);
        writeInt32(baos, seqno);
        writeInt256(baos, blockRootHash);
        writeInt256(baos, blockFileHash);
        
        // Serialize liteServer.accountId
        writeInt32(baos, accountWorkchain);
        writeInt256(baos, accountAddress);
    }
}
