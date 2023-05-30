package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.TonHashMapE;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
public class ShardStateUnsplit {
    long magic;//      `tlb:"#9023afe2"`
    int globalId;          //    `tlb:"## 32"`
    ShardIdent shardIdent;  //    `tlb:"."`
    long seqno;             //    `tlb:"## 32"`
    long vertSeqno;         //    `tlb:"## 32"`
    long genUTime;          //    `tlb:"## 32"`
    BigInteger genLT;       //    `tlb:"## 64"`
    long minRefMCSeqno;     //    `tlb:"## 32"`
    Cell outMsgQueueInfo;   //    `tlb:"^"`
    boolean beforeSplit;    //    `tlb:"## 1"`
    TonHashMapE accounts;    //    `tlb:"dict 256"`	`tlb:"^"`
    Cell stats; // `tlb:"^"`
    McStateExtra mc; //`tlb:"maybe ^"`
}
