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
public class AccountTransactionId extends TypedAsyncObject {
    private String account; //after_hash
    private long lt; //after_lt

    @Override
    public String getTypeName() {
        return "blocks.accountTransactionId";
    }
}

