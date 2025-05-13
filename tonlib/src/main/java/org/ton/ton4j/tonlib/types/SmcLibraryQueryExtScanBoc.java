package org.ton.ton4j.tonlib.types;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class SmcLibraryQueryExtScanBoc implements Serializable {
  @SerializedName("@type")
  final String type = "smc.libraryQueryExt.scanBoc";

  String boc;
  long max_libs;
}
