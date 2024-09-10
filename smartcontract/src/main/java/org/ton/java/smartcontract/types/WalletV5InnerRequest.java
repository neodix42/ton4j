package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.tlb.types.ActionList;
import org.ton.java.tlb.types.OutList;

import static java.util.Objects.nonNull;

/**
 * <pre>
 * actions$_ out_actions:(Maybe OutList) has_other_actions:(## 1) {m:#} {n:#} other_actions:(ActionList n m) = InnerRequest;
 * </pre>
 */
@Builder
@Data
public class WalletV5InnerRequest {

    OutList outActions;
    boolean hasOtherActions;
    ActionList otherActions;

    public Cell toCell() {
        if (hasOtherActions) {
            return CellBuilder.beginCell()
                    .storeRefMaybe(outActions.toCell())
                    .storeBit(hasOtherActions)
                    .storeCell(otherActions.toCell())
                    .endCell();
        } else {
            return CellBuilder.beginCell()
                    .storeRefMaybe(outActions.toCell())
                    .storeBit(hasOtherActions)
                    .endCell();
        }
    }

    public static WalletV5InnerRequest deserialize(CellSlice cs) {
        if (nonNull(cs.preloadMaybeRefX())) {
            return WalletV5InnerRequest.builder()
                    .outActions(OutList.deserialize(CellSlice.beginParse(cs.loadMaybeRefX())))
                    .hasOtherActions(cs.loadBit())
                    .otherActions(ActionList.deserialize(CellSlice.beginParse(cs)))
                    .build();
        } else {
            cs.loadBit();
            return WalletV5InnerRequest.builder()
                    .outActions(OutList.builder().build())
                    .hasOtherActions(cs.loadBit())
                    .otherActions(ActionList.deserialize(CellSlice.beginParse(cs)))
                    .build();
        }
    }
}
