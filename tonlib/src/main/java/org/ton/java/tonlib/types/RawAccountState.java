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
public class RawAccountState extends TypedAsyncObject {
    private String balance;
    private String code;
    private String data;
    private LastTransactionId last_transaction_id;
    private long sync_utime;

    @Override
    public String getTypeName() {
        return "raw.accountState";
    }
}
