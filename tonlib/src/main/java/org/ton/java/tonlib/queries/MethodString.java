package org.ton.java.tonlib.queries;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.ton.java.tonlib.base.TypedAsyncObject;
@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MethodString extends TypedAsyncObject {
    private String name;
    @Override
    public String getTypeObjectName() {
        return "smc.methodIdName";
    }
}
