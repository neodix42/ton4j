package org.ton.java.tonlib.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.ton.java.tonlib.base.TypedAsyncObject;

import java.util.List;

@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Shards extends TypedAsyncObject {
    private List<BlockIdExt> shards;

    @Override
    public String getTypeName() {
        return "blocks.shards";
    }
}

