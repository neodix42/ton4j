package org.ton.java.tonlib.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.ton.java.tonlib.base.TypedAsyncObject;
@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TonlibError extends TypedAsyncObject {
    private long code;
    private String message;
    @Override
    public String getTypeObjectName() {
        return "error";
    }
}

