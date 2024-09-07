package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

/**
 * <pre>
 * msg_import_tr$101
 *  in_msg:^MsgEnvelope
 *  out_msg:^MsgEnvelope
 *  transit_fee:Grams = InMsg;
 *  </pre>
 */
@Builder
@Getter
@Setter
@ToString

public class InMsgImportTr implements InMsg {
    MsgEnvelope inMsg;
    MsgEnvelope outMsg;
    BigInteger transitFee;

    @Override
    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0b101, 3)
                .storeRef(inMsg.toCell())
                .storeRef(outMsg.toCell())
                .storeCoins(transitFee)
                .endCell();
    }

    public static InMsgImportTr deserialize(CellSlice cs) {
        return InMsgImportTr.builder()
                .inMsg(MsgEnvelope.deserialize(CellSlice.beginParse(cs.loadRef())))
                .outMsg(MsgEnvelope.deserialize(CellSlice.beginParse(cs.loadRef())))
                .transitFee(cs.loadCoins())
                .build();
    }
}
