package org.ton.java.tonlib.types;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Setter
@Getter
@ToString
public class MsgData {

    @SerializedName("@type")
    final String type;// = "msg.dataRaw"; can be also msg.dataText
    String body;
    String init_state;

}

