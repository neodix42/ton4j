package org.ton.ton4j.tonlib.types;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Builder
@Data
public class TonlibError implements Serializable {
  @SerializedName("@type")
  final String type = "error";

  long code;
  String message;
}
