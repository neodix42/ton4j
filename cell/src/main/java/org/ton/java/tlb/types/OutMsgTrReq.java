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
 msg_export_tr_req$111 out_msg:^MsgEnvelope
 imported:^InMsg = OutMsg;
 */
public class OutMsgTrReq implements OutMsg {
    MsgEnvelope msg;
    InMsg imported;

    @Override
    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0b111, 3)
                .storeRef(msg.toCell())
                .storeRef(imported.toCell())
                .endCell();
    }
}
