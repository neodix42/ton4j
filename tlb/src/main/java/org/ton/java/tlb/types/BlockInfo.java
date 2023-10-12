package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 * block_info#9bc7a987
 *   version:uint32
 *   not_master:(## 1)
 *   after_merge:(## 1)
 *   before_split:(## 1)
 *   after_split:(## 1)
 *   want_split:Bool
 *   want_merge:Bool
 *   key_block:Bool
 *   vert_seqno_incr:(## 1)
 *   flags:(## 8) { flags <= 1 }
 *   seq_no:# vert_seq_no:# { vert_seq_no >= vert_seqno_incr }
 *   { prev_seq_no:# } { ~prev_seq_no + 1 = seq_no }
 *   shard:ShardIdent
 *   gen_utime:uint32
 *   start_lt:uint64
 *   end_lt:uint64
 *   gen_validator_list_hash_short:uint32
 *   gen_catchain_seqno:uint32
 *   min_ref_mc_seqno:uint32
 *   prev_key_block_seqno:uint32
 *   gen_software:flags . 0?GlobalVersion
 *   master_ref:not_master?^BlkMasterInfo
 *   prev_ref:^(BlkPrevInfo after_merge)
 *   prev_vert_ref:vert_seqno_incr?^(BlkPrevInfo 0)
 *   = BlockInfo;
 */
public class BlockInfo {
    long magic;
    long version;
    boolean notMaster;
    boolean afterMerge;
    boolean beforeSplit;
    boolean afterSplit;
    boolean wantSplit;
    boolean wantMerge;
    boolean keyBlock;
    boolean vertSeqnoIncr;
    long flags;
    long seqno;
    long vertSeqno;
    ShardIdent shard;
    long genuTime;
    BigInteger startLt;
    BigInteger endLt;
    long genValidatorListHashShort;
    long genCatchainSeqno;
    long minRefMcSeqno;
    long prevKeyBlockSeqno;
    GlobalVersion globalVersion;
    ExtBlkRef masterRef;
    BlkPrevInfo prefRef;
    BlkPrevInfo prefVertRef;

    private String getMagic() {
        return Long.toHexString(magic);
    }
}
