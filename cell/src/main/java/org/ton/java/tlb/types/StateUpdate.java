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
 * !merkle_update#02 {X:Type} old_hash:bits256 new_hash:bits256 old:^X new:^X  = MERKLE_UPDATE X;
 *  update_hashes#72 {X:Type} old_hash:bits256 new_hash:bits256                = HASH_UPDATE X;
 * !merkle_proof#03 {X:Type} virtual_hash:bits256 depth:uint16 virtual_root:^X = MERKLE_PROOF X;
 */
public class StateUpdate {
    //    ShardState oldOne;
    BigInteger oldHash;
    BigInteger newHash;
    ShardState oldOne;
    ShardState newOne;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeRef(oldOne.toCell())
                .storeRef(newOne.toCell())
                .endCell();
    }
}
