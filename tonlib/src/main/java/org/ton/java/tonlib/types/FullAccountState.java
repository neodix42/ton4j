package org.ton.java.tonlib.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FullAccountState {
    private AccountAddressOnly address;
    private String balance;
    private LastTransactionId last_transaction_id;
    private BlockIdExt block_id;
    private long sync_utime;
    private AccountState account_state;
}
