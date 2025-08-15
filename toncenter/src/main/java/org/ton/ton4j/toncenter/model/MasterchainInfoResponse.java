package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import java.math.BigInteger;

/** Response model for getMasterchainInfo endpoint */
@Data
public class MasterchainInfoResponse {

  @SerializedName("@type")
  private String type;

  @SerializedName("last")
  private BlockId last;

  @SerializedName("state_root_hash")
  private String stateRootHash;

  @SerializedName("init")
  private BlockId init;

  @Data
  public static class BlockId {
    @SerializedName("@type")
    private String type;

    @SerializedName("workchain")
    private Long workchain;

    @SerializedName("shard")
    private BigInteger shard;

    @SerializedName("seqno")
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
