package org.ton.java.tonlib.types;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.ton.java.tonlib.base.TypedAsyncObject;

@SuperBuilder
@lombok.Data
@AllArgsConstructor
@NoArgsConstructor
public class Data extends TypedAsyncObject {
    private String bytes;
    @Override
    public String getTypeObjectName() {
        return "data";
    }
}

