package org.ton.java.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import org.ton.java.tonlib.types.BlockIdExt;

@Builder
@Data
public class GetAccountStateOnlyWithBlockQuery extends ExtraQuery {
  @SerializedName(value = "@type")
  final String type = "withBlock";

  BlockIdExt id;
  GetAccountStateQueryOnly function;
}
