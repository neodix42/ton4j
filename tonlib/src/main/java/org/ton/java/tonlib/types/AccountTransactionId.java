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
public class AccountTransactionId extends TypedAsyncObject {
    private String account; //after_hash
    private BigInteger lt; //after_lt

    public void setLt(String value){
        this.lt = new BigInteger(value);
    }

    @Override
    public String getTypeObjectName() {
        return "blocks.accountTransactionId";
    }
}

