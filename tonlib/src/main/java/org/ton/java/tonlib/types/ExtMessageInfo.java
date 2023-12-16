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
public class ExtMessageInfo extends TypedAsyncObject {
    private String body_hash;
    private TonlibError error;

    @Override
    public String getTypeName() {
        return "raw.extMessageInfo";
    }
}

