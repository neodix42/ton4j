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
 * msg_discard_fin$110
 * in_msg:^MsgEnvelope
 * transaction_id:uint64
 * fwd_fee:Grams = InMsg;
 * </pre>
 */
@Builder
@Getter
@Setter
@ToString

public class InMsgDiscardFin implements InMsg {
    MsgEnvelope inMsg;
    BigInteger transactionId;
    BigInteger fwdFee;

    @Override
    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0b110, 3)
                .storeRef(inMsg.toCell())
                .storeUint(transactionId, 64)
                .storeCoins(fwdFee)
                .endCell();
    }

    public static InMsgDiscardFin deserialize(CellSlice cs) {
        return InMsgDiscardFin.builder()
                .inMsg(MsgEnvelope.deserialize(CellSlice.beginParse(cs.loadRef())))
                .transactionId(cs.loadUint(64))
                .fwdFee(cs.loadCoins())
                .build();
    }
}
