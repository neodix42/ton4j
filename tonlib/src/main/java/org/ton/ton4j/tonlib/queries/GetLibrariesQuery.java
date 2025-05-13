package org.ton.ton4j.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class GetLibrariesQuery extends ExtraQuery {
  @SerializedName(value = "@type")
  final String type = "smc.getLibraries";

  List<String> library_list;
}
