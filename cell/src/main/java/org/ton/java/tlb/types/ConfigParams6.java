package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;
import java.util.Currency;

@Builder
@Getter
@Setter
@ToString
public class ConfigParams6 {
    BigInteger mintNewPrice;
    BigInteger mintAddPrice;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeCoins(mintNewPrice)
                .storeCoins(mintAddPrice)
                .endCell();
    }

    public static ConfigParams6 deserialize(CellSlice cs) {
        return ConfigParams6.builder()
                .mintNewPrice(cs.loadCoins())
                .mintAddPrice(cs.loadCoins())
                .build();
    }
}
