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
public class RunResult {
    List<TvmStackEntry> stackEntry;
    long gas_used;
    long exit_code;

    @SerializedName("@extra")
    String extra;
}

