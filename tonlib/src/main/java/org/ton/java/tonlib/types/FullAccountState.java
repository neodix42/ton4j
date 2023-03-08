package org.ton.java.tonlib.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Builder
@Setter
@Getter
@ToString
public class FullAccountState implements Serializable {
    AccountAddressOnly address;
    String balance;
    LastTransactionId last_transaction_id;
    BlockIdExt block_id;
    long sync_utime;
    AccountState account_state;
}
