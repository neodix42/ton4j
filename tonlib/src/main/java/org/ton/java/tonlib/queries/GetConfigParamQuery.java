package org.ton.java.tonlib.queries;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.ton.java.tonlib.base.TypedAsyncObject;
import org.ton.java.tonlib.types.BlockIdExt;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetConfigParamQuery extends TypedAsyncObject {
    private BlockIdExt id;
    private long param;
    private long mode;

    @Override
    public String getTypeObjectName() {
        return "getConfigParam";
    }
}
