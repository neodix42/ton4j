package org.ton.java.tonlib.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.ton.java.tonlib.base.TypedAsyncObject;

import java.math.BigInteger;

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
    private BigInteger created_lt;
    private String body_hash;
    private MsgData msg_data;

    public void setCreated_lt(String value){
        this.created_lt = new BigInteger(value);
    }

    @Override
    public String getTypeObjectName() {
        return "raw.message";
    }
}