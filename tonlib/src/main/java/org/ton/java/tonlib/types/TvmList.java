package org.ton.java.tonlib.types;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Builder
@Data
public class TvmList extends TvmEntry implements Serializable {
  @SerializedName("@type")
  final String type = "tvm.list";

  List<Object> elements;
}
