package org.ton.java.tonlib.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Setter
@Getter
@ToString
public class TvmNumber extends TvmEntry {
    private String number;
    @Override
    public String getTypeName() {
        return "tvm.numberDecimal";
    }
}

