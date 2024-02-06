package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.*;

@Builder
@Getter
@Setter
@ToString
/**
 * _ (HashmapE 32 ^(BinTree ShardDescr)) = ShardHashes;
 */
public class ShardHashes {

    TonHashMapE shardHashes;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeDict(shardHashes.serialize(
                        k -> CellBuilder.beginCell().storeUint((Long) k, 32).bits,
                        v -> CellBuilder.beginCell().storeCell(((BinTree) v).toCell())
                )).endCell();
    }

    public static ShardHashes deserialize(CellSlice cs) {
        return ShardHashes.builder()
                .shardHashes(cs.loadDictE(32,
                        k -> k.readInt(32),
                        v -> BinTree.deserialize(CellSlice.beginParse(cs.loadRef()))))
                .build();
    }

}
