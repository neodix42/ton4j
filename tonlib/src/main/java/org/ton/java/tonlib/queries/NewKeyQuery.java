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
public class NewKeyQuery extends TypedAsyncObject {
    private String mnemonic_password;
    private String random_extra_seed;
    private String local_password;

    @Override
    public String getTypeName() {
        return "createNewKey";
    }
}
