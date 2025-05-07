package org.ton.java.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class GetConfigAllQuery extends ExtraQuery {
  @SerializedName(value = "@type")
  final String type = "getConfigAll";

  int mode;
}
