package org.ton.ton4j.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tonlib.types.TvmStackEntry;

import java.util.Deque;

@Builder
@Data
public class RunMethodStrQuery extends ExtraQuery {
  @SerializedName(value = "@type")
  final String type = "smc.runGetMethod";

  long id;
  MethodString method; // long or string
  Deque<TvmStackEntry> stack;
}
