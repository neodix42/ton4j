package org.ton.java.smartcontract.wallet.v1;

import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.smartcontract.types.ExternalMessage;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.WalletContract;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

public class WalletV1ContractR1 implements WalletContract {

    Options options;
    Address address;

    /**
     * @param options Options
     */
    public WalletV1ContractR1(Options options) {
        this.options = options;
        options.code = Cell.fromBoc(WalletCodes.V1R1.getValue());
    }

    @Override
    public String getName() {
        return "V1R1";
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    public Address getAddress() {
        if (address == null) {
            return (createStateInit()).address;
        }
        return address;
    }

    public void sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount) {
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, 0);
        tonlib.sendRawMessage(Utils.bytesToBase64(msg.message.toBoc(false)));
    }

    public void deploy(Tonlib tonlib, byte[] secretKey) {
        tonlib.sendRawMessage(Utils.bytesToBase64(createInitExternalMessage(secretKey).message.toBoc(false)));
    }
}
