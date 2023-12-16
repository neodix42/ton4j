package org.ton.java.tonlib.types;

import com.jsoniter.any.Any;
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
public class TvmTuple extends TypedAsyncObject {
    private List<Any> elements;
    @Override
    public String getTypeName() {
        return "tvm.tuple";
    }
}

