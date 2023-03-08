package org.ton.java.tonlib.types;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigInteger;

@Builder
@Setter
@Getter
@ToString
public class LastTransactionId implements Serializable {
    @SerializedName("@type")
    final String type = "internal.transactionId"; // not necessary
    BigInteger lt;
    String hash;
}

