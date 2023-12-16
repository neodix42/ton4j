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
public class RawTransactions extends TypedAsyncObject {
    private List<RawTransaction> transactions;
    private LastTransactionId previous_transaction_id;

    @Override
    public String getTypeObjectName() {
        return "raw.transactions";
    }
}