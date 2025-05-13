package org.ton.ton4j.tonlib.types;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Builder
@Data
public class SmcLibraryEntry implements Serializable {
  @SerializedName("@type")
  final String type = "smc.libraryEntry";

  String hash;
  String data;
}
