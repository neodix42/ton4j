package org.ton.ton4j.tl.liteserver.responses;

import java.io.Serializable;
import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.tlb.Block;
import org.ton.ton4j.utils.Utils;

@Data
@Builder
public class BlockData implements Serializable, LiteServerAnswer {
  public static final int BLOCK_DATA_ANSWER = -1519063700;

  private BlockIdExt id;
  public byte[] data;

  public String getData() {
    return Utils.bytesToHex(data);
  }

  public Block getBlock() {
    return Block.deserialize(CellSlice.beginParse(Cell.fromBoc(data)));
  }

  // 0x6ced74a5
  public static final int constructorId = BLOCK_DATA_ANSWER;

  public static BlockData deserialize(ByteBuffer buffer) {
    return BlockData.builder()
        .id(BlockIdExt.deserialize(buffer))
        .data(Utils.fromBytes(buffer))
        .build();
  }

  public static BlockData deserialize(byte[] data) {
    return deserialize(ByteBuffer.wrap(data));
  }
}
