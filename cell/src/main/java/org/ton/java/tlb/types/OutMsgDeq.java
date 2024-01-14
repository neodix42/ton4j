package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 msg_export_deq$1100 out_msg:^MsgEnvelope
 import_block_lt:uint63 = OutMsg;
 */
public class OutMsgDeq implements OutMsg {
    MsgEnvelope outMsg;
    BigInteger importBlockLt;

    @Override
    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0b1100, 4)
                .storeRef(outMsg.toCell())
                .storeUint(importBlockLt, 63)
                .endCell();
    }
}
