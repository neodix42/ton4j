package org.ton.java.tonlib.types;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class QueryFees implements Serializable {
  @SerializedName("@type")
  final String type = "query.fees";

  Fees source_fees;
  List<Fees> destination_fees;
}
