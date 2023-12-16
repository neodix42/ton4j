package org.ton.java.tonlib.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TvmStackEntryTuple extends TvmStackEntry {
    private TvmTuple tuple;
    @Override
    public String getTypeName() {
        return "tvm.stackEntryTuple";
    }
}

