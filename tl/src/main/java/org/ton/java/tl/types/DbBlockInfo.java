package org.ton.java.tl.types;

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
 * ton_api.tl
 * db.block.info#4ac6e727 id:tonNode.blockIdExt flags:# prev_left:flags.1?tonNode.blockIdExt
 *                                             prev_right:flags.2?tonNode.blockIdExt
 *                                             next_left:flags.3?tonNode.blockIdExt
 *                                             next_right:flags.4?tonNode.blockIdExt
 *                                             lt:flags.13?long
 *                                             ts:flags.14?int
 *                                             state:flags.17?int256
 *                                             masterchain_ref_seqno:flags.23?int = db.block.Info;
 */
public class DbBlockInfo {
    long magic;
    BlockIdExt id;
    BigInteger flags;
    BlockIdExt prevLeft;
    BlockIdExt prevRight;
    BlockIdExt nextLeft;
    BlockIdExt nextRight;
    BigInteger lt;
    BigInteger ts;
    BigInteger state;
    BigInteger masterChainRefSeqNo;
}
