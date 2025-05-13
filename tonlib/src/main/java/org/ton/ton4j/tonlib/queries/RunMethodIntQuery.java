package org.ton.ton4j.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tonlib.types.TvmStackEntry;

import java.util.Deque;

@Builder
@Data
public class RunMethodIntQuery extends ExtraQuery {
  @SerializedName(value = "@type")
  final String type = "smc.runGetMethod";

  long id;
  MethodNumber method;
  Deque<TvmStackEntry> stack;
}
