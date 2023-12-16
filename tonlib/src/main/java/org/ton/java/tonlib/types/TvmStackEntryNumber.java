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
public class TvmStackEntryNumber extends TvmStackEntry {
    private TvmNumber number;

    public BigInteger getNumber() {
        return new BigInteger(number.getNumber());
    }

    @Override
    public String getTypeName() {
        return "tvm.stackEntryNumber";
    }
}

