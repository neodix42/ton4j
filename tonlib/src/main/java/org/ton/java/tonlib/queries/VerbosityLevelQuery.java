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
public class VerbosityLevelQuery extends TypedAsyncObject {
    private int new_verbosity_level;
    @Override
    public String getTypeObjectName() {
        return "setLogVerbosityLevel";
    }
}
