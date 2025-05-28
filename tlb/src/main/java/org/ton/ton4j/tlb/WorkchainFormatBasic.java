package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

@Builder
@Data
public class WorkchainFormatBasic implements WorkchainFormat, Serializable {
  int wfmtBasic;
  long vmVersion;
  BigInteger vmMode;

  public Cell toCell(boolean basic) {

    if (basic) {

      return CellBuilder.beginCell()
          .storeUint(1, 4)
          .storeUint(vmVersion, 32)
          .storeUint(vmMode, 64)
          .endCell();
    }
    return null;
  }

  public static WorkchainFormatBasic deserialize(CellSlice cs) {
    return WorkchainFormatBasic.builder()
        .wfmtBasic(cs.loadUint(4).intValue())
        .vmVersion(cs.loadUint(32).longValue())
        .vmMode(cs.loadUint(64))
        .build();
  }
}
