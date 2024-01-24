package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 * shard_ident$00
 *  shard_pfx_bits:(#<= 60)
 *  workchain_id:int32
 *  shard_prefix:uint64
 *  = ShardIdent;
 */
public class ShardIdent {
    long magic;
    long prefixBits;
    int workchain;
    BigInteger shardPrefix;

    private String getMagic() {
        return Long.toBinaryString(magic);
    }


    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0, 2)
                .storeUint(prefixBits, 6)
                .storeInt(workchain, 32)
                .storeUint(shardPrefix, 64)
                .endCell();
    }

    // todo ConvertShardIdentToShard from tonutils.go

}