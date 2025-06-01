package org.ton.java.adnl.globalconfig;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class TonGlobalConfig implements Serializable {
  @SerializedName(value = "@type")
  String type;

  LiteServers[] liteservers;
  Validator validator;
  Dht dht;
}
