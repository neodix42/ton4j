package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Data
public class WorkchainFormatExt implements WorkchainFormat {
  int wfmtExt;
  int minAddrLen;
  int maxAddrLen;
  int addrLenStep;
  long workchainTypeId;

  public Cell toCell(boolean basic) {
    if (!basic) {
      return CellBuilder.beginCell()
          .storeUint(0, 4)
          .storeUint(minAddrLen, 12)
          .storeUint(maxAddrLen, 12)
          .storeUint(addrLenStep, 12)
          .storeUint(workchainTypeId, 32)
          .endCell();
    }
    return null;
  }

  public static WorkchainFormatExt deserialize(CellSlice cs) {
    return WorkchainFormatExt.builder()
        .wfmtExt(cs.loadUint(4).intValue())
        .minAddrLen(cs.loadUint(12).intValue())
        .maxAddrLen(cs.loadUint(12).intValue())
        .addrLenStep(cs.loadUint(12).intValue())
        .workchainTypeId(cs.loadUint(32).longValue())
        .build();
  }
}
