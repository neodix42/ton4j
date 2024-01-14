package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;

@Builder
@Getter
@Setter
@ToString
/**
 * _ ShardStateUnsplit = ShardState;
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
}
