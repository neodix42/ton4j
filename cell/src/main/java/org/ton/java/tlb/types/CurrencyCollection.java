package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.TonHashMapE;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
public class CurrencyCollection {
    BigInteger coins;
    TonHashMapE extraCurrencies; // `tlb dict 32 32
    //todo dump hashmap
}
