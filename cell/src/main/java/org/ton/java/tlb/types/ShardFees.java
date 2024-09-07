package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapAugE;

/**
 * <pre>
 * _ (HashmapAugE 96 ShardFeeCreated ShardFeeCreated) = ShardFees;
 * </pre>
 */
@Builder
@Getter
@Setter
@ToString

public class ShardFees {

    TonHashMapAugE shardFees;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeDict(shardFees.serialize(
                        k -> CellBuilder.beginCell().storeUint((Long) k, 96).endCell().getBits(),
                        v -> CellBuilder.beginCell().storeCell(((ShardFeeCreated) v).toCell()).endCell(),
                        e -> CellBuilder.beginCell().storeCell(((ShardFeeCreated) e).toCell()),
                        (fk, fv) -> CellBuilder.beginCell().storeUint(0, 1) // todo
                )).endCell();
    }

    public static ShardFees deserialize(CellSlice cs) {
        return ShardFees.builder()
                .shardFees(cs.loadDictAugE(96,
                        k -> k.readInt(96),
                        v -> ShardFeeCreated.deserialize(v),
                        e -> ShardFeeCreated.deserialize(e)))
                .build();
    }

}
