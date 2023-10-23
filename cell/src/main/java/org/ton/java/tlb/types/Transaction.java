package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 * transaction$0111
 *   account_addr:bits256
 *   lt:uint64
 *   prev_trans_hash:bits256
 *   prev_trans_lt:uint64
 *   now:uint32
 *   outmsg_cnt:uint15
 *   orig_status:AccountStatus
 *   end_status:AccountStatus
 *   ^[
 *     in_msg:(Maybe ^(Message Any))
 *     out_msgs:(HashmapE 15 ^(Message Any))
 *     ]
 *   total_fees:CurrencyCollection state_update:^(HASH_UPDATE Account)
 *   description:^TransactionDescr = Transaction;
 */
public class Transaction {
    int magic;
    BigInteger accountAddr;
    BigInteger lt;
    BigInteger prevTxHash;
    BigInteger prevTxLt;
    long now;
    long outMsgCount;
    String origStatus;
    String endStatus;
    TransactionIO inOut;
    CurrencyCollection totalFees;
    HashUpdate stateUpdate;
    TransactionDescription description;

    // not in scheme, but might be filled based on request data for flexibility
    byte[] hash;

    private String getMagic() {
        return Long.toBinaryString(magic);
    }

    private String getAccountAddr() {
        return accountAddr.toString(16);
    }

    private String getPrevTxHash() {
        return prevTxHash.toString(16);
    }

    public void dump() {
        //todo
    }
}
