package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.TonHashMapAugE;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 * block_extra
 *   in_msg_descr:^InMsgDescr
 *   out_msg_descr:^OutMsgDescr
 *   account_blocks:^ShardAccountBlocks
 *   rand_seed:bits256
 *   created_by:bits256
 *   custom:(Maybe ^McBlockExtra) = BlockExtra;
 */
public class BlockExtra {
    InMsgDescr inMsgDesc;
    OutMsgDescr outMsgDesc;
    TonHashMapAugE shardAccountBlocks; // _ (HashmapAugE 256 AccountBlock CurrencyCollection) = ShardAccountBlocks;
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

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeRef(inMsgDesc.toCell())
                .storeRef(outMsgDesc.toCell())
                .storeRef(shardAccountBlocks.serialize(
                        k -> CellBuilder.beginCell().storeUint((Long) k, 256).bits,
                        v -> CellBuilder.beginCell().storeCell(((AccountBlock) v).toCell()),
                        e -> CellBuilder.beginCell().storeCell(((CurrencyCollection) e).toCell())
                ))
                .storeUint(randSeed, 256)
                .storeUint(createdBy, 256)
                .storeRefMaybe(custom.toCell())
                .endCell();
    }
}




