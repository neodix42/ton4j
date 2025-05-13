package org.ton.ton4j.tonlib.types;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class BlockHeader implements Serializable {
  @SerializedName(value = "@type")
  final String type = "blocks.header";

  BlockIdExt id;
  long global_id;
  long version;
  long flags;
  boolean after_merge;
  boolean after_split;
  boolean before_split;
  boolean want_merge;
  boolean want_split;
  long validator_list_hash_short;
  long catchain_seqno;
  long min_ref_mc_seqno;
  boolean is_key_block;
  long prev_key_block_seqno;
  String start_lt;
  String end_lt;
  long gen_utime;
  List<BlockIdExt> prev_blocks;
}
