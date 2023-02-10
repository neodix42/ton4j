package org.ton.java.tlb.block;

public class BlockInfoPart {
    long magic;
    int version;
    boolean notMaster;
    boolean afterMerge;
    boolean BeforeSplit;
    boolean AfterSplit;
    boolean WantSplit;
    boolean WantMerge;
    boolean KeyBlock;
    boolean VertSeqnoIncr;
    int flags; //`tlb:"## 8"`
    int seqno;
    int vertseqno;
    ShardIdent shard;
    int genutime;
    long startLt;
    long endLt;
    int GenValidatorListHashShort;
    int GenCatchainSeqno;
    int MinRefMcSeqno;
    int PrevKeyBlockSeqno;
}
