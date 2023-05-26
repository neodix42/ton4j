package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
public class Validator {
    long magic;                     // `tlb:"#53"`
    SigPubKeyED25519 publicKey;     // `tlb:"."`
    BigInteger weight;              // `tlb:"## 64"`
}
