package org.ton.ton4j.tl.queries;

import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Getter;
import org.ton.ton4j.tl.types.BlockIdExt;
import org.ton.ton4j.tl.types.LiteServerQueryData;

@Builder
@Getter
public class OneTransactionQuery implements LiteServerQueryData {
    private BlockIdExt id;
    private byte[] account;
    private long lt;

    public String getQueryName() {
        return "liteServer.getOneTransaction id:tonNode.blockIdExt account:liteServer.accountId lt:long = liteServer.TransactionInfo";
    }

    public byte[] getQueryData() {
        ByteBuffer buffer = ByteBuffer.allocate(BlockIdExt.getSize() + 4 + account.length + 8);
        buffer.put(id.serialize());
        buffer.putInt(account.length);
        buffer.put(account);
        buffer.putLong(lt);
        return buffer.array();
    }
}
