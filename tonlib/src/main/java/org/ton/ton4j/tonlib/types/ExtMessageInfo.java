package org.ton.ton4j.tonlib.types;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ExtMessageInfo implements Serializable {

  @SerializedName("@type")
  final String type = "raw.extMessageInfo";

  String hash;
  String hash_norm;
  TonlibError error;
  AdnlLiteClientError adnlLiteClientError;
  TonCenterError tonCenterError;
}
