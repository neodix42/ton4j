package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Builder
@Getter
@Setter
@ToString
public class HighloadConfig {
    //    public List<Long> queryId;
    public List<Destination> destinations;
}
