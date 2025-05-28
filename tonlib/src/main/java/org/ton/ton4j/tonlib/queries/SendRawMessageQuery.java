package org.ton.ton4j.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class SendRawMessageQuery extends ExtraQuery {
  @SerializedName(value = "@type")
  final String type = "raw.sendMessageReturnHash";

  String body;
}
