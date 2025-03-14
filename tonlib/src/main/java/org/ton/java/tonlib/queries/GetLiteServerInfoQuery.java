package org.ton.java.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Setter
@Getter
@ToString
public class GetLiteServerInfoQuery extends ExtraQuery {
  @SerializedName("@type")
  final String type = "liteServer.getInfo";
}
