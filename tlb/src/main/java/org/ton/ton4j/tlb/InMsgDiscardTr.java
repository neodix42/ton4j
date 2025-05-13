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
 * msg_discard_tr$111
 * in_msg:^MsgEnvelope
 * transaction_id:uint64
 * fwd_fee:Grams
 * proof_delivered:^Cell = InMsg;
 * </pre>
 */
@Builder
@Data
public class InMsgDiscardTr implements InMsg, Serializable {
  MsgEnvelope inMsg;
  BigInteger transactionId;
  BigInteger fwdFee;
  Cell proofDelivered;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0b100, 3)
        .storeRef(inMsg.toCell())
        .storeUint(transactionId, 64)
        .storeCoins(fwdFee)
        .storeRef(proofDelivered)
        .endCell();
  }

  public static InMsgDiscardTr deserialize(CellSlice cs) {
    return InMsgDiscardTr.builder()
        .inMsg(MsgEnvelope.deserialize(CellSlice.beginParse(cs.loadRef())))
        .transactionId(cs.loadUint(64))
        .fwdFee(cs.loadCoins())
        .proofDelivered(cs.loadRef())
        .build();
  }
}
