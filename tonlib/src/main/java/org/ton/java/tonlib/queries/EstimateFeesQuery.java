package org.ton.java.tonlib.queries;


import com.jsoniter.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.ton.java.tonlib.base.TypedAsyncObject;
@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstimateFeesQuery extends TypedAsyncObject {
    @JsonProperty(value = "id")
    long queryId;
    boolean ignore_chksig;

    @Override
    public String getTypeObjectName() {
        return "query.estimateFees";
    }
}
