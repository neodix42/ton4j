package org.ton.ton4j.tonlib.types;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Builder
@Data
public class TvmTuple implements Serializable {
  @SerializedName("@type")
  final String type = "tvm.tuple";

  List<Object> elements;
}
