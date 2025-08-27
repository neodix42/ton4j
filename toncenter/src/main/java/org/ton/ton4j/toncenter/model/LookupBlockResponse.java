package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import java.math.BigInteger;
import org.ton.ton4j.utils.Utils;

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
        ", shard=0x" + Utils.bigIntegerToUnsignedHex(shard) +
        ", seqno=" + seqno +
        ", rootHash='" + rootHash + '\'' +
        ", fileHash='" + fileHash + '\'' +
        '}';
  }
}
