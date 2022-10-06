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
public class LastTransactionId {
    @SerializedName("@type")
    final String type = "internal.transactionId"; // not necessary
    BigInteger lt;
    String hash;
}

