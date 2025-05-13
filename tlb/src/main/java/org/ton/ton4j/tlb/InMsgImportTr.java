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
 * msg_import_tr$101
 *  in_msg:^MsgEnvelope
 *  out_msg:^MsgEnvelope
 *  transit_fee:Grams = InMsg;
 *  </pre>
 */
@Builder
@Data
public class InMsgImportTr implements InMsg, Serializable {
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
