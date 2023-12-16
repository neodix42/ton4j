package org.ton.java.tonlib.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigInteger;

@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShortTxId {
    private long mode;
    private String account; //base64
    private BigInteger lt;
    private String hash;

    public void setLt(String value){
        this.lt = new BigInteger(value);
    }
}

