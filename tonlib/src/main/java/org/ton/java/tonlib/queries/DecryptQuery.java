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
public class DecryptQuery extends TypedAsyncObject {
    private String encrypted_data;
    private String secret;

    @Override
    public String getTypeObjectName() {
        return "decrypt";
    }
}
