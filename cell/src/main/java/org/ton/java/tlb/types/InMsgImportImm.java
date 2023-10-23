package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.tlb.loader.Tlb;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 * msg_import_imm$011 in_msg:^MsgEnvelope transaction:^Transaction fwd_fee:Grams = InMsg;
 */
public class InMsgImportImm implements InMsg {
    MsgEnvelope inMsg;
    Transaction transaction;
    BigInteger fwdFee;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0b011, 3) //magic
                .storeRef(Tlb.save(MsgEnvelope.class, inMsg))
                .storeRef(Tlb.save(Transaction.class, transaction))
                .storeUint(fwdFee, 32)
                .endCell();
    }
}
