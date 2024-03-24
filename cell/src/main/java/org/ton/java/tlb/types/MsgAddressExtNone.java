package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Getter
@Setter
/**
 * addr_none$00 = MsgAddressExt;
 * addr_extern$01 len:(## 9) external_address:(bits len) = MsgAddressExt;
 */
public class MsgAddressExtNone implements MsgAddressExt {

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0, 2)
                .endCell();
    }

    public MsgAddressExtNone deserialize(CellSlice cs) {
        cs.loadUint(2); // int flagMsg =
        return MsgAddressExtNone.builder().build();

    }
}
