package org.ton.java.smartcontract.wallet.v1;

import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.ExternalMessage;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.WalletContract;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static java.util.Objects.isNull;

public class WalletV1ContractR2 implements WalletContract {

    Options options;
    Address address;

    /**
     * @param options Options
     */
    public WalletV1ContractR2(Options options) {
        this.options = options;
        options.code = Cell.fromBoc(WalletCodes.V1R2.getValue());
    }

    @Override
    public String getName() {
        return "V1R2";
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    public Address getAddress() {
        if (isNull(address)) {
            return (createStateInit()).address;
        }
        return address;
    }

    /**
     * Sends amount of nano toncoins to destination address using auto-fetched seqno without the body and default send-mode 3
     *
     * @param tonlib             Tonlib
     * @param secretKey          byte[]
     * @param destinationAddress Address
     * @param amount             BigInteger
     */
    public void sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount) {
        long seqno = getSeqno(tonlib);
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno);
        tonlib.sendRawMessage(Utils.bytesToBase64(msg.message.toBoc(false)));
    }

    /**
     * Sends amount of nano toncoins to destination address using specified seqno without the body and default send-mode 3
     *
     * @param tonlib             Tonlib
     * @param secretKey          byte[]
     * @param destinationAddress Address
     * @param amount             BigInteger
     * @param seqno              long
     */
    public void sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount, long seqno) {
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno);
        tonlib.sendRawMessage(Utils.bytesToBase64(msg.message.toBoc(false)));
    }

    /**
     * Sends amount of nano toncoins to destination address using specified seqno with the body and default send-mode 3
     *
     * @param tonlib             Tonlib
     * @param secretKey          byte[]
     * @param destinationAddress Address
     * @param amount             BigInteger
     * @param seqno              long
     * @param comment            String
     */
    public void sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount, long seqno, String comment) {
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno, CellBuilder.beginCell().storeUint(0, 32).storeString(comment).endCell());
        tonlib.sendRawMessage(msg.message.toBocBase64(false));
    }

    /**
     * Sends amount of nano toncoins to destination address using auto-fetched seqno with the body and default send-mode 3
     *
     * @param tonlib             Tonlib
     * @param secretKey          byte[]
     * @param destinationAddress Address
     * @param amount             BigInteger
     * @param comment            String
     */
    public void sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount, String comment) {
        long seqno = getSeqno(tonlib);
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno, CellBuilder.beginCell().storeUint(0, 32).storeString(comment).endCell());
        tonlib.sendRawMessage(msg.message.toBocBase64(false));
    }

    /**
     * Sends amount of nano toncoins to destination address using specified seqno with the body and default send-mode 3
     *
     * @param tonlib             Tonlib
     * @param secretKey          byte[]
     * @param destinationAddress Address
     * @param amount             BigInteger
     * @param seqno              long
     * @param body               byte[]
     */
    public void sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount, long seqno, byte[] body) {
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno, body);
        tonlib.sendRawMessage(Utils.bytesToBase64(msg.message.toBoc(false)));
    }

    /**
     * Sends amount of nano toncoins to destination address using specified seqno with the body and send-mode
     *
     * @param tonlib             Tonlib
     * @param secretKey          byte[]
     * @param destinationAddress Address
     * @param amount             BigInteger
     * @param seqno              long
     * @param body               byte[]
     * @param sendMode           byte
     */
    public void sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount, long seqno, byte[] body, byte sendMode) {
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno, body, sendMode);
        tonlib.sendRawMessage(Utils.bytesToBase64(msg.message.toBoc(false)));
    }

    /**
     * Sends amount of nano toncoins to destination address using auto-fetched seqno with the body and default send-mode 3
     *
     * @param tonlib             Tonlib
     * @param secretKey          byte[]
     * @param destinationAddress Address
     * @param amount             BigInteger
     * @param body               byte[]
     */
    public void sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount, byte[] body) {
        long seqno = getSeqno(tonlib);
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno, body);
        tonlib.sendRawMessage(Utils.bytesToBase64(msg.message.toBoc(false)));
    }

    /**
     * Sends amount of nano toncoins to destination address using auto-fetched seqno with the body and specified send-mode
     *
     * @param tonlib             Tonlib
     * @param secretKey          byte[]
     * @param destinationAddress Address
     * @param amount             BigInteger
     * @param body               byte[]
     * @param sendMode           byte
     */
    public void sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount, byte[] body, byte sendMode) {
        long seqno = getSeqno(tonlib);
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno, body, sendMode);
        tonlib.sendRawMessage(Utils.bytesToBase64(msg.message.toBoc(false)));
    }

    public void deploy(Tonlib tonlib, byte[] secretKey) {
        tonlib.sendRawMessage(Utils.bytesToBase64(createInitExternalMessage(secretKey).message.toBoc(false)));
    }
}
