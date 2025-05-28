package org.ton.ton4j.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class GetLiteServerInfoQuery extends ExtraQuery {
  @SerializedName("@type")
  final String type = "liteServer.getInfo";
}
