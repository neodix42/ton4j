package org.ton.java.tonlib.base;

import com.jsoniter.annotation.JsonIgnore;
import com.jsoniter.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
public abstract class TypedAsyncObject {
    @JsonProperty(to = "@type", from = "@type")
    private String type;
    @Builder.Default
    @JsonProperty(to = "@extra", from = "@extra", defaultValueToOmit = "null")
    private UUID tag = UUID.randomUUID();
    @JsonIgnore
    public abstract String getTypeName();
}
