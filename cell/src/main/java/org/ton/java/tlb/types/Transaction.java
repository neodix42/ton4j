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
    int magic; //         `tlb:"$0111"`
    int[] accountAddr; //        `tlb:"bits 256"`
    BigInteger lt; //        `tlb:"## 64"`
    int[] prevTxHash; //       `tlb:"bits 256"`
    BigInteger prevTxLT; //        `tlb:"## 64"`
    long now; //        `tlb:"## 32"`
    long outMsgCount; //        `tlb:"## 15"`
    String origStatus; // `tlb:"."`
    String endStatus; // `tlb:"."`
    TransactionIO inOut; // `tlb:"^"`
    CurrencyCollection totalFees; //     `tlb:"."`
    HashUpdate stateUpdate; //             `tlb:"^"` // of Account
    TransactionDescription description;// `tlb:"^"`

    // not in scheme, but will be filled based on request data for flexibility
    byte[] hash; //  `tlb:"-"`

    private String getMagic() {
        return Long.toBinaryString(magic);
    }

    public void dump() {
        //todo
    }
}
