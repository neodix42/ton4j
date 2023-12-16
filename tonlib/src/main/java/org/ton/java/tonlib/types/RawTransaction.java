package org.ton.java.tonlib.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.ton.java.tonlib.base.TypedAsyncObject;

import java.util.List;

@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RawTransaction extends TypedAsyncObject {
    private long utime;
    private String data;
    private LastTransactionId transaction_id;
    private String fee;
    private String storage_fee;
    private String other_fee;
    private RawMessage in_msg;
    private List<RawMessage> out_msgs;

    @Override
    public String getTypeName() {
        return "raw.transaction";
    }
}