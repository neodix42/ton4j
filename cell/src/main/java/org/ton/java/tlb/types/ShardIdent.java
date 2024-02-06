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

    public static ShardIdent deserialize(CellSlice cs) {
        long magic = cs.loadUint(2).longValue();
        assert (magic == 0b00) : "ShardIdent: magic not equal to 0b00, found 0b" + Long.toBinaryString(magic);
        ShardIdent s = ShardIdent.builder()
                .magic(0L)
                .prefixBits(cs.loadUint(6).longValue())
                .workchain(cs.loadInt(32).intValue())
                .shardPrefix(cs.loadUint(64))
                .build();
        System.out.println(s);
        return s;
    }

    // todo ConvertShardIdentToShard from tonutils.go

}