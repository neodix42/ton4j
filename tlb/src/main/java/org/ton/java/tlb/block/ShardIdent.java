package org.ton.java.tlb.block;

public class ShardIdent {
    int magic; // `tlb:"$00"`
    byte PrefixBits;
    int workchain;
    long shardPrefix; //uint64 `tlb:"## 64"`
}
