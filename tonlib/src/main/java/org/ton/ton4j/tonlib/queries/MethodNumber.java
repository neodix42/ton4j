package org.ton.ton4j.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class MethodNumber extends ExtraQuery {
  @SerializedName(value = "@type")
  final String type = "smc.methodIdNumber";

  long number;
}
