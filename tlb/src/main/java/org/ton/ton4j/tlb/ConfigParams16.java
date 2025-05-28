package org.ton.ton4j.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

import java.io.Serializable;

/**
 *
 *
 * <pre>
 *   max_validators:(## 16)
 *   max_main_validators:(## 16)
 *   min_validators:(## 16)
 *   { max_validators &gt;= max_main_validators }
 *   { max_main_validators &gt;= min_validators }
 *   { min_validators &gt;= 1 }
 *   = ConfigParam 16;
 *   </pre>
 */
@Builder
@Data
public class ConfigParams16 implements Serializable {
  long maxValidators;
  long maxMainValidators;
  long minValidators;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(maxValidators, 16)
        .storeUint(maxMainValidators, 16)
        .storeUint(minValidators, 16)
        .endCell();
  }

  public static ConfigParams16 deserialize(CellSlice cs) {
    return ConfigParams16.builder()
        .maxValidators(cs.loadUint(16).longValue())
        .maxMainValidators(cs.loadUint(16).longValue())
        .minValidators(cs.loadUint(16).longValue())
        .build();
  }
}
