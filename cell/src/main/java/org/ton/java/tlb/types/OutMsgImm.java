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
 msg_export_imm$010 out_msg:^MsgEnvelope
 transaction:^Transaction reimport:^InMsg = OutMsg;
 */
public class OutMsgImm implements OutMsg {
    MsgEnvelope msg;
    Transaction transaction;
    InMsg reimport;

    @Override
    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0b010, 3)
                .storeRef(msg.toCell())
                .storeRef(transaction.toCell())
                .storeRef(reimport.toCell())
                .endCell();
    }
}
