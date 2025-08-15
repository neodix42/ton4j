package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import java.math.BigInteger;

@Data
public class ExtendedAddressInformationResponse {
  @SerializedName("@type")
  private String type;

  private String balance;
  private String state;
  private String code;
  private String data;

  @SerializedName("last_transaction_id")
  private TransactionId lastTransactionId;

  @SerializedName("block_id")
  private BlockId blockId;

  @SerializedName("sync_utime")
  private Long syncUtime;

  @Data
  public static class TransactionId {
    @SerializedName("@type")
    private String type;

    private String lt;
    private String hash;
  }

  @Data
  public static class BlockId {
    @SerializedName("@type")
    private String type;

    private Long workchain;
    private BigInteger shard;
    private Long seqno;

    @SerializedName("root_hash")
    private String rootHash;

    @SerializedName("file_hash")
    private String fileHash;
    
    @Override
    public String toString() {
      return "BlockId{" +
          "type='" + type + '\'' +
          ", workchain=" + workchain +
          ", shard=0x" + (shard != null ? shard.toString(16) : "null") +
          ", seqno=" + seqno +
          ", rootHash='" + rootHash + '\'' +
          ", fileHash='" + fileHash + '\'' +
          '}';
    }
  }
}
