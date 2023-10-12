package org.ton.java.tl.types;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigInteger;

@ToString
@Builder
@Getter
/**
 * ton_api.tl
 * tonNode.blockIdExt workchain:int shard:long seqno:int root_hash:int256 file_hash:int256 = tonNode.BlockIdExt;
 */
public class BlockIdExt {
    long workchain;
    BigInteger shard;
    long seqno;
    int[] root_hash;
    int[] file_hash;
}
