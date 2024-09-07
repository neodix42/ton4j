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
 * _ (HashmapAugE 256 ShardAccount DepthBalanceInfo) = ShardAccounts;
 * </pre>
 */
@Builder
@Getter
@Setter
@ToString

public class ShardAccounts {
    TonHashMapAugE shardAccounts;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeCell(shardAccounts.serialize(
                        k -> CellBuilder.beginCell().storeUint((Long) k, 256).endCell().getBits(),
                        v -> CellBuilder.beginCell().storeCell(((ShardAccount) v).toCell()),
                        e -> CellBuilder.beginCell().storeCell(((DepthBalanceInfo) e).toCell()),
                        (fk, fv) -> CellBuilder.beginCell().storeUint(0, 1)))
                .endCell();
    }

    public static ShardAccounts deserialize(CellSlice cs) {
        return ShardAccounts.builder()
                .shardAccounts(CellSlice.beginParse(cs).loadDictAugE(256,
                        k -> k.readInt(256),
                        v -> ShardAccount.deserialize(v),
                        e -> DepthBalanceInfo.deserialize(e)))
                .build();
    }
}
