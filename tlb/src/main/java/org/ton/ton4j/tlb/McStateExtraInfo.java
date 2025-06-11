package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.cell.TonHashMapAugE;

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
  //    OldMcBlocksInfo prevBlocks;
  TonHashMapAugE prevBlocks;
  Boolean afterKeyBlock;
  ExtBlkRef lastKeyBlock;
  BlockCreateStats blockCreateStats;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(flags, 16)
        .storeCell(validatorInfo.toCell())
        .storeDict(
            prevBlocks.serialize(
                k -> CellBuilder.beginCell().storeUint((BigInteger) k, 32).endCell().getBits(),
                v -> CellBuilder.beginCell().storeCell(((KeyExtBlkRef) v).toCell()),
                e -> CellBuilder.beginCell().storeCell(((KeyMaxLt) e).toCell()),
                (fk, fv) -> CellBuilder.beginCell().storeUint(0, 1)))
        .storeBit(afterKeyBlock)
        .storeCellMaybe(lastKeyBlock.toCell())
        .storeCell(flags.testBit(0) ? blockCreateStats.toCell() : null)
        .endCell();
  }

  public static McStateExtraInfo deserialize(CellSlice cs) {
    BigInteger flags = cs.preloadUint(16);
    if (flags.longValue() > 1) {
      throw new Error("McStateExtra deserialization error expected flags <= 1, got: " + flags);
    }
    McStateExtraInfo mcStateExtraInfo =
        McStateExtraInfo.builder()
            .flags(cs.loadUint(16))
            .validatorInfo(ValidatorInfo.deserialize(cs))
            //                .prevBlocks(OldMcBlocksInfo.deserialize(cs))
            .prevBlocks(cs.loadDictAugE(32, k -> k.readUint(32), v -> v, e -> e))
            .afterKeyBlock(cs.loadBit())
            .build();

    cs.loadBits(65); // todo why?

    mcStateExtraInfo.setLastKeyBlock(cs.loadBit() ? ExtBlkRef.deserialize(cs) : null);
    mcStateExtraInfo.setBlockCreateStats(
        flags.testBit(0) ? BlockCreateStats.deserialize(cs) : null);

    return mcStateExtraInfo;
  }

  public List<KeyExtBlkRef> getPrevBlocksAsList() {
    List<KeyExtBlkRef> prevBlock = new ArrayList<>();
    for (Map.Entry<Object, Pair<Object, Object>> entry : prevBlocks.elements.entrySet()) {
      prevBlock.add((KeyExtBlkRef) entry.getValue().getLeft());
    }
    return prevBlock;
  }
}
