package org.ton.ton4j.tl.types;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TransactionId3 implements Serializable {
  private long account;
  private long lt;

  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(16);
    buffer.putLong(account);
    buffer.putLong(lt);
    return buffer.array();
  }

  public static TransactionId3 deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    return TransactionId3.builder().account(buffer.getLong()).lt(buffer.getLong()).build();
  }

  public static TransactionId3 deserialize(byte[] data) {
    return deserialize(ByteBuffer.wrap(data));
  }

  public static int getSize() {
    return 16;
  }
}
