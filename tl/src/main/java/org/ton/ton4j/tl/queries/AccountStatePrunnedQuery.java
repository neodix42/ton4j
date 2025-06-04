package org.ton.ton4j.tl.queries;

import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Getter;
import org.ton.ton4j.tl.types.BlockIdExt;
import org.ton.ton4j.tl.types.LiteServerQueryData;

@Builder
@Getter
public class AccountStatePrunnedQuery implements LiteServerQueryData {
    private BlockIdExt id;
    private byte[] account;

    public String getQueryName() {
        return "liteServer.getAccountStatePrunned id:tonNode.blockIdExt account:liteServer.accountId = liteServer.AccountState";
    }

    public byte[] getQueryData() {
        ByteBuffer buffer = ByteBuffer.allocate(BlockIdExt.getSize() + 4 + account.length);
        buffer.put(id.serialize());
        buffer.putInt(account.length);
        buffer.put(account);
        return buffer.array();
    }
}
