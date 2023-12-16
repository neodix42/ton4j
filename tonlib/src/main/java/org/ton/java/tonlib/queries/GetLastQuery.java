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
public class GetLastQuery extends TypedAsyncObject {
    @Override
    public String getTypeName() {
        return "blocks.getMasterchainInfo";
    }
}
