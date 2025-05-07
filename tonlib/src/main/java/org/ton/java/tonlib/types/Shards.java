package org.ton.java.tonlib.types;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Builder
@Data
public class Shards implements Serializable {
  @SerializedName("@type")
  final String type = "blocks.shards";

  List<BlockIdExt> shards;
}
