package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.cell.*;

/**
 *
 *
 * <pre>
 * _ (HashmapAugE 256 OutMsg CurrencyCollection) = OutMsgDescr;
 * </pre>
 */
@Builder
@Data
@Slf4j
public class OutMsgDescr implements Serializable {
  TonHashMapAugE outMsg;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeDict(
            outMsg.serialize(
                k -> CellBuilder.beginCell().storeUint((BigInteger) k, 256).endCell().getBits(),
                v -> ((OutMsg) v).toCell(),
                e -> ((CurrencyCollection) e).toCell(),
                (fk, fv) -> CellBuilder.beginCell().storeUint(0, 1) // todo
                ))
        .endCell();
  }

  public static OutMsgDescr deserialize(CellSlice cs) {
    return OutMsgDescr.builder()
        .outMsg(
            cs.loadDictAugE(
                256, k -> k.readUint(256), OutMsg::deserialize, CurrencyCollection::deserialize))
        .build();
  }

  public long getCount() {
    return outMsg.elements.size();
  }

  public List<OutMsg> getOutMessages() {
    List<OutMsg> outMsgs = new ArrayList<>();
    for (Map.Entry<Object, ValueExtra> entry : outMsg.elements.entrySet()) {
      outMsgs.add((OutMsg) entry.getValue().getValue());
    }
    return outMsgs;
  }
}
