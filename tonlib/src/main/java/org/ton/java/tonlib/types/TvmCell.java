package org.ton.java.tonlib.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TvmCell extends TvmEntry {
    private String bytes; //base64
    @Override
    public String getTypeObjectName() {
        return "tvm.cell";
    }
}

