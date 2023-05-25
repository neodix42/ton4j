package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.TonHashMap;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
public class CurrencyCollection {
    BigInteger coins;
    TonHashMap extraCurrencies;
}
