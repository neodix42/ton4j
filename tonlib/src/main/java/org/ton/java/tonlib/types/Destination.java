package org.ton.java.tonlib.types;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class Destination implements Serializable {

  @SerializedName("@type")
  final String type = "data"; // not necessary

  String account_address;
}
