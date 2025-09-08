package org.ton.ton4j.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 * block_limits_v2#5e bytes:ParamLimits gas:ParamLimits lt_delta:ParamLimits
 * collated_data:ParamLimits imported_msg_queue:ImportedMsgQueueLimits = BlockLimits;
 */
@Builder
@Data
public class BlockLimitsV2 implements BlockLimits, Serializable {
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

  public static BlockLimitsV2 deserialize(CellSlice cs) {
    return BlockLimitsV2.builder()
        .magic(cs.loadUint(8).intValue())
        .bytes(ParamLimits.deserialize(cs))
        .gas(ParamLimits.deserialize(cs))
        .ltDelta(ParamLimits.deserialize(cs))
        .build();
  }
}
