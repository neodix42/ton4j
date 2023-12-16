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
public class Fees extends TypedAsyncObject {
    private long in_fwd_fee;
    private long storage_fee;
    private long gas_fee;
    private long fwd_fee;

    @Override
    public String getTypeObjectName() {
        return "fees";
    }
}

