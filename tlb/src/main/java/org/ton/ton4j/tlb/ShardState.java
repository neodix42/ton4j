package org.ton.ton4j.tlb;

import static java.util.Objects.nonNull;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 * shard_state#9023afe2 global_id:int32
 * shard_id:ShardIdent
 * seq_no:uint32 vert_seq_no:#
 * gen_utime:uint32 gen_lt:uint64
 * min_ref_mc_seqno:uint32
 * out_msg_queue_info:^OutMsgQueueInfo
 * before_split:(## 1)
 * accounts:^ShardAccounts
 * ^[ overload_history:uint64 underload_history:uint64
 * total_balance:CurrencyCollection
 * total_validator_fees:CurrencyCollection
 * libraries:(HashmapE 256 LibDescr)
 * master_ref:(Maybe BlkMasterInfo) ]
 * custom:(Maybe ^McStateExtra)
 * = ShardStateUnsplit;
 *
 * _ ShardStateUnsplit = ShardState;
 * split_state#5f327da5 left:^ShardStateUnsplit right:^ShardStateUnsplit = ShardState;
 * </pre>
 */
@Builder
@Data
public class ShardState implements Serializable {

  long magic;
  ShardStateUnsplit left;
  ShardStateUnsplit right;

  private String getMagic() {
    return Long.toHexString(magic);
  }

  public Cell toCell() {
    if (magic == 0x5f327da5L) {
      return CellBuilder.beginCell()
          .storeUint(0x5f327da5L, 32)
          .storeRef(left.toCell())
          .storeRef(right.toCell())
          .endCell();
    }
    if (nonNull(left.shardIdent)) {
      return CellBuilder.beginCell().storeCell(left.toCell()).endCell();
    } else {
      return CellBuilder.beginCell().endCell();
    }
  }

  public static ShardState deserialize(CellSlice cs) {
    long tag = cs.preloadUint(32).longValue();
    if (tag == 0x5f327da5L) {
      ShardStateUnsplit left, right;
      left = ShardStateUnsplit.deserialize(CellSlice.beginParse(cs.loadRef()));
      right = ShardStateUnsplit.deserialize(CellSlice.beginParse(cs.loadRef()));
      return ShardState.builder().magic(tag).left(left).right(right).build();
    } else {
      return ShardState.builder().magic(tag).left(ShardStateUnsplit.deserialize(cs)).build();
    }
  }
}
