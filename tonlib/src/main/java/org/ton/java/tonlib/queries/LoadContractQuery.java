package org.ton.java.tonlib.queries;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.ton.java.tonlib.base.TypedAsyncObject;
import org.ton.java.tonlib.types.AccountAddressOnly;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoadContractQuery extends TypedAsyncObject {
    private AccountAddressOnly account_address;

    @Override
    public String getTypeName() {
        return "smc.load";
    }
}