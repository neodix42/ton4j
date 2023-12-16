package org.ton.java.tonlib.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.ton.java.tonlib.base.TypedAsyncObject;

import java.util.List;

@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RunResultGeneric<T> extends TypedAsyncObject {
    private long gas_used;
    private List<T> stack;
    private long exit_code;

    @Override
    public String getTypeName() {
        return "smc.runResult";
    }
}

