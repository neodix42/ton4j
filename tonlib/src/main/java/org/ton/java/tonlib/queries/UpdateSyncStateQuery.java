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
public class UpdateSyncStateQuery extends TypedAsyncObject {
    private long id; // result from createQuery
    @Override
    public String getTypeName() {
        return "updateSyncState";
    }
}
