package org.ton.java.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.ton.java.tonlib.types.SmcLibraryQueryExt;

@Builder
@Data
public class GetLibrariesExtQuery extends ExtraQuery {
  @SerializedName(value = "@type")
  final String type = "smc.getLibrariesExt";

  List<SmcLibraryQueryExt> list;
}
