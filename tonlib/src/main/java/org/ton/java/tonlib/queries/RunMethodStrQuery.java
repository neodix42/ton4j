package org.ton.java.tonlib.queries;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.ton.java.tonlib.base.TypedAsyncObject;
import org.ton.java.tonlib.types.TvmStackEntry;

import java.util.Deque;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RunMethodStrQuery extends TypedAsyncObject {
    private long id;
    private MethodString method; //long or string
    private Deque<TvmStackEntry> stack;

    @Override
    public String getTypeObjectName() {
        return "smc.runGetMethod";
    }
}

