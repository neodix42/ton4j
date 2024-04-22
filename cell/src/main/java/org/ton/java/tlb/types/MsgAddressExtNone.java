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
 */
public class MsgAddressExtNone implements MsgAddressExt {
    int magic;

    @Override
    public String toString() {
        return "";
    }

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0, 2)
                .endCell();
    }

    public static MsgAddressExtNone deserialize(CellSlice cs) {
        int magic = cs.loadUint(2).intValue();
        assert (magic == 0b00) : "MsgAddressExtNone: magic not equal to 0b00, found " + magic;

        return MsgAddressExtNone.builder()
                .magic(magic)
                .build();
    }
}