package org.ton.ton4j.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tonlib.types.BlockIdExt;

@Builder
@Data
public class RawGetAccountStateWithBlockQuery extends ExtraQuery {
  @SerializedName(value = "@type")
  final String type = "withBlock";

  BlockIdExt id;
  GetRawAccountStateQuery function;
}
