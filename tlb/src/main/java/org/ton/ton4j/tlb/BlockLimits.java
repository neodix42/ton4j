package org.ton.ton4j.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

@Builder
@Data
public class BlockLimits implements Serializable {
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

  public static BlockLimits deserialize(CellSlice cs) {
    return BlockLimits.builder()
        .magic(cs.loadUint(8).intValue())
        .bytes(ParamLimits.deserialize(cs))
        .gas(ParamLimits.deserialize(cs))
        .ltDelta(ParamLimits.deserialize(cs))
        .build();
  }
}
