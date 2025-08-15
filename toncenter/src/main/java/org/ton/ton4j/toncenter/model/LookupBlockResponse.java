package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import java.math.BigInteger;

/** Response for lookupBlock endpoint */
@Data
public class LookupBlockResponse {

  private Long workchain;
  private BigInteger shard;
  private Long seqno;

  @SerializedName("root_hash")
  private String rootHash;

  @SerializedName("file_hash")
  private String fileHash;
  
  @Override
  public String toString() {
    return "LookupBlockResponse{" +
        "workchain=" + workchain +
        ", shard=0x" + (shard != null ? shard.toString(16) : "null") +
        ", seqno=" + seqno +
        ", rootHash='" + rootHash + '\'' +
        ", fileHash='" + fileHash + '\'' +
        '}';
  }
}
