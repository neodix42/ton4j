package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.CellSlice;
import org.ton.java.tlb.loader.Tlb;

@Builder
@Getter
@Setter
@ToString
public class ShardState {
    ShardStateUnsplit left;
    ShardStateUnsplit right;

    public static ShardState loadFromCell(CellSlice slice) {
        long tag = slice.loadUint(32).longValue();

        if (tag == 0x5f327da5L) {
            ShardStateUnsplit left = (ShardStateUnsplit) Tlb.load(ShardStateUnsplit.class, CellSlice.beginParse(slice.loadRef()));
            ShardStateUnsplit right = (ShardStateUnsplit) Tlb.load(ShardStateUnsplit.class, CellSlice.beginParse(slice.loadRef()));
            ShardState.builder()
                    .left(left)
                    .right(right)
                    .build();
        } else if (tag == 0x9023afe2L) {
            ShardStateUnsplit left = (ShardStateUnsplit) Tlb.load(ShardStateUnsplit.class, CellSlice.beginParse(slice.loadRef()));
            ShardState.builder()
                    .left(left)
                    .right(null)
                    .build();
        }
        return null;
    }
}
