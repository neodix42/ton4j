package org.ton.java.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import org.ton.java.tonlib.types.BlockIdExt;

@Builder
@Data
public class GetShardsQuery extends ExtraQuery {
  @SerializedName(value = "@type")
  final String type = "blocks.getShards";

  BlockIdExt id;
}
