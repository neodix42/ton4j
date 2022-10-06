package org.ton.java.tonlib.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Setter
@Getter
@ToString
public class ShortTxId {
    long mode;
    String account; //base64
    long lt;
    String hash;
}

