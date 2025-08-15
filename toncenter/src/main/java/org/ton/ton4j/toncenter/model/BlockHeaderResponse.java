package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import java.math.BigInteger;
import java.util.List;

@Data
public class BlockHeaderResponse {
  @SerializedName("@type")
  private String type;

  private BlockIdExt id;

  @SerializedName("global_id")
  private Long globalId;

  private Long version;
  private Long flags;

  @SerializedName("after_merge")
  private Boolean afterMerge;

  @SerializedName("after_split")
  private Boolean afterSplit;

  @SerializedName("before_split")
  private Boolean beforeSplit;

  @SerializedName("want_merge")
  private Boolean wantMerge;

  @SerializedName("want_split")
  private Boolean wantSplit;

  @SerializedName("validator_list_hash_short")
  private Long validatorListHashShort;

  @SerializedName("catchain_seqno")
  private Long catchainSeqno;

  @SerializedName("min_ref_mc_seqno")
  private Long minRefMcSeqno;

  @SerializedName("is_key_block")
  private Boolean isKeyBlock;

  @SerializedName("prev_key_block_seqno")
  private Long prevKeyBlockSeqno;

  @SerializedName("start_lt")
  private Long startLt;

  @SerializedName("end_lt")
  private Long endLt;

  @SerializedName("gen_utime")
  private Long genUtime;

  @SerializedName("prev_blocks")
  private List<BlockIdExt> prevBlocks;

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
