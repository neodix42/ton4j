package org.ton.ton4j.tl.queries;

import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Getter;
import org.ton.ton4j.tl.types.LiteServerQueryData;

@Builder
@Getter
public class MasterchainInfoExtQuery implements LiteServerQueryData {
    private int mode;

    public String getQueryName() {
        return "liteServer.getMasterchainInfoExt mode:# = liteServer.MasterchainInfoExt";
    }

    public byte[] getQueryData() {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(mode);
        return buffer.array();
    }
}
