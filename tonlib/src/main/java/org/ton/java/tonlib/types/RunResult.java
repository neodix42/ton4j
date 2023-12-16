package org.ton.java.tonlib.types;

import com.jsoniter.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RunResult{
    private List<TvmStackEntry> stackEntry;
    private long gas_used;
    private long exit_code;
    @JsonProperty(to = "@extra", from = "@extra")
    String extra;
}

