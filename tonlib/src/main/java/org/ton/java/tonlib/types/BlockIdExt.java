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
public class BlockIdExt extends TypedAsyncObject {
    private BigInteger workchain;
    private BigInteger shard;
    private BigInteger seqno;
    private String root_hash;
    private String file_hash;

    public void setShard(String value){
        this.shard = new BigInteger(value,16);
    }

    public String getShard(){
        return this.shard.toString(16);
    }

    @Override
    public String getTypeObjectName() {
        return "ton.blockIdExt";
    }
}

