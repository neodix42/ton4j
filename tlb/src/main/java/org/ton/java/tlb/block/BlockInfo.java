package org.ton.java.tlb.block;

import lombok.Builder;

@Builder
public class BlockInfo {
    int workchain;
    long shard;
    int seqno;
    byte[] rootHash;
    byte[] fileHash;

    public static BlockInfo load(byte[] data) {
        return BlockInfo.builder().build(); // todo
    }

    public static byte[] serialize(BlockInfo b) {
        return new byte[0]; //todo
    }
}
