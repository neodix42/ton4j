package org.ton.ton4j.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/** block_limits#5d bytes:ParamLimits gas:ParamLimits lt_delta:ParamLimits = BlockLimits; */
@Builder
@Data
public class BlockLimitsV1 implements BlockLimits, Serializable {
  int magic;
  ParamLimits bytes;
  ParamLimits gas;
  ParamLimits ltDelta;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0x5d, 8)
        .storeCell(bytes.toCell())
        .storeCell(gas.toCell())
        .storeCell(ltDelta.toCell())
        .endCell();
  }

  public static BlockLimitsV1 deserialize(CellSlice cs) {
    return BlockLimitsV1.builder()
        .magic(cs.loadUint(8).intValue())
        .bytes(ParamLimits.deserialize(cs))
        .gas(ParamLimits.deserialize(cs))
        .ltDelta(ParamLimits.deserialize(cs))
        .build();
  }
}
