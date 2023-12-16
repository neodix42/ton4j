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
public class SendRawMessageQuery extends TypedAsyncObject {
    private String body;
    @Override
    public String getTypeObjectName() {
        return "raw.sendMessage";
    }
}
