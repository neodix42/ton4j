package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.cell.TonHashMapE;

/**
 *
 *
 * <pre>
 * workchain#a6 enabled_since:uint32 actual_min_split:(## 8)
 *   min_split:(## 8) max_split:(## 8) { actual_min_split &lt;= min_split }
 *   basic:(## 1) active:Bool accept_msgs:Bool flags:(## 13) { flags = 0 }
 *   zerostate_root_hash:bits256 zerostate_file_hash:bits256
 *   version:uint32 format:(WorkchainFormat basic)
 *   = WorkchainDescr;
 *
 * workchain_v2#a7 enabled_since:uint32 actual_min_split:(## 8)
 *   min_split:(## 8) max_split:(## 8) { actual_min_split &lt;= min_split }
 *   basic:(## 1) active:Bool accept_msgs:Bool flags:(## 13) { flags = 0 }
 *   zerostate_root_hash:bits256 zerostate_file_hash:bits256
 *   version:uint32 format:(WorkchainFormat basic)
 *   split_merge_timings:WcSplitMergeTimings
 *   = WorkchainDescr;
 *
 * _ workchains:(HashmapE 32 WorkchainDescr) = ConfigParam 12;
 * </pre>
 */
@Builder
@Data
public class ConfigParams12 implements Serializable {
  TonHashMapE workchains;

  public Cell toCell() {

    return CellBuilder.beginCell()
        .storeDict(
            workchains.serialize(
                k -> CellBuilder.beginCell().storeUint((BigInteger) k, 32).endCell().getBits(),
                v -> CellBuilder.beginCell().storeCell(((WorkchainDescr) v).toCell()).endCell()))
        .endCell();
  }

  public static ConfigParams12 deserialize(CellSlice cs) {
    return ConfigParams12.builder()
        .workchains(
            cs.loadDictE(
                32, k -> k.readUint(32), v -> WorkchainDescr.deserialize(CellSlice.beginParse(v))))
        .build();
  }
}
