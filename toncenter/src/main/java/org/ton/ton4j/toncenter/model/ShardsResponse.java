package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.math.BigInteger;
import java.util.List;

/** Response model for getShards API call */
@Data
public class ShardsResponse {

  @SerializedName("@type")
  private String type;

  private List<BlockIdExt> shards;

  @Data
  public static class BlockIdExt {
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
      return "BlockIdExt{" +
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
