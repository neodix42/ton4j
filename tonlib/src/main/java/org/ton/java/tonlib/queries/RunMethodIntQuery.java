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
public class RunMethodIntQuery extends TypedAsyncObject {
    private long id;
    private MethodNumber method;
    private String stack;

    @Override
    public String getTypeName() {
        return "smc.runGetMethod";
    }
}


