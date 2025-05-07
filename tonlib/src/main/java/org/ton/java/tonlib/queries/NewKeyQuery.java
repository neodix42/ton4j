package org.ton.java.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class NewKeyQuery extends ExtraQuery {
  @SerializedName("@type")
  final String type = "createNewKey";

  String mnemonic_password;
  String random_extra_seed;
  String local_password;
}
