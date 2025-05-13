package org.ton.ton4j.tonlib.types;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Builder
@Data
public class SmcLibraryResult implements Serializable {
  @SerializedName("@type")
  final String type = "smc.libraryResult";

  List<SmcLibraryEntry> result;
}
