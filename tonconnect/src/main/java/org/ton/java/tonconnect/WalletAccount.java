package org.ton.java.tonconnect;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Setter
@Getter
@ToString
public class WalletAccount {
    private String address;
    private String publicKey;
    private int chain;
    private String walletStateInit; //base64UrlSafe
}