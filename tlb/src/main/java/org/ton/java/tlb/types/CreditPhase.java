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
public class CreditPhase {
    BigInteger dueFeesCollected;       // `tlb:"maybe ."`
    CurrencyCollection credit;             // `tlb:"."`
}
