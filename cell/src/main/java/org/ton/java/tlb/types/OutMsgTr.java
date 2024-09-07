package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 * <pre>
 * msg_export_tr$011  out_msg:^MsgEnvelope
 * imported:^InMsg = OutMsg;
 * </pre>
 */
@Builder
@Getter
@Setter
@ToString

public class OutMsgTr implements OutMsg {
    int magic;
    MsgEnvelope outMsg;
    InMsg imported;

    @Override
    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0b011, 3)
                .storeRef(outMsg.toCell())
                .storeRef(imported.toCell())
                .endCell();
    }

    public static OutMsgTr deserialize(CellSlice cs) {
        return OutMsgTr.builder()
                .magic(cs.loadUint(3).intValue())
                .outMsg(MsgEnvelope.deserialize(CellSlice.beginParse(cs.loadRef())))
                .imported(InMsg.deserialize(CellSlice.beginParse(cs.loadRef())))
                .build();
    }
}
