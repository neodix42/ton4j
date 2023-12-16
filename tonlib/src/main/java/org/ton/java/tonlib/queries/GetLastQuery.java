package org.ton.java.tonlib.queries;

import lombok.experimental.SuperBuilder;
import org.ton.java.tonlib.base.TypedAsyncObject;
@SuperBuilder
public class GetLastQuery extends TypedAsyncObject {
    @Override
    public String getTypeObjectName() {
        return "blocks.getMasterchainInfo";
    }
}
