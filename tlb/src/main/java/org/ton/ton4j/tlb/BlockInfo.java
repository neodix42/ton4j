package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 * <pre>{@code
 * block_info#9bc7a987
 *   version:uint32
 *   not_master:(## 1)
 *   after_merge:(## 1)
 *   before_split:(## 1)
 *   after_split:(## 1)
 *   want_split:Bool
 *   want_merge:Bool
 *   key_block:Bool
 *   vert_seqno_incr:(## 1)
 *   flags:(## 8) { flags <= 1 }
 *   seq_no:#
 *   vert_seq_no:# { vert_seq_no >= vert_seqno_incr }
 *   { prev_seq_no:# } { ~prev_seq_no + 1 = seq_no }
 *   shard:ShardIdent
 *   gen_utime:uint32
 *   start_lt:uint64
 *   end_lt:uint64
 *   gen_validator_list_hash_short:uint32
 *   gen_catchain_seqno:uint32
 *   min_ref_mc_seqno:uint32
 *   prev_key_block_seqno:uint32
 *   gen_software:flags . 0?GlobalVersion
 *   master_ref:not_master?^BlkMasterInfo
 *   prev_ref:^(BlkPrevInfo after_merge)
 *   prev_vert_ref:vert_seqno_incr?^(BlkPrevInfo 0)
 *   = BlockInfo;
 *   }
 *   <pre>
 */
@Builder
@Data
public class BlockInfo implements Serializable {
  long magic;
  long version;
  boolean notMaster;
  boolean afterMerge;
  boolean beforeSplit;
  boolean afterSplit;
  boolean wantSplit;
  boolean wantMerge;
  boolean keyBlock;
  boolean vertSeqnoIncr;
  long flags;
  long seqno;
  long vertSeqno;
  ShardIdent shard;
  long genuTime;
  BigInteger startLt;
  BigInteger endLt;
  long genValidatorListHashShort;
  long genCatchainSeqno;
  long minRefMcSeqno;
  long prevKeyBlockSeqno;
  GlobalVersion globalVersion;
  ExtBlkRef masterRef;
  BlkPrevInfo prevRef;
  BlkPrevInfo prevVertRef;

  private String getMagic() {
    return Long.toHexString(magic);
  }

  public Cell toCell() {
    CellBuilder result =
        CellBuilder.beginCell()
            .storeUint(2613553543L, 32) // 0x9bc7a987L
            .storeUint(version, 32)
            .storeBit(notMaster)
            .storeBit(afterMerge)
            .storeBit(beforeSplit)
            .storeBit(afterSplit)
            .storeBit(wantSplit)
            .storeBit(wantMerge)
            .storeBit(keyBlock)
            .storeBit(vertSeqnoIncr)
            .storeUint(flags, 8)
            .storeUint(seqno, 32)
            .storeUint(vertSeqno, 32)
            .storeCell(shard.toCell())
            .storeUint(genuTime, 32)
            .storeUint(startLt, 64)
            .storeUint(endLt, 64)
            .storeUint(genValidatorListHashShort, 32)
            .storeUint(getGenCatchainSeqno(), 32)
            .storeUint(minRefMcSeqno, 32)
            .storeUint(prevKeyBlockSeqno, 32);

    if ((flags & 0x1L) == 0x1L) {
      result.storeCell(globalVersion.toCell());
    }
    if (notMaster) {
      result.storeRef(masterRef.toCell());
    }
    result.storeRef(prevRef.toCell(afterMerge));
    if (vertSeqnoIncr) {
      result.storeRef(prevVertRef.toCell(afterMerge));
    }

    return result.endCell();
  }

  public static BlockInfo deserialize(CellSlice cs) {
    long magic = cs.loadUint(32).longValue();
    assert (magic == 0x9bc7a987L)
        : "BlockInfo: magic not equal to 0x9bc7a987, found 0x" + Long.toHexString(magic);

    BlockInfo blockInfo =
        BlockInfo.builder()
            .magic(0x9bc7a987L)
            .version(cs.loadUint(32).longValue())
            .notMaster(cs.loadBit())
            .afterMerge(cs.loadBit())
            .beforeSplit(cs.loadBit())
            .afterSplit(cs.loadBit())
            .wantSplit(cs.loadBit())
            .wantMerge(cs.loadBit())
            .keyBlock(cs.loadBit())
            .vertSeqnoIncr(cs.loadBit())
            .flags(cs.loadUint(8).longValue())
            .seqno(cs.loadUint(32).longValue())
            .vertSeqno(cs.loadUint(32).longValue())
            .shard(ShardIdent.deserialize(cs))
            .genuTime(cs.loadUint(32).longValue())
            .startLt(cs.loadUint(64))
            .endLt(cs.loadUint(64))
            .genValidatorListHashShort(cs.loadUint(32).longValue())
            .genCatchainSeqno(cs.loadUint(32).longValue())
            .minRefMcSeqno(cs.loadUint(32).longValue())
            .prevKeyBlockSeqno(cs.loadUint(32).longValue())
            .build();
    blockInfo.setGlobalVersion(
        ((blockInfo.getFlags() & 0x1L) == 0x1L) ? GlobalVersion.deserialize(cs) : null);
    blockInfo.setMasterRef(
        blockInfo.isNotMaster() ? ExtBlkRef.deserialize(CellSlice.beginParse(cs.loadRef())) : null);
    blockInfo.setPrevRef(
        loadBlkPrevInfo(CellSlice.beginParse(cs.loadRef()), blockInfo.isAfterMerge()));
    blockInfo.setPrevVertRef(
        blockInfo.isVertSeqnoIncr()
            ? loadBlkPrevInfo(CellSlice.beginParse(cs.loadRef()), blockInfo.isAfterMerge())
            : null);
    return blockInfo;
  }

  private static BlkPrevInfo loadBlkPrevInfo(CellSlice cs, boolean afterMerge) {
    BlkPrevInfo blkPrevInfo = BlkPrevInfo.builder().build();
    if (!afterMerge) {
      ExtBlkRef blkRef = ExtBlkRef.deserialize(cs);
      blkPrevInfo.setPrev1(blkRef);
      return blkPrevInfo;
    }

    ExtBlkRef blkRef1 = ExtBlkRef.deserialize(CellSlice.beginParse(cs.loadRef()));
    ExtBlkRef blkRef2 = ExtBlkRef.deserialize(CellSlice.beginParse(cs.loadRef()));
    blkPrevInfo.setPrev1(blkRef1);
    blkPrevInfo.setPrev2(blkRef2);
    return blkPrevInfo;
  }
}
