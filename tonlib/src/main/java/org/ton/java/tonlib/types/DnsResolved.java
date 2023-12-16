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
public class DnsResolved extends TypedAsyncObject {
    private List<DnsEntry> entries;
    @Override
    public String getTypeName() {
        return "dns.resolved";
    }
}
