package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.smartcontract.wallet.v5.WalletActions;

@Builder
@Data
public class WalletV5Config implements WalletConfig{
    long walletId;
    long seqno;
    long validUntil;
    long createdAt;
    boolean bounce;
    WalletActions extensions;
    boolean signatureAllowed;
}