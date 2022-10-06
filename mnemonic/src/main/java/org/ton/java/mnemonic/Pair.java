package org.ton.java.mnemonic;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Pair {
    byte[] publicKey;
    byte[] secretKey;

    private Pair() {

    }

    public Pair(byte[] publicKey, byte[] secretKey) {
        this.publicKey = publicKey;
        this.secretKey = secretKey;
    }
}
