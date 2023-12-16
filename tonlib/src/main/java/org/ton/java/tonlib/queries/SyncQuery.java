package org.ton.java.tonlib.queries;

import org.ton.java.tonlib.base.TypedAsyncObject;

public class SyncQuery extends TypedAsyncObject {
    @Override
    public String getTypeName() {
        return "sync";
    }
}
