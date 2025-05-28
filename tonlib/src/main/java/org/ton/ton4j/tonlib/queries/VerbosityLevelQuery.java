package org.ton.ton4j.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class VerbosityLevelQuery extends ExtraQuery {
  @SerializedName("@type")
  final String type = "setLogVerbosityLevel";

  int new_verbosity_level;
}
