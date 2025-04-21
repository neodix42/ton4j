package org.ton.java.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMap;

/**
 *
 *
 * <pre>
 * _#cc utime_since:uint32
 * bit_price_ps:uint64
 * cell_price_ps:uint64
 * mc_bit_price_ps:uint64
 * mc_cell_price_ps:uint64 = StoragePrices;
 *
 * _ (Hashmap 32 StoragePrices) = ConfigParam 18;
 * </pre>
 */
@Builder
@Data
public class ConfigParams18 implements Serializable {
  TonHashMap storagePrices;

  public Cell toCell() {

    Cell dict;

    dict =
        storagePrices.serialize(
            k -> CellBuilder.beginCell().storeUint((BigInteger) k, 32).endCell().getBits(),
            v -> CellBuilder.beginCell().storeCell(((StoragePrices) v).toCell()).endCell());
    return CellBuilder.beginCell().storeDict(dict).endCell();
  }

  public static ConfigParams18 deserialize(CellSlice cs) {
    return ConfigParams18.builder()
        .storagePrices(cs.loadDict(32, k -> k.readUint(32), v -> StoragePrices.deserialize(cs)))
        .build();
  }
}
