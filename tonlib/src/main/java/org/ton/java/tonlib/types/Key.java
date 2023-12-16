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
public class Key extends TypedAsyncObject {
    private String public_key;
    private String secret;

    @Override
    public String getTypeObjectName() {
        return "key";
    }
}

