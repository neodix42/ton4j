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
public class SyncStateInProgress extends TypedAsyncObject {
    private long from_seqno;
    private long to_seqno;
    private long current_seqno;

    @Override
    public String getTypeName() {
        return "syncStateInProgress";
    }
}
