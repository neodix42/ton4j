package org.ton.ton4j.tonlib.types;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class LiteServerVersion implements Serializable {

  @SerializedName("@type")
  final String type = "liteServer.info";

  long now;
  long version;
  long capabilities;
}
