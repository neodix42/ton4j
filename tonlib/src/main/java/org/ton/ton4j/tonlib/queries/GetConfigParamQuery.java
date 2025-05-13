package org.ton.ton4j.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tonlib.types.BlockIdExt;

@Builder
@Data
public class GetConfigParamQuery extends ExtraQuery {
  @SerializedName("@type")
  final String type = "getConfigParam";

  BlockIdExt id;
  long param;
  long mode;
}
