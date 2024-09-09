package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.tlb.types.ActionList;
import org.ton.java.tlb.types.OutList;

/**
 * <pre>
 * actions$_ out_actions:(Maybe OutList) has_other_actions:(## 1) {m:#} {n:#} other_actions:(ActionList n m) = InnerRequest;
 * </pre>
 */
@Builder
@Getter
@Setter
@ToString

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
        return WalletV5InnerRequest.builder()
                .outActions(OutList.deserialize(CellSlice.beginParse(cs.loadMaybeRefX())))
                .hasOtherActions(cs.loadBit())
                .otherActions(ActionList.deserialize(CellSlice.beginParse(cs.loadRef())))
                .build();
    }
}
