package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;

import static org.ton.java.utils.Utils.log2;

@Builder
@Getter
@Setter
@ToString
/**
 * depth_balance$_ split_depth:(#<= 30) balance:CurrencyCollection = DepthBalanceInfo;
 */
public class DepthBalanceInfo {
    int depth;
    CurrencyCollection currencies;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(depth, (int) Math.ceil(log2((depth))))
                .storeCell(currencies.toCell()).endCell();
    }
}
