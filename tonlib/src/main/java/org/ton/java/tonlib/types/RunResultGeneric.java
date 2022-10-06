package org.ton.java.tonlib.types;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Builder
@Setter
@Getter
@ToString
public class RunResultGeneric<T> {
    @SerializedName("@type")
    final String type = "smc.runResult";
    long gas_used;
    List<T> stack;
    long exit_code;

    @SerializedName("@extra")
    String extra;
}

