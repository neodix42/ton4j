package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;

@Builder
@Getter
@Setter
@ToString
/**
 msg_export_tr$011  out_msg:^MsgEnvelope
 imported:^InMsg = OutMsg;
 */
public class OutMsgTr implements OutMsg {
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
}
