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
public class MasterChainInfo extends TypedAsyncObject {
    private BlockIdExt last;
    private String state_root_hash;
    private BlockIdExt init;

    @Override
    public String getTypeName() {
        return "blocks.masterchainInfo";
    }
}

