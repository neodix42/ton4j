package org.ton.java.tonlib.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Setter
@Getter
@ToString
public class TvmSlice extends TvmEntry {
    private String bytes;
    @Override
    public String getTypeName() {
        return "tvm.slice";
    }
}

