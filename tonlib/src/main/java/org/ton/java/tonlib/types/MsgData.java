package org.ton.java.tonlib.types;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class MsgData implements Serializable {
  @SerializedName("@type")
  final String type;

  String body;
  String text;
  String init_state;
}
