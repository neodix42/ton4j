package org.ton.java.tonlib.types;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigInteger;

@Builder
@Setter
@Getter
@ToString
public class TvmStackEntryNumber extends TvmStackEntry {
    @SerializedName("@type")
    final String type = "tvm.stackEntryNumber";
    TvmNumber number;

    public BigInteger getNumber() {
        return new BigInteger(number.getNumber());
    }
}

