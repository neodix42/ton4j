package org.ton.ton4j.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tonlib.types.BlockId;

@Builder
@Data
public class LookupBlockQuery extends ExtraQuery {
  @SerializedName(value = "@type")
  final String type = "blocks.lookupBlock";

  long mode;
  BlockId id;
  long lt;
  long utime;
}
