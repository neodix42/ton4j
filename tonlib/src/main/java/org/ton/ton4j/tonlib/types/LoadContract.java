package org.ton.ton4j.tonlib.types;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class LoadContract implements Serializable {
  @SerializedName(value = "@type")
  final String type = "smc.info";

  long id;
}
