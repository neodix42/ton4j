package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.TonHashMap;

@Builder
@Getter
@Setter
@ToString
/**
 * masterchain_block_extra#cca5
 *   key_block:(## 1)
 *   shard_hashes:ShardHashes
 *   shard_fees:ShardFees
 *   ^[ prev_blk_signatures:(HashmapE 16 CryptoSignaturePair)
 *      recover_create_msg:(Maybe ^InMsg)
 *      mint_msg:(Maybe ^InMsg) ]
 *   config:key_block?ConfigParams
 * = McBlockExtra;
 */
public class McBlockExtra {
    long magic;
    boolean keyBlock;
    TonHashMap shardHashes;
    TonHashMap shardFees;
    ConfigParams config;

    private String getMagic() {
        return Long.toHexString(magic);
    }
}
