package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 * shard_descr#b
 *   seq_no:uint32
 *   reg_mc_seqno:uint32
 *   start_lt:uint64
 *   end_lt:uint64
 *   root_hash:bits256
 *   file_hash:bits256
 *   before_split:Bool
 *   before_merge:Bool
 *   want_split:Bool
 *   want_merge:Bool
 *   nx_cc_updated:Bool
 *   flags:(## 3) { flags = 0 }
 *   next_catchain_seqno:uint32
 *   next_validator_shard:uint64
 *   min_ref_mc_seqno:uint32
 *   gen_utime:uint32
 *   split_merge_at:FutureSplitMerge
 *   fees_collected:CurrencyCollection
 *   funds_created:CurrencyCollection = ShardDescr;
 *
 * shard_descr_new#a
 *   seq_no:uint32
 *   reg_mc_seqno:uint32
 *   start_lt:uint64
 *   end_lt:uint64
 *   root_hash:bits256
 *   file_hash:bits256
 *   before_split:Bool
 *   before_merge:Bool
 *   want_split:Bool
 *   want_merge:Bool
 *   nx_cc_updated:Bool
 *   flags:(## 3) { flags = 0 }
 *   next_catchain_seqno:uint32
 *   next_validator_shard:uint64
 *   min_ref_mc_seqno:uint32
 *   gen_utime:uint32
 *   split_merge_at:FutureSplitMerge
 *   ^[ fees_collected:CurrencyCollection
 *      funds_created:CurrencyCollection ] = ShardDescr;
 */
public class ShardDescr {
    long magic;
    long SeqNo;
    long RegMcSeqno;
    BigInteger startLt;
    BigInteger endLt;
    BigInteger rootHash;
    BigInteger fileHash;
    boolean beforeSplit;
    boolean beforeMerge;
    boolean wantSplit;
    boolean wantMerge;
    boolean nXCCUpdated;
    byte flags;
    long nextCatchainSeqNo;
    BigInteger nextValidatorShard;
    long minRefMcSeqNo;
    long genUTime;

    private String getMagic() {
        return Long.toHexString(magic);
    }

    private String getRootHash() {
        return rootHash.toString(16);
    }

    private String getFileHash() {
        return fileHash.toString(16);
    }

    public static ShardDescr deserialize(CellSlice cs) {
        return null;
    }
}
