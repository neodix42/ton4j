package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;

import java.math.BigInteger;

@Builder
@Data
public class PaymentChannelConfig implements WalletConfig {
    long seqno;
    int mode;
    long createdAt;
    Address destination;
    BigInteger amount;
    Cell body;
    int operation; // 0 - simple send; 1 - deploy and install plugin; 2 - install plugin; 3 - remove plugin
    NewPlugin newPlugin;
    DeployedPlugin deployedPlugin;
}
