package org.ton.ton4j.tl.liteserver.responses;

import java.io.Serializable;
import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

@Builder
@Data
public class TransactionId3 implements Serializable {
  public byte[] account;
  private long lt;

  public String getAccount() {
    return Utils.bytesToHex(account);
  }

  public byte[] serialize() {
    return ByteBuffer.allocate(32 + 8).put(account).putLong(lt).array();
  }

  public static TransactionId3 deserialize(ByteBuffer buffer) {
    return TransactionId3.builder().account(Utils.read(buffer, 32)).lt(buffer.getLong()).build();
  }

  public static TransactionId3 deserialize(byte[] data) {
    return deserialize(ByteBuffer.wrap(data));
  }

  public static int getSize() {
    return 32 + 8;
  }
}
