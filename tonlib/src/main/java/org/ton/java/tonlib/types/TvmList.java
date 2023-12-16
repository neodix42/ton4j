package org.ton.java.tonlib.types;

import com.jsoniter.any.Any;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TvmList extends TvmEntry {
    private List<Any> elements;
    @Override
    public String getTypeName() {
        return "tvm.list";
    }
}

