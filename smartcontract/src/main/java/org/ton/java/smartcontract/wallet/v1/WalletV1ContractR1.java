package org.ton.java.smartcontract.wallet.v1;

import org.ton.java.address.Address;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.ExternalMessage;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.WalletContract;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static java.util.Objects.isNull;

public class WalletV1ContractR1 implements WalletContract {

    Options options;
    Address address;

    /**
     * @param options Options
     */
    public WalletV1ContractR1(Options options) {
        this.options = options;
        options.code = CellBuilder.beginCell().fromBoc(WalletCodes.V1R1.getValue()).endCell();
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
        if (isNull(address)) {
            return (createStateInit()).address;
        }
        return address;
    }

    /**
     * Sends amount of nano toncoins to destination address with seqno 0
     *
     * @param tonlib             Tonlib
     * @param secretKey          byte[]
     * @param destinationAddress Address
     * @param amount             BigInteger
     */
    public ExtMessageInfo sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount) {
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, 0);
        return tonlib.sendRawMessage(Utils.bytesToBase64(msg.message.toBoc()));
    }

    /**
     * Sends amount of nano toncoins to destination address using specified seqno
     *
     * @param tonlib             Tonlib
     * @param secretKey          byte[]
     * @param destinationAddress Address
     * @param amount             BigInteger
     * @param seqno              long
     */
    public ExtMessageInfo sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount, long seqno) {
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno);
        return tonlib.sendRawMessage(msg.message.toBase64());
    }

    /**
     * Sends amount of nano toncoins to destination address using specified seqno with the comment
     *
     * @param tonlib             Tonlib
     * @param secretKey          byte[]
     * @param destinationAddress Address
     * @param amount             BigInteger
     * @param seqno              long
     * @param comment            String
     */
    public ExtMessageInfo sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount, long seqno, String comment) {
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno, CellBuilder.beginCell().storeUint(0, 32).storeString(comment).endCell());
        return tonlib.sendRawMessage(msg.message.toBase64());
    }

    /**
     * Sends amount of nano toncoins to destination address using auto-fetched seqno with the comment
     *
     * @param tonlib             Tonlib
     * @param secretKey          byte[]
     * @param destinationAddress Address
     * @param amount             BigInteger
     * @param comment            String
     */
    public ExtMessageInfo sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount, String comment) {
        long seqno = getSeqno(tonlib);
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno, CellBuilder.beginCell().storeUint(0, 32).storeString(comment).endCell());
        return tonlib.sendRawMessage(msg.message.toBase64());
    }

    /**
     * Sends amount of nano toncoins to destination address using specified seqno with the body
     *
     * @param tonlib             Tonlib
     * @param secretKey          byte[]
     * @param destinationAddress Address
     * @param amount             BigInteger
     * @param seqno              long
     * @param body               byte[]
     */
    public ExtMessageInfo sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount, long seqno, byte[] body) {
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno, body);
        return tonlib.sendRawMessage(msg.message.toBase64());
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
    public ExtMessageInfo sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount, long seqno, byte[] body, byte sendMode) {
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno, body, sendMode);
        return tonlib.sendRawMessage(msg.message.toBase64());
    }

    public ExtMessageInfo deploy(Tonlib tonlib, byte[] secretKey) {
        return tonlib.sendRawMessage(Utils.bytesToBase64(createInitExternalMessage(secretKey).message.toBoc()));
    }
}
