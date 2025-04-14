package org.ton.java.tlb;

import static java.util.Objects.isNull;

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
 * _ (HashmapAugE 32 KeyExtBlkRef KeyMaxLt) = OldMcBlocksInfo;
 * </pre>
 */
@Builder
@Data
public class OldMcBlocksInfo implements Serializable {
  TonHashMapAugE list;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeDict(
            list.serialize(
                k -> CellBuilder.beginCell().storeUint((BigInteger) k, 32).endCell().getBits(),
                v -> CellBuilder.beginCell().storeCell(((KeyExtBlkRef) v).toCell()),
                e -> CellBuilder.beginCell().storeCell(((KeyMaxLt) e).toCell()),
                (fk, fv) -> CellBuilder.beginCell().storeUint(0, 1) // todo
                ))
        .endCell();
  }

  public static OldMcBlocksInfo deserialize(CellSlice cs) {
    if (isNull(cs)) {
      return OldMcBlocksInfo.builder().build();
    }
    return OldMcBlocksInfo.builder()
        .list(
            cs.loadDictAugE(
                32,
                k -> k.readInt(32),
                v -> KeyExtBlkRef.deserialize(CellSlice.beginParse(v)),
                e -> KeyMaxLt.deserialize(CellSlice.beginParse(e))))
        .build();
  }

  public List<KeyExtBlkRef> getKeyExtBlkRefAsList() {
    List<KeyExtBlkRef> keyExtBlkRefs = new ArrayList<>();
    for (Map.Entry<Object, Pair<Object, Object>> entry : list.elements.entrySet()) {
      keyExtBlkRefs.add((KeyExtBlkRef) entry.getValue().getLeft());
    }
    return keyExtBlkRefs;
  }
}
