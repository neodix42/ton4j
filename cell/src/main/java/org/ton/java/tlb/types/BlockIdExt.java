package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;

import java.math.BigInteger;

@ToString
@Builder
@Getter
/**
 * block_id_ext$_
 *   shard_id:ShardIdent
 *   seq_no:uint32
 *   root_hash:bits256
 *   file_hash:bits256 = BlockIdExt;
 */
public class BlockIdExt {
    long workchain;
    long shard;
    long seqno;
    BigInteger rootHash;
    BigInteger fileHash;

    private String getRootHash() {
        return rootHash.toString(16);
    }

    private String getFileHash() {
        return fileHash.toString(16);
    }

    public String getShard() {
        return Long.toHexString(shard);
    }

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(workchain, 32)
                .storeUint(shard, 64)
                .storeUint(seqno, 32)
                .storeUint(rootHash, 256)
                .storeUint(fileHash, 256)
                .endCell();
    }
}
