package org.ton.ton4j.tl.queries;

import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Getter;
import org.ton.ton4j.tl.types.BlockIdExt;
import org.ton.ton4j.tl.types.LiteServerQueryData;

@Builder
@Getter
public class BlockProofQuery implements LiteServerQueryData {
    private int mode;
    private BlockIdExt knownBlock;
    private BlockIdExt targetBlock;

    public String getQueryName() {
        return "liteServer.getBlockProof mode:# known_block:tonNode.blockIdExt target_block:mode.0?tonNode.blockIdExt = liteServer.PartialBlockProof";
    }

    public byte[] getQueryData() {
        int size = 4 + BlockIdExt.getSize();
        if ((mode & 1) != 0 && targetBlock != null) size += BlockIdExt.getSize();
        
        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.putInt(mode);
        buffer.put(knownBlock.serialize());
        
        if ((mode & 1) != 0 && targetBlock != null) {
            buffer.put(targetBlock.serialize());
        }
        
        return buffer.array();
    }
}
