package org.ton.ton4j.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tonlib.types.Destination;

@Builder
@Data
public class CreateQuery extends ExtraQuery {
  @SerializedName(value = "@type")
  final String type = "raw.createQuery";

  String body; // base64 encoded
  String init_code;
  String init_data;
  Destination destination;
}
