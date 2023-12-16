package org.ton.java.tonlib.types;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Setter
@Getter
@ToString
public class TvmSlice extends TvmEntry {
    private String bytes;
    @Override
    public String getTypeObjectName() {
        return "tvm.slice";
    }
}

