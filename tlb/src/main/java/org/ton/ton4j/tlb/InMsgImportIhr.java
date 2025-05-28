package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 * msg_import_ihr$010
 *  msg:^(Message Any)
 *  transaction:^Transaction
 *  ihr_fee:Grams
 *  proof_created:^Cell = InMsg;
 *  </pre>
 */
@Builder
@Data
public class InMsgImportIhr implements InMsg, Serializable {
  Message msg;
  Transaction transaction;
  BigInteger ihrFee;
  Cell proofCreated;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0b010, 3)
        .storeRef(transaction.toCell())
        .storeCoins(ihrFee)
        .storeRef(proofCreated)
        .endCell();
  }

  public static InMsgImportIhr deserialize(CellSlice cs) {
    return InMsgImportIhr.builder()
        .msg(Message.deserialize(CellSlice.beginParse(cs.loadRef())))
        .transaction(Transaction.deserialize(CellSlice.beginParse(cs.loadRef())))
        .ihrFee(cs.loadCoins())
        .proofCreated(cs.loadRef())
        .build();
  }
}
