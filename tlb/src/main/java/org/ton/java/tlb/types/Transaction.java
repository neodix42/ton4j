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

    public void dump() {
        //todo
    }
}
