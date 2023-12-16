package org.ton.java.tonlib.queries;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.ton.java.tonlib.base.TypedAsyncObject;
import org.ton.java.tonlib.types.Destination;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateQuery extends TypedAsyncObject {
    private String body; //base64 encoded
    private String init_code;
    private String init_data;
    private Destination destination;

    @Override
    public String getTypeObjectName() {
        return "raw.createQuery";
    }
}
