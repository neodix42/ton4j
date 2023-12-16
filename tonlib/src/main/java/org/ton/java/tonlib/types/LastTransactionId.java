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
public class LastTransactionId extends TypedAsyncObject {
    private BigInteger lt;
    private String hash;

    @Override
    public String getTypeName() {
        return "internal.transactionId";
    }
}

