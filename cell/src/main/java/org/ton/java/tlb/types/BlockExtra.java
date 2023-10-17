package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 * block_extra in_msg_descr:^InMsgDescr
 *   out_msg_descr:^OutMsgDescr
 *   account_blocks:^ShardAccountBlocks
 *   rand_seed:bits256
 *   created_by:bits256
 *   custom:(Maybe ^McBlockExtra) = BlockExtra;
 */
public class BlockExtra {
    InMsgDescr inMsgDesc;
    OutMsgDescr outMsgDesc;
    Cell shardAccountBlocks;
    BigInteger randSeed;
    BigInteger createdBy;
    McBlockExtra custom;

    private String getRandSeed() {
        return randSeed.toString(16);
    }

    private String getCreatedBy() {
        return createdBy.toString(16);
    }

//    @Override
//    public String toString() {
//        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
//    }
}




