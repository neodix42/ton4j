package org.ton.ton4j.tonlib.types;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Builder
@Data
public class TvmStackEntryCell extends TvmStackEntry implements Serializable {
  @SerializedName("@type")
  final String type = "tvm.stackEntryCell";

  TvmCell cell;
}
