package org.ton.ton4j.exporter.lazy;

import static java.util.Objects.isNull;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.exporter.reader.CellDbReader;
import org.ton.ton4j.tlb.*;
import org.ton.ton4j.utils.Utils;

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
@Slf4j
@Builder
@Data
public class ShardStateUnsplitLazy implements Serializable {
  long magic;
  int globalId;
  ShardIdentLazy shardIdent;
  long seqno;
  long vertSeqno;
  long genUTime;
  BigInteger genLt;
  long minRefMCSeqno;
  OutMsgQueueInfoLazy outMsgQueueInfo;
  boolean beforeSplit;
  ShardAccountsLazy shardAccounts;
  ShardStateInfo shardStateInfo;
  McStateExtra custom;

  private String getMagic() {
    return Long.toHexString(magic);
  }

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(2418257890L, 32)
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
        .storeRefMaybe(isNull(custom) ? null : custom.toCell())
        .endCell();
  }

  public static ShardStateUnsplitLazy deserialize(
      CellDbReader cellDb, CellSliceLazy cs, boolean full) throws IOException {
    if (cs.isExotic()) {
      return ShardStateUnsplitLazy.builder().build();
    }
    long magic = cs.loadUint(32).longValue();
    assert (magic == 0x9023afe2L)
        : "ShardStateUnsplit magic not equal to 0x9023afe2L, found 0x" + Long.toHexString(magic);

    ShardStateUnsplitLazy shardStateUnsplitLazy =
        ShardStateUnsplitLazy.builder()
            .magic(magic)
            .globalId(cs.loadInt(32).intValue())
            .shardIdent(ShardIdentLazy.deserialize(cs)) // 104
            .seqno(cs.loadUint(32).longValue())
            .vertSeqno(cs.loadUint(32).longValue())
            .genUTime(cs.loadUint(32).longValue())
            .genLt(cs.loadUint(64))
            .minRefMCSeqno(cs.loadUint(32).longValue())
            .build(); // 360 bits

    log.info(
        "looking in {}:{}",
        shardStateUnsplitLazy.getShardIdent().convertShardIdentToShard().toString(16),
        shardStateUnsplitLazy.getSeqno());

    // ref1
    byte[] outMsgQueueInfoKeyHash = Utils.slice(cs.getHashes(), 0, 32);
    cs.hashes = Arrays.copyOfRange(cs.getHashes(), 32, cs.getHashes().length);

    Cell outMsgQueueInfoCell = cs.getRefByHash(outMsgQueueInfoKeyHash);
    shardStateUnsplitLazy.setOutMsgQueueInfo(
        OutMsgQueueInfoLazy.deserialize(CellSliceLazy.beginParse(cellDb, outMsgQueueInfoCell)));

    shardStateUnsplitLazy.setBeforeSplit(cs.loadBit());

    // ref2
    byte[] shardAccountsKeyHash = Utils.slice(cs.getHashes(), 0, 32);
    cs.hashes = Arrays.copyOfRange(cs.getHashes(), 32, cs.getHashes().length);

    Cell shardAccountsCell = cs.getRefByHash(shardAccountsKeyHash);

    if (full) {
      shardStateUnsplitLazy.setShardAccounts(
          ShardAccountsLazy.deserialize(CellSliceLazy.beginParse(cellDb, shardAccountsCell)));
    } else {
      shardStateUnsplitLazy.setShardAccounts(
          ShardAccountsLazy.prepare(CellSliceLazy.beginParse(cellDb, shardAccountsCell)));
    }

    // ref3
    //    shardStateUnsplit.setShardStateInfo(
    //        ShardStateInfo.deserialize(CellSlice.beginParse(cs.loadRef())));

    // ref4
    //    shardStateUnsplit.setCustom(
    //        cs.loadBit() ? McStateExtra.deserialize(CellSlice.beginParse(cs.loadRef())) : null);
    return shardStateUnsplitLazy;
  }
}
