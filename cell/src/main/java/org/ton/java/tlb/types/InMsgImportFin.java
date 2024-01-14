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
 * msg_import_fin$100
 *   in_msg:^MsgEnvelope
 *   transaction:^Transaction
 *   fwd_fee:Grams = InMsg;
 */

// msg_export_new extends InMsg

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
}
