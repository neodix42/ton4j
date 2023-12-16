package org.ton.java.tonlib.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TvmStackEntryCell extends TvmStackEntry {
    private TvmCell cell;
    @Override
    public String getTypeObjectName() {
        return "tvm.stackEntryCell";
    }
}

