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
public class BlockInfoPart {
    long magic; //`tlb:"#9bc7a987"`
    long version;// `tlb:"## 32"`, uint32
    boolean notMaster;//`tlb:"bool"`
    boolean afterMerge;//`tlb:"bool"`
    boolean beforeSplit;//`tlb:"bool"`
    boolean afterSplit;//`tlb:"bool"`
    boolean wantSplit;//`tlb:"bool"`
    boolean wantMerge;//`tlb:"bool"`
    boolean keyBlock;//`tlb:"bool"`
    boolean vertSeqnoIncr;//`tlb:"bool"`
    int flags; //`tlb:"## 8"`
    long seqno; //`tlb:"## 32"`
    long vertSeqno; //`tlb:"## 32"`
    ShardIdent shard;
    long genuTime; //`tlb:"## 32"`
    BigInteger startLt; //`tlb:"## 64"`
    BigInteger endLt;//`tlb:"## 64"`
    long genValidatorListHashShort;//`tlb:"## 32"`
    long genCatchainSeqno;//`tlb:"## 32"`
    long minRefMcSeqno;//`tlb:"## 32"`
    long prevKeyBlockSeqno;//`tlb:"## 32"`
}
