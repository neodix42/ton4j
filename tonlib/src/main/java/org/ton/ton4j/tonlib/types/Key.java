package org.ton.ton4j.tonlib.types;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class Key implements Serializable {

  @SerializedName("@type")
  final String type = "key";

  String public_key;
  String secret;
}
