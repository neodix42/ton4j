package org.ton.ton4j.tl.types.db.block;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

/**
 *
 *
 * <pre>
 * db.block.info#4ac6e727
 *  id:tonNode.blockIdExt
 *  flags:#
 *  prev_left:flags.1?tonNode.blockIdExt
 *  prev_right:flags.2?tonNode.blockIdExt
 *  next_left:flags.3?tonNode.blockIdExt
 *  next_right:flags.4?tonNode.blockIdExt
 *  lt:flags.13?long
 *  ts:flags.14?int
 *  state:flags.17?int256
 *  masterchain_ref_seqno:flags.23?int = db.block.Info;
 * </pre>
 */
@Builder
@Data
public class BlockInfo {
  static final int MAGIC = 0x4ac6e727;
  BlockIdExt id;
  int flags;
  BlockIdExt prevLeft;
  BlockIdExt prevRight;
  BlockIdExt nextLeft;
  BlockIdExt nextRight;
  long lt;
  long ts;
  public byte[] state;
  long masterRefSeqno;

  public String getState() {
    if (state == null) {
      return "";
    }
    return Utils.bytesToHex(state);
  }

  public static BlockInfo deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);

    BlockInfo blockInfo = BlockInfo.builder().build();

    int magic = buffer.getInt();
    if (magic != MAGIC) {
      throw new RuntimeException("Invalid magic number: " + magic);
    }

    blockInfo.id = BlockIdExt.deserialize(buffer);
    // Read flags
    int flagsInt = buffer.getInt();
    blockInfo.flags = flagsInt;
    BigInteger flags = BigInteger.valueOf(flagsInt);

    if (flags.testBit(1)) {
      blockInfo.prevLeft = BlockIdExt.deserialize(buffer);
    }
    if (flags.testBit(2)) {
      blockInfo.prevRight = BlockIdExt.deserialize(buffer);
    }
    if (flags.testBit(3)) {
      blockInfo.nextLeft = BlockIdExt.deserialize(buffer);
    }
    if (flags.testBit(4)) {
      blockInfo.nextRight = BlockIdExt.deserialize(buffer);
    }
    if (flags.testBit(13)) {
      blockInfo.lt = buffer.getLong();
    }
    if (flags.testBit(14)) {
      blockInfo.ts = buffer.getInt();
    }
    if (flags.testBit(17)) {
      byte[] state = new byte[32];
      buffer.get(state);
      blockInfo.state = state;
    }
    if (flags.testBit(23)) {
      blockInfo.masterRefSeqno = buffer.getInt();
    }
    return blockInfo;
  }

  public ByteBuffer serialize() {
    return null;
  }

  public static BlockInfo deserialize(byte[] data) {
    return deserialize(ByteBuffer.wrap(data));
  }
}
