package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import java.util.List;

/** Response for getMasterchainBlockSignatures endpoint */
@Data
public class MasterchainBlockSignaturesResponse {

  @SerializedName("signatures")
  private List<BlockSignature> signatures;

  @Data
  public static class BlockSignature {
    @SerializedName("node_id_short")
    private String nodeIdShort;

    @SerializedName("signature")
    private String signature;
  }
}
