package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigInteger;

/**
 * db.block.info#4ac6e727 id:tonNode.blockIdExt flags:# prev_left:flags.1?tonNode.blockIdExt
 * prev_right:flags.2?tonNode.blockIdExt
 * next_left:flags.3?tonNode.blockIdExt
 * next_right:flags.4?tonNode.blockIdExt
 * lt:flags.13?long
 * ts:flags.14?int
 * state:flags.17?int256
 * masterchain_ref_seqno:flags.23?int = db.block.Info;
 */
@Builder
@Getter
@Setter
@ToString
public class BlockInfo {
    int flag;
    BlockIdExt id;
    BlockIdExt prev_left;
    BlockIdExt prev_right;
    BlockIdExt next_left;
    BlockIdExt next_right;
    long lt;
    long ts;
    BigInteger state;
    int masterchain_ref_seqno;
}