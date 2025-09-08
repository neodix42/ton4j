package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.*;

/**
 *
 *
 * <pre>
 *  messages:(HashmapE 64 EnqueuedMsg) count:uint48 = AccountDispatchQueue;
 *  </pre>
 */
@Builder
@Data
public class AccountDispatchQueue implements Serializable {
  TonHashMapE messages;
  Long count;

  public Cell toCell() {
    Cell dict =
        messages.serialize(
            k -> CellBuilder.beginCell().storeUint((BigInteger) k, 64).endCell().getBits(),
            v -> CellBuilder.beginCell().storeCell(((EnqueuedMsg) v).toCell()).endCell());

    return CellBuilder.beginCell().storeDict(dict).storeUint(count, 48).endCell();
  }

  public static AccountDispatchQueue deserialize(CellSlice cs) {

    return AccountDispatchQueue.builder()
        .messages(
            cs.loadDictE(
                64, k -> k.readUint(64), v -> EnqueuedMsg.deserialize(CellSlice.beginParse(v))))
        .count(cs.loadUint(48).longValue())
        .build();
  }
}
