package org.ton.java.tonlib.queries;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.ton.java.tonlib.base.TypedAsyncObject;
import org.ton.java.tonlib.types.BlockId;

@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class LookupBlockQuery extends TypedAsyncObject {
    private long mode;
    private BlockId id;
    private long lt;
    private long utime;

    @Override
    public String getTypeName() {
        return "blocks.lookupBlock";
    }
}
