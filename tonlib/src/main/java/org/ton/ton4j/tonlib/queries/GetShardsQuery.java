package org.ton.ton4j.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tonlib.types.BlockIdExt;

@Builder
@Data
public class GetShardsQuery extends ExtraQuery {
  @SerializedName(value = "@type")
  final String type = "blocks.getShards";

  BlockIdExt id;
}
