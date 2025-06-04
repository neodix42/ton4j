package org.ton.ton4j.tl.queries;

import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Getter;
import org.ton.ton4j.tl.types.BlockIdExt;
import org.ton.ton4j.tl.types.LiteServerQueryData;
import org.ton.ton4j.tl.types.TransactionId3;

@Builder
@Getter
public class ListBlockTransactionsExtQuery implements LiteServerQueryData {
    private BlockIdExt id;
    private int mode;
    private int count;
    private TransactionId3 after;
    private Boolean reverseOrder;
    private Boolean wantProof;

    public String getQueryName() {
        return "liteServer.listBlockTransactionsExt id:tonNode.blockIdExt mode:# count:# after:mode.7?liteServer.transactionId3 reverse_order:mode.6?true want_proof:mode.5?true = liteServer.BlockTransactionsExt";
    }

    public byte[] getQueryData() {
        // Calculate size
        int size = BlockIdExt.getSize() + 4 + 4;
        if ((mode & 128) != 0 && after != null) size += TransactionId3.getSize(); // after
        if ((mode & 64) != 0 && reverseOrder != null) size += 1; // reverse_order
        if ((mode & 32) != 0 && wantProof != null) size += 1; // want_proof
        
        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.put(id.serialize());
        buffer.putInt(mode);
        buffer.putInt(count);
        
        if ((mode & 128) != 0 && after != null) {
            buffer.put(after.serialize());
        }
        
        if ((mode & 64) != 0 && reverseOrder != null) {
            buffer.put((byte) (reverseOrder ? 1 : 0));
        }
        
        if ((mode & 32) != 0 && wantProof != null) {
            buffer.put((byte) (wantProof ? 1 : 0));
        }
        
        return buffer.array();
    }
}
