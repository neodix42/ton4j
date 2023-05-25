package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.CellSlice;

@Builder
@Getter
@Setter
@ToString
public class Shard {
    public Shard() {
        System.out.println("asdf");
    }
    
    public static ShardState loadShardStateFromCell(CellSlice slice) {
        long tag = slice.loadUint(32).longValue();
        System.out.println("Tag " + tag);

        if (tag == 0x5f327da5) { // splitstate

        } else if (tag == 0x9023afe2) {

        } else {

        }
        return ShardState.builder()
                .left(null)
                .right(null) // todo
                .build();
    }

    public static ConfigParams loadConfigParamsFromCell(CellSlice slice) {
        return null; // todo
    }
}
