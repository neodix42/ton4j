package org.ton.java.tonlib.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.ton.java.tonlib.base.TypedAsyncObject;

import java.math.BigInteger;

@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlockId extends TypedAsyncObject {
    private long workchain;
    private BigInteger shard;
    private long seqno;

    private void setShard(String value){
        this.shard = new BigInteger(value);
    }

    @Override
    public String getTypeObjectName() {
        return "ton.blockId";
    }
}
