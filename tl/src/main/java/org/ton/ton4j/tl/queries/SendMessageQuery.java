package org.ton.ton4j.tl.queries;

import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Getter;
import org.ton.ton4j.tl.types.LiteServerQueryData;

@Builder
@Getter
public class SendMessageQuery implements LiteServerQueryData {
    private byte[] body;

    public String getQueryName() {
        return "liteServer.sendMessage body:bytes = liteServer.SendMsgStatus";
    }

    public byte[] getQueryData() {
        ByteBuffer buffer = ByteBuffer.allocate(4 + body.length);
        buffer.putInt(body.length);
        buffer.put(body);
        return buffer.array();
    }
}
