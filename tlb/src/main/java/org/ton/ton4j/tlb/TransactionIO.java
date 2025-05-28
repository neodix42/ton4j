package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.TonHashMapE;

/**
 *
 *
 * <pre>
 * ^[
 *   in_msg:(Maybe ^(Message Any))
 *   out_msgs:(HashmapE 15 ^(Message Any))
 *  ]
 *  </pre>
 */
@Builder
@Data
public class TransactionIO implements Serializable {
  Message in;
  TonHashMapE out;

  public Cell toCell() {

    Cell dictCell =
        out.serialize(
            k -> CellBuilder.beginCell().storeUint((BigInteger) k, 15).endCell().getBits(),
            v -> CellBuilder.beginCell().storeRef(((Message) v).toCell()).endCell());
    return CellBuilder.beginCell().storeRefMaybe(in.toCell()).storeDict(dictCell).endCell();
  }

  public List<Message> getOutMessages() {
    List<Message> msgs = new ArrayList<>();
    for (Map.Entry<Object, Object> entry : out.elements.entrySet()) {
      msgs.add((Message) entry.getValue());
    }
    return msgs;
  }
}
