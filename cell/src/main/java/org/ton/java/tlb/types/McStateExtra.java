package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.TonHashMapE;

@Builder
@Getter
@Setter
@ToString
/**
 * masterchain_state_extra#cc26
 *   shard_hashes:ShardHashes
 *   config:ConfigParams
 *   ^[ flags:(## 16) { flags <= 1 }
 *      validator_info:ValidatorInfo
 *      prev_blocks:OldMcBlocksInfo
 *      after_key_block:Bool
 *      last_key_block:(Maybe ExtBlkRef)
 *      block_create_stats:(flags . 0)?BlockCreateStats ]
 *   global_balance:CurrencyCollection
 * = McStateExtra;
 */
public class McStateExtra {
    long magic;
    TonHashMapE shardHashes;
    ConfigParams configParams;
    Cell info;
    CurrencyCollection globalBalance;

    private String getMagic() {
        return Long.toHexString(magic);
    }

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0xcc26, 32)
                .storeDict(shardHashes.serialize(
                        k -> CellBuilder.beginCell().storeUint((Long) k, 32).bits,
                        v -> CellBuilder.beginCell().storeCell((Cell) v))// ConfigParams
                )
                .storeCell(configParams.toCell())
                .storeRef(info)
                .storeCell(globalBalance.toCell())
                .endCell();
    }
}
