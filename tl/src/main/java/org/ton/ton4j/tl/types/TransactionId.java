package org.ton.ton4j.tl.types;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.swing.*;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

/**
 * liteServer.transactionId#b12f65af mode:# account:mode.0?int256 lt:mode.1?long hash:mode.2?int256
 * metadata:mode.8?liteServer.transactionMetadata = liteServer.TransactionId;
 */
@Builder
@Data
public class TransactionId implements Serializable {
  int mode;
  public byte[] account;
  long lt;
  public byte[] hash;
  TransactionMetadata transactionMetadata;

  public String getAccount() {
    return Utils.bytesToHex(account);
  }

  public String getHash() {
    return Utils.bytesToHex(hash);
  }

  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(4 + 32); // todo
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(0xb12f65af);
    buffer.putInt(mode);
    if (mode == 1) {
      buffer.put(account);
    }
    if ((mode & 2) != 0) {
      buffer.putLong(lt);
    }
    if ((mode & 4) != 0) {
      buffer.put(hash);
    }
    if ((mode & 256) != 0) {
      buffer.put(transactionMetadata.serialize());
    }
    return buffer.array();
  }

  public static TransactionId deserialize(ByteBuffer buffer) {
    //    int id = buffer.getInt();
    int mode = buffer.getInt();
    byte[] account = new byte[0];
    if ((mode & 1) != 0) {
      account = Utils.read(buffer, 32);
    }
    long lt = 0;
    if ((mode & 2) != 0) {
      lt = buffer.getLong();
    }
    byte[] hash = new byte[0];
    if ((mode & 4) != 0) {
      hash = Utils.read(buffer, 32);
    }
    TransactionMetadata transactionMetadata = null;
    if ((mode & 256) != 0) {
      transactionMetadata = TransactionMetadata.deserialize(buffer);
    }
    return TransactionId.builder()
        .account(account)
        .lt(lt)
        .hash(hash)
        .transactionMetadata(transactionMetadata)
        .build();
  }

  public static TransactionId deserialize(byte[] data) {
    return deserialize(ByteBuffer.wrap(data));
  }

  public static int getSize() {
    return 16;
  }
}
