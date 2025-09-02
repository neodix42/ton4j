package org.ton.ton4j.tl.types.db.files.pkg;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;

/**
 *
 *
 * <pre>
 * ton_api.tl
 * db.files.package.firstBlock workchain:int shard:long seqno:int unixtime:int lt:long = db.files.package.FirstBlock;
 * </pre>
 */
@Builder
@Data
public class FirstBlock implements Serializable {

  int workchain;
  long shard;
  int seqno;
  int unixtime;
  long lt;

  public static FirstBlock deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    int workchain = buffer.getInt();
    long shard = buffer.getLong();
    int seqno = buffer.getInt();
    int unixtime = buffer.getInt();
    long lt = buffer.getLong();

    return FirstBlock.builder()
        .workchain(workchain)
        .shard(shard)
        .seqno(seqno)
        .unixtime(unixtime)
        .lt(lt)
        .build();
  }

  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(4 + 8 + 4 + 4 + 8);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(workchain);
    buffer.putLong(shard);
    buffer.putInt(seqno);
    buffer.putInt(unixtime);
    buffer.putLong(lt);
    return buffer.array();
  }
}
