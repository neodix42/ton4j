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
 _ enqueued_lt:uint64 out_msg:^MsgEnvelope = EnqueuedMsg;
 */
public class EnqueuedMsg implements InMsg {
    BigInteger enqueuedLt;
    MsgEnvelope outMsg;

    @Override
    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(enqueuedLt, 64)
                .storeRef(outMsg.toCell())
                .endCell();
    }
}
