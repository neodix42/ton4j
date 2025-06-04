package org.ton.ton4j.tl.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Getter;
import org.ton.ton4j.tl.types.BlockIdExt;
import org.ton.ton4j.tl.types.LiteServerQueryData;

@Builder
@Getter
public class BlockQuery implements LiteServerQueryData {
  public static final int BLOCK_QUERY = 1668796173;
  BlockIdExt id;

  public String getQueryName() {
    return "liteServer.getBlock id:tonNode.blockIdExt = liteServer.BlockData";
  }

  public byte[] getQueryData() {
    int len = id.serialize().length + 4;
    //    int bodyLenPad4 = Utils.pad4(id.serialize().length + 1);
    //    int queryLenPad8 = Utils.pad8(bodyLenPad4 + 1);
    // byte[] d = Utils.toBytes(id.serialize()); // simple padding todo
    //    ByteBuffer buffer = ByteBuffer.allocate(d.length + 4);
    ByteBuffer buffer = ByteBuffer.allocate(len);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    //    buffer.put((byte) bodyLenPad4);
    //    buffer.put((byte) d.length); // d.length = 84
    buffer.putInt(BLOCK_QUERY);
    buffer.put(id.serialize());
    return buffer.array(); // buf size 88
  }
}

// works 92 bytes get block
// df068c79540dcf7763ffffffff0000000000000080e5fbe501c5f4a6b440311fdca1d36b0b5fd24cbbf683ac1cff090f33b9ec2e10dd004eecc6297c6454ea0a93f419cda6cbdaed96085ae8297079045bf939bace2ce56e94000000
