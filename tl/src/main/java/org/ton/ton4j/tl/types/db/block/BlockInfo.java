package org.ton.ton4j.tl.types.db.block;

import java.io.Serializable;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tl.liteserver.responses.BlockIdExt;
import org.ton.ton4j.utils.Utils;

/**
 *
 *
 * <pre>
 * ton_api.tl
 * db.block.info#4ac6e727 id:tonNode.blockIdExt flags:# prev_left:flags.1?tonNode.blockIdExt
 *                                            prev_right:flags.2?tonNode.blockIdExt
 *                                            next_left:flags.3?tonNode.blockIdExt
 *                                            next_right:flags.4?tonNode.blockIdExt
 *                                            lt:flags.13?long 
 *                                            ts:flags.14?int
 *                                            state:flags.17?int256 
 *                                            masterchain_ref_seqno:flags.23?int = db.block.Info;
 * </pre>
 */
@Builder
@Data
public class BlockInfo implements Serializable {

  public static final long MAGIC = 0x4ac6e727L;

  long magic;
  BlockIdExt id;
  public BigInteger flags;
  BlockIdExt prevLeft;
  BlockIdExt prevRight;
  BlockIdExt nextLeft;
  BlockIdExt nextRight;
  BigInteger lt;
  BigInteger ts;
  public byte[] state;  // int256
  BigInteger masterchainRefSeqno;

  // Convenience getter
  public String getState() {
    return Utils.bytesToHex(state);
  }

  public static BlockInfo deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    
    long magic = buffer.getInt() & 0xFFFFFFFFL; // Read as unsigned int
    if (magic != MAGIC) {
      throw new IllegalArgumentException("Invalid magic: " + Long.toHexString(magic) + ", expected: " + Long.toHexString(MAGIC));
    }
    
    BlockIdExt id = BlockIdExt.deserialize(buffer);
    int flagsInt = buffer.getInt();
    BigInteger flags = BigInteger.valueOf(flagsInt);
    
    BlockInfoBuilder builder = BlockInfo.builder()
        .magic(MAGIC)
        .id(id)
        .flags(flags);
    
    // Optional fields based on flags
    if (flags.testBit(1)) {
      builder.prevLeft(BlockIdExt.deserialize(buffer));
    }
    
    if (flags.testBit(2)) {
      builder.prevRight(BlockIdExt.deserialize(buffer));
    }
    
    if (flags.testBit(3)) {
      builder.nextLeft(BlockIdExt.deserialize(buffer));
    }
    
    if (flags.testBit(4)) {
      builder.nextRight(BlockIdExt.deserialize(buffer));
    }
    
    if (flags.testBit(13)) {
      builder.lt(BigInteger.valueOf(buffer.getLong()));
    }
    
    if (flags.testBit(14)) {
      builder.ts(BigInteger.valueOf(buffer.getInt()));
    }
    
    if (flags.testBit(17)) {
      builder.state(Utils.read(buffer, 32));
    }
    
    if (flags.testBit(23)) {
      builder.masterchainRefSeqno(BigInteger.valueOf(buffer.getInt()));
    }
    
    return builder.build();
  }

  public byte[] serialize() {
    // Calculate buffer size based on which optional fields are present
    int size = 4 + BlockIdExt.getSize() + 4; // magic + id + flags
    
    if (flags.testBit(1) && prevLeft != null) {
      size += BlockIdExt.getSize();
    }
    
    if (flags.testBit(2) && prevRight != null) {
      size += BlockIdExt.getSize();
    }
    
    if (flags.testBit(3) && nextLeft != null) {
      size += BlockIdExt.getSize();
    }
    
    if (flags.testBit(4) && nextRight != null) {
      size += BlockIdExt.getSize();
    }
    
    if (flags.testBit(13) && lt != null) {
      size += 8; // long
    }
    
    if (flags.testBit(14) && ts != null) {
      size += 4; // int
    }
    
    if (flags.testBit(17) && state != null) {
      size += 32; // int256
    }
    
    if (flags.testBit(23) && masterchainRefSeqno != null) {
      size += 4; // int
    }
    
    ByteBuffer buffer = ByteBuffer.allocate(size);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    
    buffer.putInt((int) magic);
    buffer.put(id.serialize());
    buffer.putInt(flags.intValue());
    
    if (flags.testBit(1) && prevLeft != null) {
      buffer.put(prevLeft.serialize());
    }
    
    if (flags.testBit(2) && prevRight != null) {
      buffer.put(prevRight.serialize());
    }
    
    if (flags.testBit(3) && nextLeft != null) {
      buffer.put(nextLeft.serialize());
    }
    
    if (flags.testBit(4) && nextRight != null) {
      buffer.put(nextRight.serialize());
    }
    
    if (flags.testBit(13) && lt != null) {
      buffer.putLong(lt.longValue());
    }
    
    if (flags.testBit(14) && ts != null) {
      buffer.putInt(ts.intValue());
    }
    
    if (flags.testBit(17) && state != null) {
      buffer.put(state);
    }
    
    if (flags.testBit(23) && masterchainRefSeqno != null) {
      buffer.putInt(masterchainRefSeqno.intValue());
    }
    
    return buffer.array();
  }
}
