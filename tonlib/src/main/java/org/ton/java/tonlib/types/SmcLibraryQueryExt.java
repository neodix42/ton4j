package org.ton.java.tonlib.types;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Setter
@Getter
@ToString
public class SmcLibraryQueryExt implements Serializable {
  @SerializedName("@type")
  //  final String type = "smc.libraryQueryExt.one";
  final String type = "smc.LibraryQueryExt";

  long hash;
  String boc;
  long max_libs;
}
