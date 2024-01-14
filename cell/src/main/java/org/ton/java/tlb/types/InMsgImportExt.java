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
 * msg_import_ext$000
 *  msg:^(Message Any)
 *  transaction:^Transaction  = InMsg;
 */
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
}
