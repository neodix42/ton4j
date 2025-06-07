package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 *     blocks.header
 *     id:ton.blockIdExt
 *     global_id:int32
 *     version:int32
 *     flags:#
 *     after_merge:Bool
 *     after_split:Bool
 *     before_split:Bool
 *     want_merge:Bool
 *     want_split:Bool
 *     validator_list_hash_short:int32
 *     catchain_seqno:int32
 *     min_ref_mc_seqno:int32
 *     is_key_block:Bool
 *     prev_key_block_seqno:int32
 *     start_lt:int64
 *     end_lt:int64
 *     gen_utime:int53
 *     vert_seqno:#
 *     prev_blocks:vector<ton.blockIdExt> = blocks.Header;
 * </pre>
 */
@Builder
@Data
public class BlockHeader implements Serializable {

  BlockIdExt id;
  long global_id;
  long version;
  long flags;
  boolean after_merge;
  boolean after_split;
  boolean before_split;
  boolean want_merge;
  boolean want_split;
  long validator_list_hash_short;
  long catchain_seqno;
  long min_ref_mc_seqno;
  boolean is_key_block;
  long prev_key_block_seqno;
  String start_lt;
  String end_lt;
  long gen_utime;
  List<BlockIdExt> prev_blocks;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeCell(id.toCell())
        .storeInt(global_id, 32)
        .storeInt(version, 32)
        .storeUint(flags, 4)
        // todo
        .endCell();
  }

  public static BlockHeader deserialize(CellSlice cs) {
    return BlockHeader.builder()
        .id(BlockIdExt.deserialize(cs))
        .global_id(cs.loadInt(32).intValue())
        .version(cs.loadInt(32).longValue())
        .flags(cs.loadUint(4).longValue())
        .build();
  }
}
