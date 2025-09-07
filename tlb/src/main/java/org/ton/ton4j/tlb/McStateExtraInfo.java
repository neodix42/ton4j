package org.ton.ton4j.tlb;

import static java.util.Objects.isNull;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 * ...
 *   ^[ flags:(## 16) { flags &lt;= 1 }
 *      validator_info:ValidatorInfo
 *      prev_blocks:OldMcBlocksInfo
 *      after_key_block:Bool
 *      last_key_block:(Maybe ExtBlkRef)
 *      block_create_stats:(flags . 0)?BlockCreateStats ]
 *   ....
 * = McStateExtra;
 * </pre>
 */
@Builder
@Data
public class McStateExtraInfo implements Serializable {
  BigInteger flags;
  ValidatorInfo validatorInfo;
  OldMcBlocksInfo prevBlocks;
  Boolean afterKeyBlock;
  ExtBlkRef lastKeyBlock;
  BlockCreateStats blockCreateStats;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(flags, 16)
        .storeCell(validatorInfo.toCell())
        .storeCell(prevBlocks.toCell())
        .storeBit(afterKeyBlock)
        .storeCellMaybe(isNull(lastKeyBlock) ? null : lastKeyBlock.toCell())
        .storeCell(flags.testBit(0) ? blockCreateStats.toCell() : null)
        .endCell();
  }

  public static McStateExtraInfo deserialize(CellSlice cs) {
    BigInteger flags = cs.loadUint(16);
    if (flags.longValue() > 1) {
      throw new Error("McStateExtra deserialization error expected flags <= 1, got: " + flags);
    }

    McStateExtraInfo mcStateExtraInfo =
        McStateExtraInfo.builder()
            .flags(flags)
            .validatorInfo(ValidatorInfo.deserialize(cs))
            .prevBlocks(OldMcBlocksInfo.deserialize(cs))
            .afterKeyBlock(cs.loadBit())
            .build();

    mcStateExtraInfo.setLastKeyBlock(cs.loadBit() ? ExtBlkRef.deserialize(cs) : null);
    mcStateExtraInfo.setBlockCreateStats(
        !flags.testBit(0) ? BlockCreateStats.deserialize(cs) : null);

    return mcStateExtraInfo;
  }
}
