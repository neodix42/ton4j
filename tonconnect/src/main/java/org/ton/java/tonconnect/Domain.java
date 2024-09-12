package org.ton.java.tonconnect;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Setter
@Getter
@ToString
public class Domain {
    private int lengthBytes;
    private String value;
}
