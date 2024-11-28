package org.ton.java.tonlib.types;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Setter
@Getter
@ToString
public class SmcLibraryExtResult implements Serializable {
  @SerializedName("@type")
  final String type = "smc.LibraryResultExt";

  String dict_boc;
  List<String> libs_ok;
  List<String> libs_not_found;
}
