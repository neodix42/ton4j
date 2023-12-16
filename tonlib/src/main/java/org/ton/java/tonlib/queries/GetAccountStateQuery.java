package org.ton.java.tonlib.queries;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.ton.java.tonlib.base.TypedAsyncObject;
import org.ton.java.tonlib.types.AccountAddress;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetAccountStateQuery extends TypedAsyncObject {
    private AccountAddress account_address;
    @Override
    public String getTypeObjectName() {
        return "getAccountState";
    }
}