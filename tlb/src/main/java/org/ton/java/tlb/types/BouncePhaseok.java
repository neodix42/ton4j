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
public class BouncePhaseok {
    int magic; // `tlb:"$1"`
    StorageUsedShort msgSize; // `tlb:"."`
    BigInteger msgFees; //`tlb:"."`
    BigInteger fwdFees; //`tlb:"."`
}
