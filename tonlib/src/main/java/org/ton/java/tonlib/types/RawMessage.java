package org.ton.java.tonlib.types;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Builder
@Setter
@Getter
@ToString
public class RawMessage implements Serializable {
    @SerializedName("@type")
    final String type = "raw.message";
    AccountAddressOnly source;
    AccountAddressOnly destination;
    String value;
    String message;
    String fwd_fee;
    String ihr_fee;
    long created_lt;
    String body_hash;
    MsgData msg_data;

}