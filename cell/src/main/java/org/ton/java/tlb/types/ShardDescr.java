package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

/**
 * <pre>
 * shard_descr#b
 *   seq_no:uint32
 *   reg_mc_seqno:uint32
 *   start_lt:uint64
 *   end_lt:uint64
 *   root_hash:bits256
 *   file_hash:bits256
 *   before_split:Bool
 *   before_merge:Bool
 *   want_split:Bool
 *   want_merge:Bool
 *   nx_cc_updated:Bool
 *   flags:(## 3) { flags = 0 }
 *   next_catchain_seqno:uint32
 *   next_validator_shard:uint64
 *   min_ref_mc_seqno:uint32
 *   gen_utime:uint32
 *   split_merge_at:FutureSplitMerge
 *   fees_collected:CurrencyCollection
 *   funds_created:CurrencyCollection = ShardDescr;
 *
 * shard_descr_new#a
 *   seq_no:uint32
 *   reg_mc_seqno:uint32
 *   start_lt:uint64
 *   end_lt:uint64
 *   root_hash:bits256
 *   file_hash:bits256
 *   before_split:Bool
 *   before_merge:Bool
 *   want_split:Bool
 *   want_merge:Bool
 *   nx_cc_updated:Bool
 *   flags:(## 3) { flags = 0 }
 *   next_catchain_seqno:uint32
 *   next_validator_shard:uint64
 *   min_ref_mc_seqno:uint32
 *   gen_utime:uint32
 *   split_merge_at:FutureSplitMerge
 *   ^[ fees_collected:CurrencyCollection
 *      funds_created:CurrencyCollection ] = ShardDescr;
 * </pre>
 */
@Builder
@Data
public class ShardDescr {
    long magic;
    long seqNo;
    long regMcSeqno;
    BigInteger startLt;
    BigInteger endLt;
    BigInteger rootHash;
    BigInteger fileHash;
    boolean beforeSplit;
    boolean beforeMerge;
    boolean wantSplit;
    boolean wantMerge;
    boolean nXCCUpdated;
    int flags;
    long nextCatchainSeqNo;
    BigInteger nextValidatorShard;
    long minRefMcSeqNo;
    long genUTime;
    FutureSplitMerge splitMergeAt;
    CurrencyCollection feesCollected;
    CurrencyCollection fundsCreated;
    Cell refInfoA;

    private String getMagic() {
        return Long.toHexString(magic);
    }

    private String getRootHash() {
        return rootHash.toString(16);
    }

    private String getFileHash() {
        return fileHash.toString(16);
    }

    public Cell toCell() {
        if (magic == 0xB) {
            return CellBuilder.beginCell()
                    .storeUint(0xB, 8)
                    .storeUint(seqNo, 32)
                    .storeUint(regMcSeqno, 32)
                    .storeUint(startLt, 64)
                    .storeUint(endLt, 64)
                    .storeUint(rootHash, 64)
                    .storeUint(fileHash, 64)
                    .storeBit(beforeSplit)
                    .storeBit(beforeMerge)
                    .storeBit(wantSplit)
                    .storeBit(wantMerge)
                    .storeBit(nXCCUpdated)
                    .storeUint(flags, 3)
                    .storeUint(nextCatchainSeqNo, 32)
                    .storeUint(nextValidatorShard, 64)
                    .storeUint(minRefMcSeqNo, 32)
                    .storeUint(genUTime, 32)
                    .storeCell(splitMergeAt.toCell())
                    .storeCell(feesCollected.toCell())
                    .storeCell(fundsCreated.toCell())
                    .endCell();
        } else if (magic == 0xA) {
            return CellBuilder.beginCell()
                    .storeUint(0xB, 8)
                    .storeUint(seqNo, 32)
                    .storeUint(regMcSeqno, 32)
                    .storeUint(startLt, 64)
                    .storeUint(endLt, 64)
                    .storeUint(rootHash, 64)
                    .storeUint(fileHash, 64)
                    .storeBit(beforeSplit)
                    .storeBit(beforeMerge)
                    .storeBit(wantSplit)
                    .storeBit(wantMerge)
                    .storeBit(nXCCUpdated)
                    .storeUint(flags, 3)
                    .storeUint(nextCatchainSeqNo, 32)
                    .storeUint(nextValidatorShard, 64)
                    .storeUint(minRefMcSeqNo, 32)
                    .storeUint(genUTime, 32)
                    .storeCell(splitMergeAt.toCell())
                    .storeRef(CellBuilder.beginCell()
                            .storeCell(feesCollected.toCell())
                            .storeCell(fundsCreated.toCell())
                            .endCell())
                    .endCell();
        } else {
            throw new Error("ShardDescr: magic neither equal to 0xA nor 0xB, found 0x" + Long.toHexString(magic));
        }
    }

    public static ShardDescr deserialize(CellSlice cs) {
        long magic = cs.loadUint(8).intValue();
        if (magic == 0xB) {
            return ShardDescr.builder()
                    .magic(0xb)
                    .seqNo(cs.loadUint(32).longValue())
                    .regMcSeqno(cs.loadUint(32).longValue())
                    .startLt(cs.loadUint(64))
                    .endLt(cs.loadUint(64))
                    .rootHash(cs.loadUint(64))
                    .fileHash(cs.loadUint(64))
                    .beforeSplit(cs.loadBit())
                    .beforeMerge(cs.loadBit())
                    .wantSplit(cs.loadBit())
                    .wantMerge(cs.loadBit())
                    .nXCCUpdated(cs.loadBit())
                    .flags(cs.loadUint(3).intValue())
                    .nextCatchainSeqNo(cs.loadUint(32).longValue())
                    .nextValidatorShard(cs.loadUint(64))
                    .minRefMcSeqNo(cs.loadUint(32).longValue())
                    .genUTime(cs.loadUint(32).longValue())
                    .splitMergeAt(FutureSplitMerge.deserialize(cs))
                    .feesCollected(CurrencyCollection.deserialize(cs))
                    .fundsCreated(CurrencyCollection.deserialize(cs))
                    .build();
        }
        if (magic == 0xA) {
            return ShardDescr.builder()
                    .magic(0xb)
                    .seqNo(cs.loadUint(32).longValue())
                    .regMcSeqno(cs.loadUint(32).longValue())
                    .startLt(cs.loadUint(64))
                    .endLt(cs.loadUint(64))
                    .rootHash(cs.loadUint(64))
                    .fileHash(cs.loadUint(64))
                    .beforeSplit(cs.loadBit())
                    .beforeMerge(cs.loadBit())
                    .wantSplit(cs.loadBit())
                    .wantMerge(cs.loadBit())
                    .nXCCUpdated(cs.loadBit())
                    .flags(cs.loadUint(3).intValue())
                    .nextCatchainSeqNo(cs.loadUint(32).longValue())
                    .nextValidatorShard(cs.loadUint(64))
                    .minRefMcSeqNo(cs.loadUint(32).longValue())
                    .genUTime(cs.loadUint(32).longValue())
                    .splitMergeAt(FutureSplitMerge.deserialize(cs))
                    .refInfoA(cs.loadRef()) // minor todo
                    .build();
        } else {
            throw new Error("ShardDescr: magic neither equal to 0xA nor 0xB, found 0x" + Long.toHexString(magic));
        }
    }
}
