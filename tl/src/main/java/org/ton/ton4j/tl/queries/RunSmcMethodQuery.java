package org.ton.ton4j.tl.queries;

import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Getter;
import org.ton.ton4j.tl.types.BlockIdExt;
import org.ton.ton4j.tl.types.LiteServerQueryData;

@Builder
@Getter
public class RunSmcMethodQuery implements LiteServerQueryData {
    private int mode;
    private BlockIdExt id;
    private byte[] account;
    private long methodId;
    private byte[] params;

    public String getQueryName() {
        return "liteServer.runSmcMethod mode:# id:tonNode.blockIdExt account:liteServer.accountId method_id:long params:bytes = liteServer.RunMethodResult";
    }

    public byte[] getQueryData() {
        ByteBuffer buffer = ByteBuffer.allocate(
            BlockIdExt.getSize() + 4 + account.length + 8 + 4 + params.length
        );
        buffer.putInt(mode);
        buffer.put(id.serialize());
        buffer.putInt(account.length);
        buffer.put(account);
        buffer.putLong(methodId);
        buffer.putInt(params.length);
        buffer.put(params);
        return buffer.array();
    }
}
