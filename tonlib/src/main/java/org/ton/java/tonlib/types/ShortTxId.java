package org.ton.java.tonlib.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Builder
@Setter
@Getter
@ToString
public class ShortTxId implements Serializable {
    long mode;
    String account; //base64
    long lt;
    String hash;
}

