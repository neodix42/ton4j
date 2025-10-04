package org.ton.ton4j.tl.types.db.lt.desc;

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
 * db.lt.desc.value first_idx:int last_idx:int last_seqno:int last_lt:long last_ts:int = db.lt.desc.Value;
 * </pre>
 */
@Builder
@Data
public class DbLtDescValue implements Serializable {
  int magic;
  int firstIdx;
  int lastIdx;
  int lastSeqno;
  long lastLt;
  int lastTs;

  public static DbLtDescValue deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    int magic = buffer.getInt();
    int firstIdx = buffer.getInt();
    int lastIdx = buffer.getInt();
    int lastSeqno = buffer.getInt();
    long lastLt = buffer.getLong();
    int lastTs = buffer.getInt();

    return DbLtDescValue.builder()
        .magic(magic)
        .firstIdx(firstIdx)
        .lastIdx(lastIdx)
        .lastSeqno(lastSeqno)
        .lastLt(lastLt)
        .lastTs(lastTs)
        .build();
  }

  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + 4 + 4 + 8 + 4);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(1907315124);
    buffer.putInt(firstIdx);
    buffer.putInt(lastIdx);
    buffer.putInt(lastSeqno);
    buffer.putLong(lastLt);
    buffer.putInt(lastTs);
    return buffer.array();
  }
}
