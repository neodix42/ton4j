package org.ton.java.tonconnect;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Setter
@Getter
@ToString
class Domain {
    private int lengthBytes;
    private String value;
}
