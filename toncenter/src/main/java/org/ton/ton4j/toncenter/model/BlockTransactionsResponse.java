package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import java.math.BigInteger;
import java.util.List;
import org.ton.ton4j.utils.Utils;

@Data
public class BlockTransactionsResponse {
  @SerializedName("@type")
  private String type;

  private BlockIdExt id;

  @SerializedName("req_count")
  private Long reqCount;

  private Boolean incomplete;

  private List<RawTransaction> transactions;

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
          ", shard=0x" + Utils.bigIntegerToUnsignedHex(shard) +
          ", seqno=" + seqno +
          ", rootHash='" + rootHash + '\'' +
          ", fileHash='" + fileHash + '\'' +
          '}';
    }
  }

  @Data
  public static class RawTransaction {
    @SerializedName("@type")
    private String type;

    private String account;
    private Integer mode;
    private String lt;
    private String hash;
  }

}
