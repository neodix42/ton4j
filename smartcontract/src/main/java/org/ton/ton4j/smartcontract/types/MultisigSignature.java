package org.ton.ton4j.smartcontract.types;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class MultisigSignature {
    long pubKeyPosition;
    byte[] signature;
}
