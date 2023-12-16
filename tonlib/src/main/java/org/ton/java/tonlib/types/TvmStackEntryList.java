package org.ton.java.tonlib.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TvmStackEntryList extends TvmStackEntry {
    private TvmList list;
    @Override
    public String getTypeObjectName() {
        return "tvm.stackEntryList";
    }
}

