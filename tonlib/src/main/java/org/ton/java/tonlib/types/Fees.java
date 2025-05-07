package org.ton.java.tonlib.types;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class Fees implements Serializable {
  @SerializedName("@type")
  final String type = "fees";

  long in_fwd_fee;
  long storage_fee;
  long gas_fee;
  long fwd_fee;
}
