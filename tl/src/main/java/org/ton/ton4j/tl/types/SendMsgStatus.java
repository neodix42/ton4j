package org.ton.ton4j.tl.types;

import java.io.Serializable;
import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

@Data
@Builder
public class SendMsgStatus implements Serializable, LiteServerAnswer {
    private int status;
    
    public static final int constructorId = 
        (int) Utils.getQueryCrc32IEEEE("liteServer.sendMsgStatus status:int = liteServer.SendMsgStatus");
    
    public static SendMsgStatus deserialize(ByteBuffer buffer) {
        int status = buffer.getInt();
        return SendMsgStatus.builder()
            .status(status)
            .build();
    }
    
    public static SendMsgStatus deserialize(byte[] data) {
        return deserialize(ByteBuffer.wrap(data));
    }
}
