package org.ton.java.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class GetLastQuery extends ExtraQuery {
  @SerializedName("@type")
  final String type = "blocks.getMasterchainInfo";
}
