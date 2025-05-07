package org.ton.java.tonlib.types;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Builder
@Data
public class TvmStackEntryTuple extends TvmStackEntry implements Serializable {
  @SerializedName("@type")
  final String type = "tvm.stackEntryTuple";

  TvmTuple tuple;
}
