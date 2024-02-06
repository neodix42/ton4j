package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Getter
@Setter
@ToString
/**
 *   split_state#5f327da5
 *   left:^ShardStateUnsplit
 *   right:^ShardStateUnsplit = ShardState;
 */
public class ShardState {

    long magic;
    ShardStateUnsplit left;
    ShardStateUnsplit right;

    private String getMagic() {
        return Long.toHexString(magic);
    }

    public Cell toCell() {
        if (magic == 0x5f327da5L) {
            return CellBuilder.beginCell()
                    .storeUint(0x5f327da5L, 32)
                    .storeRef(left.toCell())
                    .storeRef(right.toCell())
                    .endCell();
        }
        if (magic == 0x9023afe2L) {
            return CellBuilder.beginCell()
                    .storeUint(0x9023afe2L, 32)
                    .storeCell(left.toCell())
                    .endCell();
        } else {
            throw new Error("wrong magic number");
        }
    }

    public static ShardState deserialize(CellSlice cs) {
        System.out.println("shard state: " + cs.sliceToCell().toHex());
        long tag = cs.preloadUint(32).longValue();
        if (tag == 0x5f327da5L) {
            ShardStateUnsplit left, right;
            left = ShardStateUnsplit.deserialize(CellSlice.beginParse(cs.loadRef()));
            right = ShardStateUnsplit.deserialize(CellSlice.beginParse(cs.loadRef()));
            return ShardState.builder()
                    .magic(tag)
                    .left(left)
                    .right(right)
                    .build();
        } else if (tag == 0x9023afe2L) {
            return ShardState.builder()
                    .magic(tag)
                    .left(ShardStateUnsplit.deserialize(cs))
                    .build();
        } else {
            throw new Error("ShardState magic not equal neither to 0x5f327da5L nor 0x9023afe2L, found 0x" + Long.toHexString(tag));
        }
    }
}
