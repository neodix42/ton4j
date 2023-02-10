package org.ton.java.tlb.block;

import java.util.Dictionary;

public class McBlockExtra {
    long magic; // `tlb:"#cca5"`
    int keyBlock; // `tlb:"## 1"`
    Dictionary shardHashes;
    Dictionary shardFees;
}
