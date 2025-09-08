package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.cell.TonHashMapAugE;

/**
 *
 *
 * <pre>
 *  (HashmapAugE 256 AccountDispatchQueue uint64) = DispatchQueue;
 *  </pre>
 */
@Builder
@Data
public class DispatchQueue implements Serializable {
  TonHashMapAugE messages;

  public Cell toCell() {
    Cell dict =
        messages.serialize(
            k -> CellBuilder.beginCell().storeUint((BigInteger) k, 256).endCell().getBits(),
            v -> CellBuilder.beginCell().storeCell(((AccountDispatchQueue) v).toCell()),
            e -> CellBuilder.beginCell().storeUint((Long) e, 64),
            (fk, fv) -> CellBuilder.beginCell().storeUint(0, 1) // todo
            );
    return CellBuilder.beginCell().storeDict(dict).endCell();
  }

  public static DispatchQueue deserialize(CellSlice cs) {

    return DispatchQueue.builder()
        .messages(
            cs.loadDictAugE(
                256,
                k -> k.readUint(256),
                v -> AccountDispatchQueue.deserialize(CellSlice.beginParse(v)),
                e -> e.loadUint(64)))
        .build();
  }
}
