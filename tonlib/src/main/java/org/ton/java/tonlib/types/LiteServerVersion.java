package org.ton.java.tonlib.types;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Setter
@Getter
@ToString
public class LiteServerVersion implements Serializable {

  @SerializedName("@type")
  final String type = "liteServer.info";

  long now;
  long version;
  long capabilities;
}
