package org.ton.java.tonlib.queries;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.ton.java.tonlib.base.TypedAsyncObject;
import org.ton.java.tonlib.types.AccountAddressOnly;
import org.ton.java.tonlib.types.LastTransactionId;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetRawTransactionsQuery extends TypedAsyncObject {
    private AccountAddressOnly account_address;
    private LastTransactionId from_transaction_id;

    @Override
    public String getTypeObjectName() {
        return "raw.getTransactions";
    }
}
