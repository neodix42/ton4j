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
 * msg_import_ihr$010
 *  msg:^(Message Any)
 *  transaction:^Transaction
 *  ihr_fee:Grams
 *  proof_created:^Cell = InMsg;
 */

// msg_export_new extends InMsg

public class InMsgImportIhr implements InMsg {
    Message msg;
    Transaction transaction;
    BigInteger ihrFee;
    Cell proofCreated;

    @Override
    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0b010, 3)
                .storeRef(Tlb.save(transaction))
                .storeCoins(ihrFee)
                .storeRef(proofCreated)
                .endCell();
    }
}
