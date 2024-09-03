package org.ton.java.tonconnect;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Setter
@Getter
@ToString
class TonProof {
    private long timestamp;
    private Domain domain;
    private String signature; // Base64UrlSafe
    private String payload;   // plain
}