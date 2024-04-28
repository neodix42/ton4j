package org.ton.java.tonlib.types;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

@Builder
@Setter
@Getter
@ToString
public class RawTransaction implements Serializable {
    @SerializedName("@type")
    final String type = "raw.transaction";
    AccountAddressOnly address;
    long utime;
    String data;
    LastTransactionId transaction_id;
    String fee;
    String storage_fee;
    String other_fee;
    RawMessage in_msg;
    List<RawMessage> out_msgs;
}