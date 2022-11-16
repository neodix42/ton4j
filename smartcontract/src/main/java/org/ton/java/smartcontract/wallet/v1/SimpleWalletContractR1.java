package org.ton.java.smartcontract.wallet.v1;

import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.smartcontract.types.ExternalMessage;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.WalletContract;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

public class SimpleWalletContractR1 implements WalletContract {

    public static final String V1_R1_CODE_HEX = "B5EE9C72410101010044000084FF0020DDA4F260810200D71820D70B1FED44D0D31FD3FFD15112BAF2A122F901541044F910F2A2F80001D31F3120D74A96D307D402FB00DED1A4C8CB1FCBFFC9ED5441FDF089";
    Options options;
    Address address;

    /**
     * @param options Options
     */
    public SimpleWalletContractR1(Options options) {
        this.options = options;
        options.code = Cell.fromBoc(V1_R1_CODE_HEX);
    }

    @Override
    public String getName() {
        return "simpleR1";
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
