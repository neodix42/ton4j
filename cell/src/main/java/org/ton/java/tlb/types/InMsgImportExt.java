package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 * <pre>
 * msg_import_ext$000
 * msg:^(Message Any)
 * transaction:^Transaction  = InMsg;
 * </pre>
 */
@Builder
@Data
public class InMsgImportExt implements InMsg {
    Message msg;
    Transaction transaction;

    @Override
    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0b000, 3)
                .storeRef(msg.toCell())
                .storeRef(transaction.toCell())
                .endCell();
    }

    public static InMsgImportExt deserialize(CellSlice cs) {
        return InMsgImportExt.builder()
                .msg(Message.deserialize(CellSlice.beginParse(cs.loadRef())))
                .transaction(Transaction.deserialize(CellSlice.beginParse(cs.loadRef())))
                .build();
    }
}
