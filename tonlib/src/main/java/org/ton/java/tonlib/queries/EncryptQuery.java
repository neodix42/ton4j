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
public class EncryptQuery extends TypedAsyncObject {
    String decrypted_data;
    String secret;

    @Override
    public String getTypeName() {
        return "encrypt";
    }
}
