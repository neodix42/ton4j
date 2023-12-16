package org.ton.java.tonlib.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TvmStackEntrySlice extends TvmStackEntry {
    private TvmSlice slice;
    @Override
    public String getTypeObjectName() {
        return "tvm.stackEntrySlice";
    }
}

