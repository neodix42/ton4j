package org.ton.java.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapAugE;

/**
 *
 *
 * <pre>
 * _ (HashmapAugE 256 OutMsg CurrencyCollection) = OutMsgDescr;
 * </pre>
 */
@Builder
@Data
public class OutMsgDescr implements Serializable {
  TonHashMapAugE outMsg;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeDict(
            outMsg.serialize(
                k -> CellBuilder.beginCell().storeUint((BigInteger) k, 256).endCell().getBits(),
                v -> CellBuilder.beginCell().storeCell(((OutMsg) v).toCell()),
                e -> CellBuilder.beginCell().storeCell(((CurrencyCollection) e).toCell()),
                (fk, fv) -> CellBuilder.beginCell().storeUint(0, 1) // todo
                ))
        .endCell();
  }

  public static OutMsgDescr deserialize(CellSlice cs) {
    return OutMsgDescr.builder()
        .outMsg(
            cs.loadDictAugE(
                256,
                k -> k.readInt(256),
                v -> OutMsg.deserialize(v),
                e -> CurrencyCollection.deserialize(e)))
        .build();
  }

  public long getCount() {
    return outMsg.elements.size();
  }

  public List<OutMsg> getOutMessages() {
    List<OutMsg> outMsgs = new ArrayList<>();
    for (Map.Entry<Object, Pair<Object, Object>> entry : outMsg.elements.entrySet()) {
      outMsgs.add((OutMsg) entry.getValue().getLeft());
    }
    return outMsgs;
  }
}
