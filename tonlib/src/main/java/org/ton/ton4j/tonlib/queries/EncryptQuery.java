package org.ton.ton4j.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class EncryptQuery extends ExtraQuery {
  @SerializedName(value = "@type")
  final String type = "encrypt";

  String decrypted_data;
  String secret;
}
