package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import java.math.BigInteger;
import java.util.List;

/** Response for getShardBlockProof endpoint */
@Data
public class ShardBlockProofResponse {

  @SerializedName("from")
  private BlockIdExt from;

  @SerializedName("mc_id")
  private BlockIdExt masterchainId;

  @SerializedName("links")
  private List<ProofLink> links;

  @SerializedName("mc_proof")
  private List<BlockLinkBack> mcProof;

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

  @Data
  public static class ProofLink {
    private BlockIdExt id;
    private String proof;
  }

  @Data
  public static class BlockLinkBack {
    @SerializedName("@type")
    private String type;

    @SerializedName("to_key_block")
    private Boolean toKeyBlock;

    @SerializedName("from")
    private BlockIdExt from;

    @SerializedName("to")
    private BlockIdExt to;

    @SerializedName("dest_proof")
    private String destProof;

    @SerializedName("proof")
    private String proof;

    @SerializedName("state_proof")
    private String stateProof;
  }
}
