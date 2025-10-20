package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 * block_id_ext$_
 * shard_id:ShardIdent
 * seq_no:uint32  = BlockId;
 * </pre>
 */
@Builder
@Data
public class BlockId implements Serializable {
  int workchain;
  public long shard;
  long seqno;

  public String getShard() {
    return Long.toUnsignedString(shard, 16);
  }

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeInt(workchain, 32)
        .storeUint(shard, 64)
        .storeUint(seqno, 32)
        .endCell();
  }

  public static BlockId deserialize(CellSlice cs) {
    return BlockId.builder()
        .workchain(cs.loadInt(32).intValue())
        .shard(cs.loadUint(64).longValue())
        .seqno(cs.loadUint(32).longValue())
        .build();
  }

  public static BlockId deserialize(byte[] cs) {
    ByteBuffer bb = ByteBuffer.wrap(cs);

    return BlockId.builder().workchain(bb.getInt()).shard(bb.getLong()).seqno(bb.getInt()).build();
  }
}
