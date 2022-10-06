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
public class Fees {
    @SerializedName("@type")
    final String type = "fees";
    long in_fwd_fee;
    long storage_fee;
    long gas_fee;
    long fwd_fee;
}

