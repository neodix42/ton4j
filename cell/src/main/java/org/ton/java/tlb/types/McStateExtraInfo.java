package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 * ...
 *   ^[ flags:(## 16) { flags <= 1 }
 *      validator_info:ValidatorInfo
 *      prev_blocks:OldMcBlocksInfo
 *      after_key_block:Bool
 *      last_key_block:(Maybe ExtBlkRef)
 *      block_create_stats:(flags . 0)?BlockCreateStats ]
 *   ....
 * = McStateExtra;
 */
public class McStateExtraInfo {
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
                .storeCellMaybe(lastKeyBlock.toCell())
                .storeCell(flags.testBit(0) ? blockCreateStats.toCell() : null)
                .endCell();
    }

    public static McStateExtraInfo deserialize(CellSlice cs) {
        BigInteger flags = cs.preloadUint(16);
        if (flags.longValue() > 1) {
            throw new Error("McStateExtra deserialization error expected flags <= 1, got: " + flags);
        }
        McStateExtraInfo mcStateExtraInfo = McStateExtraInfo.builder()
                .flags(cs.loadUint(16))
                .validatorInfo(ValidatorInfo.deserialize(cs))
                .prevBlocks(OldMcBlocksInfo.deserialize(cs))
                .afterKeyBlock(cs.loadBit())
                .build();

        cs.loadBits(65); // todo why?

        mcStateExtraInfo.setLastKeyBlock(cs.loadBit() ? ExtBlkRef.deserialize(cs) : null);
        mcStateExtraInfo.setBlockCreateStats(flags.testBit(0) ? BlockCreateStats.deserialize(cs) : null);

        return mcStateExtraInfo;
    }
}
