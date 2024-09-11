package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

/**
 * <pre>
 * msg_import_fin$100
 *   in_msg:^MsgEnvelope
 *   transaction:^Transaction
 *   fwd_fee:Grams = InMsg;
 *   </pre>
 */
@Builder
@Data
public class InMsgImportFin implements InMsg {
    MsgEnvelope inMsg;
    Transaction transaction;
    BigInteger fwdFee;

    @Override
    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0b100, 3)
                .storeRef(inMsg.toCell())
                .storeRef(transaction.toCell())
                .storeCoins(fwdFee)
                .endCell();
    }

    public static InMsgImportFin deserialize(CellSlice cs) {
        return InMsgImportFin.builder()
                .inMsg(MsgEnvelope.deserialize(CellSlice.beginParse(cs.loadRef())))
                .transaction(Transaction.deserialize(CellSlice.beginParse(cs.loadRef())))
                .fwdFee(cs.loadCoins())
                .build();
    }
}
