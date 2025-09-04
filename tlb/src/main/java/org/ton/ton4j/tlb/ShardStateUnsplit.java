package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 * shard_state#9023afe2
 *   global_id:int32
 *   shard_id:ShardIdent
 *   seq_no:uint32
 *   vert_seq_no:#
 *   gen_utime:uint32
 *   gen_lt:uint64
 *   min_ref_mc_seqno:uint32
 *   out_msg_queue_info:^OutMsgQueueInfo
 *   before_split:(## 1)
 *   accounts:^ShardAccounts
 *   ^[ overload_history:uint64
 *     underload_history:uint64
 *     total_balance:CurrencyCollection
 *     total_validator_fees:CurrencyCollection
 *     libraries:(HashmapE 256 LibDescr)
 *     master_ref:(Maybe BlkMasterInfo) ]
 *   custom:(Maybe ^McStateExtra)
 *   = ShardStateUnsplit;
 * </pre>
 */
@Builder
@Data
public class ShardStateUnsplit implements Serializable {
  long magic;
  int globalId;
  ShardIdent shardIdent;
  long seqno;
  long vertSeqno;
  long genUTime;
  BigInteger genLt;
  long minRefMCSeqno;
  OutMsgQueueInfo outMsgQueueInfo;
  boolean beforeSplit;
  ShardAccounts shardAccounts;
  ShardStateInfo shardStateInfo;
  McStateExtra custom;

  private String getMagic() {
    return Long.toHexString(magic);
  }

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0x9023afe2, 32)
        .storeInt(globalId, 32)
        .storeCell(shardIdent.toCell())
        .storeUint(seqno, 32)
        .storeUint(vertSeqno, 32)
        .storeUint(genUTime, 32)
        .storeUint(genLt, 64)
        .storeUint(minRefMCSeqno, 32)
        .storeRef(outMsgQueueInfo.toCell())
        .storeBit(beforeSplit)
        .storeRef(shardAccounts.toCell())
        .storeRef(shardStateInfo.toCell())
        .storeRefMaybe(custom.toCell())
        .endCell();
  }

  public static ShardStateUnsplit deserialize(CellSlice cs) {
    if (cs.isExotic()) {
      return ShardStateUnsplit.builder().build();
    }
    long magic = cs.loadUint(32).longValue();
    assert (magic == 0x9023afe2L)
        : "ShardStateUnsplit magic not equal to 0x9023afe2L, found 0x" + Long.toHexString(magic);

    ShardStateUnsplit shardStateUnsplit =
        ShardStateUnsplit.builder()
            .magic(magic)
            .globalId(cs.loadInt(32).intValue())
            .shardIdent(ShardIdent.deserialize(cs))
            .seqno(cs.loadUint(32).longValue())
            .vertSeqno(cs.loadUint(32).longValue())
            .genUTime(cs.loadUint(32).longValue())
            .genLt(cs.loadUint(64))
            .minRefMCSeqno(cs.loadUint(32).longValue())
            .build();

    shardStateUnsplit.setOutMsgQueueInfo(
        OutMsgQueueInfo.deserialize(CellSlice.beginParse(cs.loadRef())));

    shardStateUnsplit.setBeforeSplit(cs.loadBit());

    shardStateUnsplit.setShardAccounts(
        ShardAccounts.deserialize(CellSlice.beginParse(cs.loadRef())));

    shardStateUnsplit.setShardStateInfo(
        ShardStateInfo.deserialize(CellSlice.beginParse(cs.loadRef())));

    shardStateUnsplit.setCustom(
        cs.loadBit() ? McStateExtra.deserialize(CellSlice.beginParse(cs.loadRef())) : null);
    return shardStateUnsplit;
  }
}
