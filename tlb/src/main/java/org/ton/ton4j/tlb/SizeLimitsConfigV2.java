package org.ton.ton4j.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

@Builder
@Data
public class SizeLimitsConfigV2 implements SizeLimitsConfig, Serializable {
  int magic;
  long maxMsgBits;
  long maxMsgCells;
  long maxLibraryCells;
  int maxVmDataDepth;
  long maxExtMsgSize;
  int maxExtMsgDepth;
  long maxAccStateCells;
  long maxAccStateBits;
  long maxAccPublicLibraries;
  long deferOutQueueSizeLimit;
  long maxMsgExtraCurrencies;
  long maxAccFixedPrefixLength;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0x02, 8)
        .storeUint(maxMsgBits, 32)
        .storeUint(maxMsgCells, 32)
        .storeUint(maxLibraryCells, 32)
        .storeUint(maxVmDataDepth, 16)
        .storeUint(maxExtMsgSize, 32)
        .storeUint(maxExtMsgDepth, 16)
        .storeUint(maxAccStateCells, 32)
        .storeUint(maxAccStateBits, 32)
        .storeUint(maxAccPublicLibraries, 32)
        .storeUint(deferOutQueueSizeLimit, 32)
        .storeUint(maxMsgExtraCurrencies, 32)
        .storeUint(maxAccFixedPrefixLength, 8)
        .endCell();
  }

  public static SizeLimitsConfigV2 deserialize(CellSlice cs) {
    return SizeLimitsConfigV2.builder()
        .magic(cs.loadUint(8).intValue())
        .maxMsgBits(cs.loadUint(32).longValue())
        .maxMsgCells(cs.loadUint(32).longValue())
        .maxLibraryCells(cs.loadUint(32).longValue())
        .maxVmDataDepth(cs.loadUint(32).intValue())
        .maxExtMsgSize(cs.loadUint(32).longValue())
        .maxExtMsgDepth(cs.loadUint(32).intValue())
        .maxAccStateCells(cs.loadUint(32).longValue())
        .maxAccStateBits(cs.loadUint(32).longValue())
        .maxAccPublicLibraries(cs.loadUint(32).longValue())
        .deferOutQueueSizeLimit(cs.loadUint(32).longValue())
        .maxMsgExtraCurrencies(cs.loadUint(32).longValue())
        .maxAccFixedPrefixLength(cs.loadUint(8).longValue())
        .build();
  }
}
