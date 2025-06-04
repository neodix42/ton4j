package org.ton.ton4j.tl.types;

import java.io.Serializable;
import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

@Data
@Builder
public class RunMethodResult implements Serializable, LiteServerAnswer {
    private int mode;
    private BlockIdExt id;
    private BlockIdExt shardblk;
    private byte[] shardProof;
    private byte[] proof;
    private byte[] stateProof;
    private byte[] initC7;
    private byte[] libExtras;
    private int exitCode;
    private byte[] result;
    
    public static final int constructorId = 
        (int) Utils.getQueryCrc32IEEEE("liteServer.runMethodResult mode:# id:tonNode.blockIdExt shardblk:tonNode.blockIdExt shard_proof:maybe(bytes) proof:maybe(bytes) state_proof:maybe(bytes) init_c7:maybe(bytes) lib_extras:maybe(bytes) exit_code:int result:bytes = liteServer.RunMethodResult");
    
    public static RunMethodResult deserialize(ByteBuffer buffer) {
        int mode = buffer.getInt();
        BlockIdExt id = BlockIdExt.deserialize(buffer);
        BlockIdExt shardblk = BlockIdExt.deserialize(buffer);
        
        byte[] shardProof = null;
        if ((mode & 1) != 0) {
            int shardProofLength = buffer.getInt();
            shardProof = new byte[shardProofLength];
            buffer.get(shardProof);
        }
        
        byte[] proof = null;
        if ((mode & 2) != 0) {
            int proofLength = buffer.getInt();
            proof = new byte[proofLength];
            buffer.get(proof);
        }
        
        byte[] stateProof = null;
        if ((mode & 4) != 0) {
            int stateProofLength = buffer.getInt();
            stateProof = new byte[stateProofLength];
            buffer.get(stateProof);
        }
        
        byte[] initC7 = null;
        if ((mode & 8) != 0) {
            int initC7Length = buffer.getInt();
            initC7 = new byte[initC7Length];
            buffer.get(initC7);
        }
        
        byte[] libExtras = null;
        if ((mode & 16) != 0) {
            int libExtrasLength = buffer.getInt();
            libExtras = new byte[libExtrasLength];
            buffer.get(libExtras);
        }
        
        int exitCode = buffer.getInt();
        int resultLength = buffer.getInt();
        byte[] result = new byte[resultLength];
        buffer.get(result);
        
        return RunMethodResult.builder()
            .mode(mode)
            .id(id)
            .shardblk(shardblk)
            .shardProof(shardProof)
            .proof(proof)
            .stateProof(stateProof)
            .initC7(initC7)
            .libExtras(libExtras)
            .exitCode(exitCode)
            .result(result)
            .build();
    }
    
    public static RunMethodResult deserialize(byte[] data) {
        return deserialize(ByteBuffer.wrap(data));
    }
}
