package org.ton.java.tonlib.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.ton.java.tonlib.base.TypedAsyncObject;

@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RawMessage extends TypedAsyncObject {
    private AccountAddressOnly source;
    private AccountAddressOnly destination;
    private String value;
    private String message;
    private String fwd_fee;
    private String ihr_fee;
    private long created_lt;
    private String body_hash;
    private MsgData msg_data;

    @Override
    public String getTypeName() {
        return "raw.message";
    }
}