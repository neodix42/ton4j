package org.ton.java.tlb.block;

public class ExtBlkRef {
    long endLt; // `tlb:"## 64"`
    int seqno; // `tlb:"## 32"`
    byte[] rootHash; // `tlb:"bits 256"`
    byte[] fileHash;
}
