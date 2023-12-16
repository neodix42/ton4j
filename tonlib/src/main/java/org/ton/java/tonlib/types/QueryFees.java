package org.ton.java.tonlib.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.ton.java.tonlib.base.TypedAsyncObject;

import java.util.List;

@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryFees extends TypedAsyncObject {
    private Fees source_fees;
    private List<Fees> destination_fees;

    @Override
    public String getTypeObjectName() {
        return "query.fees";
    }
}

