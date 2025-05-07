package org.ton.java.tonlib.types;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Builder
@Data
public class TvmSlice extends TvmEntry implements Serializable {
  @SerializedName("@type")
  final String type = "tvm.slice";

  String bytes; // base64?
}
