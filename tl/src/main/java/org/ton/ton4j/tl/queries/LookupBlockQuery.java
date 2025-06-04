package org.ton.ton4j.tl.queries;

import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Getter;
import org.ton.ton4j.tl.types.BlockId;
import org.ton.ton4j.tl.types.LiteServerQueryData;

@Builder
@Getter
public class LookupBlockQuery implements LiteServerQueryData {
    private int mode;
    private BlockId id;
    private Long lt;
    private Integer utime;

    public String getQueryName() {
        return "liteServer.lookupBlock mode:# id:tonNode.blockId lt:mode.1?long utime:mode.2?int = liteServer.BlockHeader";
    }

    public byte[] getQueryData() {
        int size = BlockId.getSize() + 4;
        if ((mode & 1) != 0) size += 8; // lt
        if ((mode & 2) != 0) size += 4; // utime
        
        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.putInt(mode);
        buffer.put(id.serialize());
        
        if ((mode & 1) != 0) {
            buffer.putLong(lt);
        }
        
        if ((mode & 2) != 0) {
            buffer.putInt(utime);
        }
        
        return buffer.array();
    }
}
