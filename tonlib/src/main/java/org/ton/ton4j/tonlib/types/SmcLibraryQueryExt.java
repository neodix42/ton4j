package org.ton.ton4j.tonlib.types;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class SmcLibraryQueryExt implements Serializable {
  @SerializedName("@type")
  //  final String type = "smc.libraryQueryExt.one";
  final String type = "smc.LibraryQueryExt";

  long hash;
  String boc;
  long max_libs;
}
