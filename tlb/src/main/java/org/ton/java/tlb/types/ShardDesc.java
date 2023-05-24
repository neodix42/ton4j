package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

@Builder
@Getter
@Setter
public class ShardDesc {
    long magic;//  `tlb:"#a"`
    long SeqNo;          //    uint32 `tlb:"## 32"`
    long RegMcSeqno;//         uint32 `tlb:"## 32"`
    BigInteger startLT;         //   uint64 `tlb:"## 64"`
    BigInteger endLT;          //   uint64 `tlb:"## 64"`
    byte[] rootHash;       //   []byte `tlb:"bits 256"`
    byte[] fileHash;        //   []byte `tlb:"bits 256"`
    boolean beforeSplit;    //   bool   `tlb:"bool"`
    boolean beforeMerge;   //   bool   `tlb:"bool"`
    boolean wantSplit;     //   bool   `tlb:"bool"`
    boolean wantMerge;    //   bool   `tlb:"bool"`
    boolean nXCCUpdated;    //    bool   `tlb:"bool"`
    byte flags;      //   uint8  `tlb:"## 3"`
    long nextCatchainSeqNo;// uint32 `tlb:"## 32"`
    BigInteger nextValidatorShard;// int64  `tlb:"## 64"`
    long minRefMcSeqNo;    // uint32 `tlb:"## 32"`
    long genUTime;          // uint32 `tlb:"## 32"`
}
