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
public class BlockIdExt extends TypedAsyncObject {
    private long workchain;
    private long shard;
    private long seqno;
    private String root_hash;
    private String file_hash;

    @Override
    public String getTypeName() {
        return "ton.blockIdExt";
    }
}

